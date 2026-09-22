#!/usr/bin/env python3
"""Deterministic API-only development/demo data orchestrator.

The orchestrator never imports service entities and never connects to a
service database. It uses stable slugs, SKUs, receipt references,
Idempotency-Key headers, and scenario references so a rerun reuses the same
records. Database-generated UUIDs are recorded only after the API returns them.
"""

from __future__ import annotations

import argparse
import ast
import hashlib
import hmac
import json
import os
import shlex
import shutil
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
IMAGES = ROOT / "images"
MANIFEST = ROOT / "manifests" / "seed-manifest.json"
SOURCE_URL = "https://bluetokaicoffee.com/collections/roasted-and-ground-coffee-beans"


def load_dotenv(path: Path) -> None:
    """Load simple KEY=VALUE pairs without requiring a platform-specific shell."""
    if not path.is_file():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        key, separator, value = line.partition("=")
        if not separator or not key.strip():
            continue
        value = value.strip()
        if value[:1] in {"'", '"'} and value[-1:] == value[:1]:
            try:
                value = str(ast.literal_eval(value))
            except (SyntaxError, ValueError):
                value = value[1:-1]
        os.environ.setdefault(key.strip(), value)


class SeedError(RuntimeError):
    pass


class ApiError(SeedError):
    def __init__(self, service: str, method: str, path: str, status: int, detail: str):
        super().__init__(f"{service} {method} {path} returned HTTP {status}: {detail}")
        self.service, self.method, self.path, self.status, self.detail = service, method, path, status, detail


class ApiClient:
    def __init__(self, service: str, base_url: str, timeout: float = 10):
        if not base_url or "://" not in base_url:
            raise SeedError(f"{service} URL is missing or invalid")
        self.service = service
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def request(self, method: str, path: str, body: Any = None, headers: dict[str, str] | None = None,
                expected: tuple[int, ...] = (200, 201, 202, 204)) -> Any:
        request_headers = {"Accept": "application/json", **(headers or {})}
        payload = None
        if body is not None:
            payload = json.dumps(body, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            request_headers.setdefault("Content-Type", "application/json")
        request = urllib.request.Request(self.base_url + path, data=payload, headers=request_headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                raw = response.read()
                if response.status not in expected:
                    raise ApiError(self.service, method, path, response.status, raw[:500].decode("utf-8", "replace"))
                if not raw:
                    return None
                content_type = response.headers.get("Content-Type", "")
                return json.loads(raw.decode("utf-8")) if "json" in content_type.lower() or raw[:1] in (b"{", b"[") else raw
        except urllib.error.HTTPError as exc:
            detail = exc.read(1000).decode("utf-8", "replace")
            raise ApiError(self.service, method, path, exc.code, detail) from exc
        except urllib.error.URLError as exc:
            raise SeedError(f"{self.service} {method} {path} is unavailable: {exc.reason}") from exc

    def upload(self, path: str, filename: str, content: bytes, token: str, sort_order: int = 0) -> Any:
        boundary = "----myEcommerceSeedBoundary"
        head = (
            f"--{boundary}\r\n"
            f"Content-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\n"
            "Content-Type: image/png\r\n\r\n"
        ).encode("utf-8")
        tail = f"\r\n--{boundary}--\r\n".encode("utf-8")
        payload = head + content + tail
        request = urllib.request.Request(
            self.base_url + path + "?" + urllib.parse.urlencode({"sortOrder": sort_order}),
            data=payload,
            headers={
                "Accept": "application/json",
                "Authorization": f"Bearer {token}",
                "Content-Type": f"multipart/form-data; boundary={boundary}",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                raw = response.read()
                if response.status not in (200, 201):
                    raise ApiError(self.service, "POST", path, response.status, raw[:500].decode("utf-8", "replace"))
                return json.loads(raw.decode("utf-8"))
        except urllib.error.HTTPError as exc:
            raise ApiError(self.service, "POST", path, exc.code, exc.read(1000).decode("utf-8", "replace")) from exc
        except urllib.error.URLError as exc:
            raise SeedError(f"{self.service} POST {path} is unavailable: {exc.reason}") from exc


def read_json(name: str) -> Any:
    return json.loads((DATA / name).read_text(encoding="utf-8"))


def bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def compact_error(exc: Exception) -> str:
    return str(exc).replace("\n", " ")[:400]


class SeedRunner:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.products_doc = read_json("products.json")
        self.products = self.products_doc["products"]
        self.categories = read_json("categories.json")
        self.customers = read_json("customers.json")["customers"]
        self.inventory_plan = read_json("inventory.json")
        self.scenarios = read_json("scenarios.json")["scenarios"]
        self.previous = self._read_manifest()
        self.state: dict[str, Any] = {
            "seedVersion": self.products_doc["seedVersion"],
            "status": "running",
            "datasetType": "DEVELOPMENT_DEMO_ONLY",
            "source": {"name": "Blue Tokai reference catalog", "url": SOURCE_URL,
                       "note": "Reference metadata only; this is synthetic development data."},
            "lastRunAt": now(),
            "environment": os.environ.get("SEED_ENV"),
            "catalog": {"categories": {}, "products": {}, "variants": {}, "images": {}},
            "inventory": {"locations": {}, "units": {}, "reservations": {}, "adjustments": {}},
            "customers": {}, "carts": {}, "orders": {}, "payments": {},
            "shipping": {"fulfillments": {}, "shipments": {}, "trackingEvents": {}},
            "counts": {},
        }
        self.urls = {key: self._required_url(*env_names) for key, env_names in {
            "auth": ("SEED_AUTH_SERVICE_URL", "AUTH_SERVICE_URL"),
            "catalog": ("SEED_CATALOG_SERVICE_URL", "CATALOG_SERVICE_URL"),
            "inventory": ("SEED_INVENTORY_SERVICE_URL", "INVENTORY_SERVICE_URL"),
            "order": ("SEED_ORDER_SERVICE_URL", "ORDER_SERVICE_URL"),
            "cart": ("SEED_CART_SERVICE_URL", "CART_SERVICE_URL"),
            "customer": ("SEED_CUSTOMER_SERVICE_URL", "CUSTOMER_SERVICE_URL"),
            "payment": ("SEED_PAYMENT_SERVICE_URL", "PAYMENT_SERVICE_URL"),
            "shipping": ("SEED_SHIPPING_SERVICE_URL", "SHIPPING_SERVICE_URL"),
        }.items()}
        self.api = {key: ApiClient(key, url) for key, url in self.urls.items()}
        self.admin_token = ""
        self.customer_tokens: dict[str, str] = {}
        self.customer_records: dict[str, dict] = {}
        self.inventory_units: dict[str, list[dict]] = {}

    def _required_url(self, *env_names: str) -> str:
        value = next((os.environ.get(name, "").strip() for name in env_names if os.environ.get(name, "").strip()), "")
        if not value:
            raise SeedError(f"one of {', '.join(env_names)} must be set; see dev-seed/README.md")
        return value

    def _read_manifest(self) -> dict:
        if not MANIFEST.exists():
            return {}
        try:
            return json.loads(MANIFEST.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return {}

    def save_manifest(self) -> None:
        self._update_counts()
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(self.state, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    def _update_counts(self) -> None:
        products = self.state["catalog"]["products"]
        self.state["counts"] = {
            "products": len(products),
            "categories": len(self.state["catalog"]["categories"]),
            "variants": len(self.state["catalog"]["variants"]),
            "skus": len(self.state["catalog"]["variants"]),
            "images": len(self.state["catalog"]["images"]),
            "locations": len(self.state["inventory"]["locations"]),
            "inventoryUnits": sum(len(units) for units in self.inventory_units.values()),
            "customers": len(self.state["customers"]),
            "addresses": sum(len(record.get("addresses", {})) for record in self.state["customers"].values()),
            "carts": len(self.state["carts"]),
            "orders": len(self.state["orders"]),
            "payments": len(self.state["payments"]),
            "fulfillments": len(self.state["shipping"]["fulfillments"]),
            "shipments": len(self.state["shipping"]["shipments"]),
            "trackingEvents": len(self.state["shipping"]["trackingEvents"]),
        }

    def health(self) -> None:
        for index, (name, client) in enumerate(self.api.items(), start=1):
            print(f"[health {index}/{len(self.api)}] {name}")
            client.request("GET", "/actuator/health", expected=(200,))

    def authenticate_admin(self) -> None:
        email = os.environ.get("SEED_ADMIN_EMAIL", "").strip()
        password = os.environ.get("SEED_ADMIN_PASSWORD", "")
        if not email or not password:
            raise SeedError("SEED_ADMIN_EMAIL and SEED_ADMIN_PASSWORD are required for catalog/inventory/admin APIs")
        response = self.api["auth"].request("POST", "/api/v1/auth/login", {"email": email, "password": password}, expected=(200,))
        self.admin_token = response["accessToken"]

    def seed_customers(self) -> None:
        print("[2/10] Customer and Auth")
        password = os.environ.get("SEED_DEMO_PASSWORD", "DemoSeedOnly!2026")
        if len(password) < 12:
            raise SeedError("SEED_DEMO_PASSWORD must contain at least 12 characters")
        for customer in self.customers:
            try:
                self.api["auth"].request("POST", "/api/v1/auth/register", {
                    "email": customer["email"], "password": password,
                    "firstName": customer["firstName"], "lastName": customer["lastName"],
                }, expected=(201,))
            except ApiError as exc:
                if exc.status != 409:
                    raise
            token_response = self.api["auth"].request("POST", "/api/v1/auth/login", {
                "email": customer["email"], "password": password,
            }, expected=(200,))
            token = token_response["accessToken"]
            self.customer_tokens[customer["key"]] = token
            profile = self.api["customer"].request("GET", "/api/v1/customers/me", headers=bearer(token), expected=(200,))
            profile = self.api["customer"].request("PATCH", "/api/v1/customers/me", {
                "firstName": customer["firstName"], "lastName": customer["lastName"], "phone": customer["phone"],
            }, headers=bearer(token), expected=(200,))
            addresses = self.api["customer"].request("GET", "/api/v1/customers/me/addresses", headers=bearer(token), expected=(200,))
            address_ids: dict[str, str] = {}
            for address_type in ("SHIPPING", "BILLING"):
                existing = next((address for address in addresses if address.get("addressType") == address_type), None)
                if existing is None:
                    existing = self.api["customer"].request("POST", "/api/v1/customers/me/addresses", self._address_payload(customer, address_type), headers=bearer(token), expected=(201,))
                address_ids[address_type.lower()] = existing["id"]
            self.customer_records[customer["key"]] = {"authUserId": profile["authUserId"], "customerId": profile["id"], "email": profile["email"], "addresses": address_ids}
            self.state["customers"][customer["key"]] = self.customer_records[customer["key"]]

    def _address_payload(self, customer: dict, address_type: str) -> dict:
        return {
            "addressType": address_type, "recipientName": f"{customer['firstName']} {customer['lastName']}",
            "phone": customer["phone"], "line1": f"Demo Block {customer['key'][-2:]}, Example Road",
            "line2": "Development District", "city": customer["city"], "state": customer["state"],
            "postalCode": customer["postalCode"], "country": "IN", "landmark": "Synthetic demo address", "isDefault": True,
        }

    def seed_catalog(self) -> None:
        print("[3/10] Catalog")
        category_ids: dict[str, str] = {}
        for category in self.categories:
            try:
                existing = self.api["catalog"].request("GET", f"/api/v1/categories/slug/{urllib.parse.quote(category['slug'])}", expected=(200,))
            except ApiError as exc:
                if exc.status != 404:
                    raise
                parent_id = category_ids.get(category["parentKey"]) if category["parentKey"] else None
                existing = self.api["catalog"].request("POST", "/api/v1/categories", {
                    "name": category["name"], "parentId": parent_id, "slug": category["slug"],
                }, headers=bearer(self.admin_token), expected=(201,))
            category_ids[category["key"]] = existing["id"]
            self.state["catalog"]["categories"][category["key"]] = existing["id"]

        for product in self.products:
            category_id = category_ids[product["categoryKey"]]
            try:
                existing = self.api["catalog"].request("GET", f"/api/v1/products/slug/{urllib.parse.quote(product['slug'])}", expected=(200,))
            except ApiError as exc:
                if exc.status != 404:
                    raise
                existing = self.api["catalog"].request("POST", "/api/v1/products", self._product_payload(product, category_id), headers=bearer(self.admin_token), expected=(201,))
            expected_skus = {variant["sku"] for variant in product["variants"]}
            actual_skus = {variant["sku"] for variant in existing.get("variants", [])}
            if expected_skus != actual_skus:
                raise SeedError(f"existing product {product['slug']} has different SKUs; use the explicit development reset before reseeding")
            self.state["catalog"]["products"][product["key"]] = existing["id"]
            for variant in existing.get("variants", []):
                self.state["catalog"]["variants"][variant["sku"]] = variant["id"]
            image_by_name = {image.get("originalFilename"): image for image in existing.get("images", [])}
            for image in product["images"]:
                if image["file"] not in image_by_name:
                    uploaded = self.api["catalog"].upload(f"/api/v1/products/{existing['id']}/images", image["file"], (IMAGES / image["file"]).read_bytes(), self.admin_token, image["sortOrder"])
                    image_by_name[image["file"]] = uploaded
                self.state["catalog"]["images"][image["file"]] = image_by_name[image["file"]].get("id")

    def _product_payload(self, product: dict, category_id: str) -> dict:
        return {
            "categoryId": category_id, "name": product["name"], "description": product["description"],
            "brand": product["brand"], "slug": product["slug"], "status": product["status"],
            "variants": [{"sku": variant["sku"], "price": variant["price"], "currency": variant["currency"], "attributes": variant["attributes"], "status": variant["status"]} for variant in product["variants"]],
            "images": [],
        }

    def _set_variant_price(self, sku: str, price: int) -> None:
        fixture = next((product for product in self.products
                        if any(variant["sku"] == sku for variant in product["variants"])), None)
        if fixture is None:
            raise SeedError(f"price scenario references unknown SKU: {sku}")
        product = self.api["catalog"].request(
            "GET", f"/api/v1/products/slug/{urllib.parse.quote(fixture['slug'])}", expected=(200,))
        variants = []
        changed = False
        for variant in product.get("variants", []):
            variant_price = price if variant["sku"] == sku else variant["price"]
            changed = changed or variant["sku"] == sku and variant["price"] != price
            variants.append({
                "sku": variant["sku"], "price": variant_price, "currency": variant["currency"],
                "attributes": variant.get("attributes", {}), "status": variant.get("status", "ACTIVE"),
            })
        if not any(variant["sku"] == sku for variant in variants):
            raise SeedError(f"catalog product {fixture['slug']} is missing price scenario SKU {sku}")
        if not changed:
            return
        # The Catalog update contract preserves uploaded images when `images` is
        # omitted. Reconcile by SKU so variant IDs (and inventory references)
        # remain stable while the Order API snapshots the staged price.
        self.api["catalog"].request("PUT", f"/api/v1/products/{product['id']}", {
            "categoryId": product["categoryId"], "name": product["name"],
            "description": product.get("description"), "brand": product.get("brand"),
            "slug": product["slug"], "status": product.get("status"), "variants": variants,
        }, headers=bearer(self.admin_token), expected=(200,))

    def seed_locations(self) -> dict[str, str]:
        print("[4/10] Inventory locations")
        locations = self.api["inventory"].request("GET", "/api/v1/inventory/locations", headers=bearer(self.admin_token), expected=(200,))
        by_code = {location["code"]: location for location in locations}
        ids = {}
        for location in self.inventory_plan["locations"]:
            record = by_code.get(location["code"])
            if record is None:
                record = self.api["inventory"].request("POST", "/api/v1/inventory/locations", {
                    "code": location["code"], "name": location["name"], "status": location["status"],
                }, headers=bearer(self.admin_token), expected=(201,))
            ids[location["code"]] = record["id"]
            self.state["inventory"]["locations"][location["code"]] = record["id"]
        return ids

    def seed_inventory(self, location_ids: dict[str, str]) -> None:
        print("[5/10] Itemized Inventory")
        per_location = self.inventory_plan["unitsPerSkuByLocation"]
        for product in self.products:
            for variant in product["variants"]:
                sku = variant["sku"]
                self.inventory_units.setdefault(sku, [])
                for location in self.inventory_plan["locations"]:
                    count = int(per_location[location["code"]])
                    reference = f"{self.inventory_plan['receiptReferencePrefix']}-{sku}-{location['code']}"
                    response = self.api["inventory"].request("POST", f"/api/v1/inventory/{urllib.parse.quote(sku)}/receive", {
                        "locationId": location_ids[location["code"]], "quantity": count,
                        "units": [{"barcode": f"{reference}-{ordinal:02d}"} for ordinal in range(1, count + 1)],
                        "referenceId": reference,
                    }, headers=bearer(self.admin_token), expected=(201,))
                    for unit in response["units"]:
                        unit["locationCode"] = location["code"]
                        if not any(old.get("id") == unit.get("id") for old in self.inventory_units[sku]):
                            self.inventory_units[sku].append(unit)
                    self.state["inventory"]["units"][sku] = [
                        {"id": unit.get("id"), "unitCode": unit.get("unitCode"),
                         "locationCode": unit.get("locationCode"), "status": unit.get("status")}
                        for unit in self.inventory_units[sku]
                    ]
        for scenario in self.inventory_plan["adjustmentScenarios"]:
            sku = scenario["sku"]
            desired_status = "DAMAGED" if scenario["reason"] == "DAMAGE" else "LOST"
            candidate = next((unit for unit in self.inventory_units.get(sku, [])
                              if unit.get("locationCode") == scenario["locationCode"] and unit.get("status") == desired_status), None)
            if candidate is None:
                candidate = next((unit for unit in self.inventory_units.get(sku, [])
                                  if unit.get("locationCode") == scenario["locationCode"] and unit.get("status") == "AVAILABLE"), None)
                if candidate is None:
                    raise SeedError(f"no AVAILABLE unit available for {scenario['key']}")
                response = self.api["inventory"].request("POST", f"/api/v1/inventory/{urllib.parse.quote(sku)}/adjustments", {
                    "locationId": location_ids[scenario["locationCode"]], "quantity": -1,
                    "reason": scenario["reason"], "referenceId": scenario["referenceId"], "unitIds": [candidate["id"]],
                }, headers=bearer(self.admin_token), expected=(200,))
                candidate["status"] = desired_status
                self.state["inventory"]["units"][sku] = [
                    {"id": unit.get("id"), "unitCode": unit.get("unitCode"),
                     "locationCode": unit.get("locationCode"), "status": unit.get("status")}
                    for unit in self.inventory_units[sku]
                ]
                self.state["inventory"]["adjustments"][scenario["key"]] = response.get("id")
        for scenario in self.inventory_plan["reservationScenarios"]:
            existing = self.state["inventory"]["reservations"].get(scenario["key"])
            response = self.api["inventory"].request("POST", f"/api/v1/inventory/{urllib.parse.quote(scenario['sku'])}/reservations", {
                "locationId": location_ids[scenario["locationCode"]], "quantity": scenario["quantity"], "referenceId": scenario["referenceId"],
            }, headers=bearer(self.admin_token), expected=(201,))
            self.state["inventory"]["reservations"][scenario["key"]] = response["reservationId"]
            reserved_ids = {unit["unitId"] for unit in response.get("units", [])}
            for unit in self.inventory_units.get(scenario["sku"], []):
                if unit.get("id") in reserved_ids:
                    unit["status"] = "RESERVED"
            self.state["inventory"]["units"][scenario["sku"]] = [
                {"id": unit.get("id"), "unitCode": unit.get("unitCode"),
                 "locationCode": unit.get("locationCode"), "status": unit.get("status")}
                for unit in self.inventory_units.get(scenario["sku"], [])
            ]

    def seed_carts(self) -> None:
        print("[6/10] Carts")
        for scenario in self.scenarios:
            customer_key = scenario.get("customerKey")
            if not customer_key or not scenario.get("items"):
                continue
            token = self.customer_tokens[customer_key]
            cart = self.api["cart"].request("GET", "/api/v1/cart", headers=bearer(token), expected=(200,))
            if cart.get("status") == "ACTIVE":
                existing_skus = {item["sku"] for item in cart.get("items", [])}
                for item in scenario["items"]:
                    if item["sku"] not in existing_skus:
                        cart = self.api["cart"].request("POST", "/api/v1/cart/items", item, headers=bearer(token), expected=(200,))
            self.state["carts"][customer_key] = cart["id"]
        for customer in self.customers:
            if customer["key"] not in self.state["carts"]:
                cart = self.api["cart"].request("GET", "/api/v1/cart", headers=bearer(self.customer_tokens[customer["key"]]), expected=(200,))
                self.state["carts"][customer["key"]] = cart["id"]

    def _scenario_items(self, scenario: dict) -> list[dict]:
        return scenario.get("items") or [{"sku": scenario["sku"], "quantity": scenario["quantity"]}]

    def seed_orders(self) -> None:
        print("[7/10] Orders")
        price_scenario = next((scenario for scenario in self.scenarios if scenario.get("key") == "historical-price-change"), None)
        staged_price = False
        try:
            if price_scenario:
                self._set_variant_price(price_scenario["sku"], price_scenario["before"])
                staged_price = True
            for scenario in self.scenarios:
                if "orderReference" not in scenario:
                    continue
                customer_key = scenario["customerKey"]
                token = self.customer_tokens[customer_key]
                address = self._shipping_address(customer_key)
                scenario_items = self._scenario_items(scenario)
                order_body = {"currency": "INR", "items": scenario_items, "shippingAddress": address}
                if scenario["orderReference"] == "DEMO-ORDER-001":
                    cart = self.api["cart"].request("GET", "/api/v1/cart", headers=bearer(token), expected=(200,))
                    if cart.get("status") == "ACTIVE":
                        desired = {item["sku"]: item["quantity"] for item in scenario_items}
                        for existing in cart.get("items", []):
                            if existing["sku"] not in desired:
                                cart = self.api["cart"].request("DELETE", f"/api/v1/cart/items/{existing['id']}", headers=bearer(token), expected=(200,))
                            elif existing["quantity"] != desired[existing["sku"]]:
                                cart = self.api["cart"].request("PATCH", f"/api/v1/cart/items/{existing['id']}", {"quantity": desired[existing["sku"]]}, headers=bearer(token), expected=(200,))
                        existing_skus = {item["sku"] for item in cart.get("items", [])}
                        for item in scenario_items:
                            if item["sku"] not in existing_skus:
                                cart = self.api["cart"].request("POST", "/api/v1/cart/items", item, headers=bearer(token), expected=(200,))
                    checkout = self.api["cart"].request("POST", "/api/v1/cart/checkout", {"currency": "INR", "preferredLocationId": None, "shippingAddress": address}, headers={**bearer(token), "Idempotency-Key": scenario["orderReference"]}, expected=(200,))
                    order_id = checkout["orderId"]
                    order = self.api["order"].request("GET", f"/api/v1/orders/{order_id}", headers=bearer(token), expected=(200,))
                else:
                    order = self.api["order"].request("POST", "/api/v1/orders", order_body, headers={**bearer(token), "Idempotency-Key": scenario["orderReference"]}, expected=(201,))
                if scenario["orderReference"] == "DEMO-ORDER-001" and price_scenario:
                    historical_item = next((item for item in order.get("items", []) if item.get("sku") == price_scenario["sku"]), None)
                    if historical_item is None or Decimal(str(historical_item.get("unitPrice"))) != Decimal(str(price_scenario["before"])):
                        raise SeedError("DEMO-ORDER-001 already has a non-historical price; use the explicit development reset before reseeding")
                self.state["orders"][scenario["orderReference"]] = {"id": order["id"], "customerKey": customer_key, "status": order["status"]}
        finally:
            if staged_price:
                self._set_variant_price(price_scenario["sku"], price_scenario["after"])

    def _shipping_address(self, customer_key: str) -> dict:
        customer = self.customer_records[customer_key]
        source = next(customer_data for customer_data in self.customers if customer_data["key"] == customer_key)
        return {
            "sourceAddressId": customer["addresses"]["shipping"], "recipientName": f"{source['firstName']} {source['lastName']}",
            "phone": source["phone"], "line1": f"Demo Block {customer_key[-2:]}, Example Road", "line2": "Development District",
            "city": source["city"], "state": source["state"], "postalCode": source["postalCode"], "country": "IN", "landmark": "Synthetic demo address",
        }

    def seed_payments(self) -> None:
        print("[8/10] Sandbox Payments")
        secret = os.environ.get("PAYMENT_SANDBOX_WEBHOOK_SECRET", "dev-sandbox-webhook-secret").encode("utf-8")
        payment_to_order_token = os.environ.get("PAYMENT_TO_ORDER_SERVICE_TOKEN", "dev-payment-to-order")
        for scenario in self.scenarios:
            if "orderReference" not in scenario:
                continue
            reference = scenario["orderReference"]
            order_record = self.state["orders"][reference]
            token = self.customer_tokens[scenario["customerKey"]]
            response = self.api["payment"].request("POST", "/api/v1/payments", {
                "orderId": order_record["id"], "preferredProvider": "SANDBOX", "paymentMethodType": "DEMO_CHECKOUT",
            }, headers={**bearer(token), "Idempotency-Key": f"{reference}-PAYMENT"}, expected=(201,))
            payment_id = response["id"]
            self.state["payments"][reference] = {"id": payment_id, "status": response["status"], "customerKey": scenario["customerKey"]}
            desired = scenario["payment"]
            if desired in {"CAPTURED", "FAILED"} and response["status"] not in {desired, "REFUNDED"}:
                event_type = "payment.captured" if desired == "CAPTURED" else "payment.failed"
                payload = json.dumps({
                    "eventId": f"{reference}-PAYMENT-EVENT", "eventType": event_type,
                    "providerPaymentId": response["providerPaymentId"], "providerOrderId": response["providerOrderId"],
                    "amount": float(response["amount"]), "currency": response["currency"],
                    "failureCode": "DEMO_DECLINED" if desired == "FAILED" else None,
                    "failureMessage": "Synthetic sandbox decline" if desired == "FAILED" else None,
                }, separators=(",", ":"))
                signature = hmac.new(secret, payload.encode("utf-8"), hashlib.sha256).hexdigest()
                self._raw_json(self.api["payment"], "POST", "/api/v1/payments/webhooks/SANDBOX", payload, {"X-Provider-Signature": signature})
                event = "CAPTURED" if desired == "CAPTURED" else "FAILED"
                self._payment_order_event(response, event, payment_to_order_token)
                response["status"] = desired
                self.state["payments"][reference]["status"] = desired
            if scenario.get("refund") and response.get("status") in {"CAPTURED", "REFUNDED"}:
                response = self.api["payment"].request("POST", f"/api/v1/payments/{payment_id}/refund", {"reason": "Synthetic demo refund"}, headers={**bearer(token), "Idempotency-Key": f"{reference}-REFUND"}, expected=(200,))
                self.state["payments"][reference]["status"] = response["status"]
            if desired == "FAILED":
                order = self.api["order"].request("GET", f"/api/v1/orders/{order_record['id']}", headers=bearer(token), expected=(200,))
                for item in order.get("items", []):
                    if item.get("reservationId"):
                        try:
                            self.api["inventory"].request("POST", f"/api/v1/inventory/reservations/{item['reservationId']}/release", headers=bearer(self.admin_token), expected=(200,))
                        except ApiError as exc:
                            if exc.status not in (409,):
                                raise

    def _raw_json(self, client: ApiClient, method: str, path: str, payload: str, headers: dict[str, str]) -> Any:
        request = urllib.request.Request(client.base_url + path, data=payload.encode("utf-8"), headers={"Accept": "application/json", "Content-Type": "application/json", **headers}, method=method)
        try:
            with urllib.request.urlopen(request, timeout=client.timeout) as response:
                raw = response.read()
                if response.status != 202:
                    raise ApiError(client.service, method, path, response.status, raw[:500].decode("utf-8", "replace"))
                return json.loads(raw.decode("utf-8")) if raw else None
        except urllib.error.HTTPError as exc:
            raise ApiError(client.service, method, path, exc.code, exc.read(1000).decode("utf-8", "replace")) from exc
        except urllib.error.URLError as exc:
            raise SeedError(f"{client.service} {method} {path} is unavailable: {exc.reason}") from exc

    def _payment_order_event(self, payment: dict, status: str, token: str) -> None:
        self.api["order"].request("POST", f"/internal/orders/{payment['orderId']}/payment-events", {
            "paymentId": payment["id"], "customerId": str(payment["customerId"]),
            "paymentStatus": status, "amount": payment["amount"], "currency": payment["currency"],
            "provider": payment["provider"], "providerPaymentId": payment.get("providerPaymentId"),
        }, headers={"X-Payment-Service-Token": token}, expected=(200, 204))

    def seed_shipping(self) -> None:
        print("[9/10] Fulfillment, Shipments and Tracking")
        order_to_shipping = os.environ.get("ORDER_TO_SHIPPING_SERVICE_TOKEN", "dev-order-to-shipping")
        shipping_secret = os.environ.get("SHIPPING_WEBHOOK_SECRET", "dev-shipping-webhook-secret").encode("utf-8")
        for scenario in self.scenarios:
            if "orderReference" not in scenario or scenario["payment"] != "CAPTURED":
                continue
            reference = scenario["orderReference"]
            order_record = self.state["orders"][reference]
            detail = self.api["order"].request("GET", f"/api/v1/orders/{order_record['id']}", headers=bearer(self.customer_tokens[scenario["customerKey"]]), expected=(200,))
            if detail["status"] not in {"CONFIRMED", "FULFILLING", "SHIPPED", "DELIVERED"}:
                raise SeedError(f"{reference} is {detail['status']}; payment-to-order transition did not complete")
            fulfillment_id = self.state["shipping"]["fulfillments"].get(reference)
            if fulfillment_id:
                fulfillment = self.api["shipping"].request("GET", f"/api/v1/fulfillments/{fulfillment_id}", headers=bearer(self.admin_token), expected=(200,))
            else:
                fulfillment = self.api["shipping"].request("POST", "/internal/fulfillments", {"orderId": order_record["id"]}, headers={"X-Order-Service-Token": order_to_shipping}, expected=(201,))
                fulfillment_id = fulfillment["id"]
                self.state["shipping"]["fulfillments"][reference] = fulfillment_id
            shipment_id = self.state["shipping"]["shipments"].get(reference)
            if shipment_id:
                shipment = self.api["shipping"].request("GET", f"/api/v1/shipments/{shipment_id}", headers=bearer(self.admin_token), expected=(200,))
            else:
                lines = [{"orderItemId": item["orderItemId"], "quantity": item["quantity"], "inventoryUnitIds": item.get("inventoryUnitIds", [])} for item in fulfillment.get("items", [])]
                shipment = self.api["shipping"].request("POST", "/internal/shipments", {
                    "fulfillmentId": fulfillment_id, "carrier": "SANDBOX", "serviceLevel": "STANDARD", "shippingCost": 0, "currency": "INR", "lines": lines,
                }, headers={"X-Order-Service-Token": order_to_shipping, "Idempotency-Key": f"{reference}-SHIPMENT"}, expected=(201,))
                shipment_id = shipment["id"]
                self.state["shipping"]["shipments"][reference] = shipment_id
            for event_type in scenario.get("tracking", []):
                event_key = f"{reference}-{event_type}"
                if event_key in self.state["shipping"]["trackingEvents"]:
                    continue
                payload = json.dumps({
                    "providerEventId": event_key, "providerShipmentId": shipment["providerShipmentId"],
                    "trackingNumber": shipment["trackingNumber"], "eventType": event_type,
                    "description": f"Synthetic sandbox carrier event: {event_type}", "location": "Demo logistics network", "occurredAt": now(),
                }, separators=(",", ":"))
                signature = hmac.new(shipping_secret, payload.encode("utf-8"), hashlib.sha256).hexdigest()
                self._raw_json(self.api["shipping"], "POST", "/api/v1/shipping/webhooks/SANDBOX", payload, {"X-Sandbox-Signature": signature})
                self.state["shipping"]["trackingEvents"][event_key] = event_type
            if "DELIVERED" in scenario.get("tracking", []):
                order = self.api["order"].request("GET", f"/api/v1/orders/{order_record['id']}", headers=bearer(self.customer_tokens[scenario["customerKey"]]), expected=(200,))
                for item in order.get("items", []):
                    if item.get("reservationId"):
                        self.api["inventory"].request("POST", f"/api/v1/inventory/reservations/{item['reservationId']}/confirm", headers=bearer(self.admin_token), expected=(200,))

    def validate_live(self) -> None:
        print("[10/10] Validation")
        catalog_page = self.api["catalog"].request("GET", "/api/v1/products?page=0&size=100&sort=name,asc", expected=(200,))
        if catalog_page.get("totalElements") != len(self.products):
            raise SeedError(f"catalog product count mismatch: expected {len(self.products)}, got {catalog_page.get('totalElements')}")
        inventory_summary = self.api["inventory"].request("GET", "/api/v1/inventory/summary", headers=bearer(self.admin_token), expected=(200,))
        if inventory_summary.get("totalInventoryUnits", 0) < 100:
            raise SeedError("inventory validation found fewer than 100 active physical units")
        for customer in self.customers:
            cart = self.api["cart"].request("GET", "/api/v1/cart", headers=bearer(self.customer_tokens[customer["key"]]), expected=(200,))
            if cart.get("status") == "ACTIVE" and cart.get("items") and cart.get("totalQuantity", 0) > 0:
                # Cart reads are intentionally not reservations; this is a visible contract check.
                pass
        for reference, record in self.state["orders"].items():
            order = self.api["order"].request("GET", f"/api/v1/orders/{record['id']}", headers=bearer(self.customer_tokens[record["customerKey"]]), expected=(200,))
            if any(item.get("sku") not in self.state["catalog"]["variants"] for item in order.get("items", [])):
                raise SeedError(f"orphan SKU in {reference}")
        self.save_manifest()

    def run(self) -> None:
        self.health()
        print("[1/10] Auth")
        self.authenticate_admin()
        self.seed_customers()
        self.seed_catalog()
        location_ids = self.seed_locations()
        self.seed_inventory(location_ids)
        self.seed_carts()
        if not self.args.skip_orders:
            self.seed_orders()
            self.seed_payments()
            if not self.args.skip_shipping:
                self.seed_shipping()
        self.save_manifest()
        self.validate_live()
        self.state["status"] = "completed"
        self.save_manifest()
        print(json.dumps(self.state["counts"], indent=2))


def reset_development() -> None:
    if os.environ.get("SEED_ENV") != "development":
        raise SeedError("--reset requires SEED_ENV=development")
    compose = [
        "auth-service/docker-compose.yml", "catalog-service/docker-compose.yml", "inventory-service/docker-compose.yml",
        "order-service/docker-compose.yml", "cart-service/docker-compose.yml", "customer-service/docker-compose.yml",
        "payment-service/docker-compose.yml", "shipping-service/docker-compose.yml",
    ]
    docker = compose_command()
    print("Resetting only the eight named local development Compose projects; production URLs are never contacted.")
    for compose_file in compose:
        subprocess.run([*docker, "-f", str(ROOT / compose_file), "down", "-v"], cwd=ROOT, check=True)
    if MANIFEST.exists():
        MANIFEST.unlink()


def compose_command() -> list[str]:
    configured = os.environ.get("DOCKER_COMPOSE_BIN", "").strip()
    if configured:
        return shlex.split(configured, posix=os.name != "nt")
    try:
        plugin = subprocess.run(
            ["docker", "compose", "version"],
            cwd=ROOT,
            check=False,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if plugin.returncode == 0:
            return ["docker", "compose"]
    except OSError:
        pass
    standalone = shutil.which("docker-compose")
    if standalone:
        return [standalone]
    return ["docker", "compose"]


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reset", action="store_true", help="Explicitly reset named local development Compose volumes")
    parser.add_argument("--refresh-source", action="store_true", help="Extract source metadata for review; never imports it")
    parser.add_argument("--refresh-images", action="store_true", help="Regenerate original local demo PNGs")
    parser.add_argument("--validate-only", action="store_true", help="Validate frozen data and local image assets without network calls")
    parser.add_argument("--skip-orders", action="store_true")
    parser.add_argument("--skip-shipping", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    load_dotenv(ROOT.parent / ".env")
    args = parse_args(argv)
    try:
        if args.refresh_source:
            from extract_catalog import main as extract_main
            return extract_main([])
        if args.refresh_images:
            from generate_demo_images import main as image_main
            image_main()
            return 0
        if args.validate_only:
            from validate_dataset import main as validate_main
            return validate_main()
        if args.reset:
            reset_development()
            return 0
        if os.environ.get("SEED_ENV") not in {"development", "test"}:
            raise SeedError("refusing to seed: set SEED_ENV=development or SEED_ENV=test explicitly")
        runner = SeedRunner(args)
        runner.run()
        return 0
    except (SeedError, OSError, ValueError) as exc:
        print(f"seed failed: {compact_error(exc)}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
