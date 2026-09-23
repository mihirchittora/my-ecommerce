#!/usr/bin/env python3
"""Offline validation for the checked-in development/demo dataset."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
IMAGES = ROOT / "images"
SKU_RE = re.compile(r"^[A-Z0-9][A-Z0-9._-]{0,79}$")
REQUIRED_ROOTS = {
    "womens-fashion", "mens-fashion", "kids", "footwear", "bags-accessories",
    "beauty-personal-care", "home-kitchen", "electronics", "home-decor", "tools-lifestyle",
}
REQUIRED_SCENARIOS = {
    "womens-fashion-browsing", "out-of-stock-variant", "low-stock-product", "partial-shipment",
    "multiple-inventory-locations", "historical-price-change",
}


class DatasetError(ValueError):
    pass


def load(name: str):
    return json.loads((DATA / name).read_text(encoding="utf-8"))


def validate() -> dict:
    categories = load("categories.json")
    product_doc = load("products.json")
    variants_doc = load("variants.json")
    skus_doc = load("skus.json")
    images_doc = load("images.json")
    customers_doc = load("customers.json")
    inventory = load("inventory.json")
    scenarios_doc = load("scenarios.json")

    category_keys = {category["key"] for category in categories}
    if len(category_keys) != len(categories):
        raise DatasetError("duplicate category key")
    for category in categories:
        parent = category.get("parentKey")
        if parent and parent not in category_keys:
            raise DatasetError(f"missing parent category: {category['key']}")
    roots = {category["key"] for category in categories if category.get("parentKey") is None}
    if roots != REQUIRED_ROOTS:
        raise DatasetError(f"top-level categories mismatch: {sorted(roots)}")
    children_by_parent = {root: [] for root in roots}
    for category in categories:
        if category.get("parentKey"):
            children_by_parent[category["parentKey"]].append(category["key"])
    if any(not 2 <= len(children) <= 4 for children in children_by_parent.values()):
        raise DatasetError("every top-level category must have 2-4 subcategories")

    products = product_doc["products"]
    flat_variants = variants_doc["variants"]
    flat_skus = skus_doc["skus"]
    flat_images = images_doc["images"]
    if not 30 <= len(products) <= 40:
        raise DatasetError(f"expected 30-40 products, found {len(products)}")

    product_keys: set[str] = set()
    product_slugs: set[str] = set()
    skus: set[str] = set()
    image_files: set[str] = set()
    product_variant_rows = []
    product_image_rows = []
    for product in products:
        key = product["key"]
        if key in product_keys or not product["name"].strip():
            raise DatasetError(f"duplicate or empty product: {product.get('name')}")
        product_keys.add(key)
        if product["slug"] in product_slugs:
            raise DatasetError(f"duplicate product slug: {product['slug']}")
        product_slugs.add(product["slug"])
        if product["categoryKey"] not in category_keys or product["categoryKey"] in roots:
            raise DatasetError(f"product must belong to a subcategory: {product['name']}")
        source = product.get("source", {})
        for field in ("sourceName", "sourceUrl", "sourceAccessedAt"):
            if not source.get(field):
                raise DatasetError(f"missing source provenance {field} for {product['name']}")
        if not 1 <= len(product["images"]) <= 4:
            raise DatasetError(f"product must have 1-4 images: {product['name']}")
        if not product["images"][0].get("primary"):
            raise DatasetError(f"first image is not primary: {product['name']}")
        if not product["variants"]:
            raise DatasetError(f"product has no variants: {product['name']}")
        for variant in product["variants"]:
            sku = variant["sku"]
            if not SKU_RE.fullmatch(sku) or sku in skus:
                raise DatasetError(f"invalid or duplicate SKU: {sku}")
            skus.add(sku)
            if variant["status"] != "ACTIVE" or float(variant["price"]) <= 0 or variant["currency"] != "INR":
                raise DatasetError(f"invalid sellable variant: {sku}")
            if not variant.get("attributes"):
                raise DatasetError(f"variant has no filterable attributes: {sku}")
            product_variant_rows.append((product["key"], product["slug"], variant))
        for image in product["images"]:
            filename = image["file"]
            if filename in image_files or not (IMAGES / filename).is_file():
                raise DatasetError(f"missing or duplicate local image: {filename}")
            image_files.add(filename)
            if not (IMAGES / filename).read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
                raise DatasetError(f"local image is not a PNG: {filename}")
            for field in ("alt", "sourceName", "sourceAccessedAt", "mediaType"):
                if not image.get(field):
                    raise DatasetError(f"incomplete image metadata {field}: {filename}")
            product_image_rows.append((product["key"], product["slug"], image))

    if len(flat_variants) != len(skus) or len(flat_skus) != len(skus) or len(flat_images) != len(image_files):
        raise DatasetError("flat normalized dataset files are out of sync with product data")
    if not 80 <= len(skus) <= 120:
        raise DatasetError(f"expected 80-120 variants/SKUs, found {len(skus)}")
    gallery_sizes = {len(product["images"]) for product in products}
    if gallery_sizes != {1, 2, 3, 4}:
        raise DatasetError(f"gallery size coverage must include 1, 2, 3, and 4 images; found {gallery_sizes}")
    prices = [float(variant["price"]) for product in products for variant in product["variants"]]
    if min(prices) > 499 or max(prices) < 3000 or max(prices) < 15000:
        raise DatasetError("price bands must include low, mid-range, and higher-value products")
    if {row["sku"] for row in flat_skus} != skus:
        raise DatasetError("flat SKU file does not match product variants")
    if {row["file"] for row in flat_images} != image_files:
        raise DatasetError("flat image file does not match product galleries")

    customers = customers_doc["customers"]
    emails = {customer["email"] for customer in customers}
    if len(customers) != 10 or len(emails) != 10 or not all(email.endswith("@example.test") for email in emails):
        raise DatasetError("expected 10 unique synthetic example.test customers")
    locations = inventory["locations"]
    if len(locations) != 3 or {location["code"] for location in locations} != {"WH-MUM", "WH-DEL", "WH-BLR"}:
        raise DatasetError("expected WH-MUM, WH-DEL, and WH-BLR inventory locations")
    planned_units = len(skus) * sum(int(value) for value in inventory["unitsPerSkuByLocation"].values())
    planned_units += sum(int(entry["quantity"]) for entry in inventory.get("extraUnits", []))
    if not 100 <= planned_units <= 300:
        raise DatasetError(f"planned physical units must be between 100 and 300, found {planned_units}")

    scenarios = scenarios_doc["scenarios"]
    scenario_keys = {scenario["key"] for scenario in scenarios}
    if not REQUIRED_SCENARIOS <= scenario_keys:
        raise DatasetError(f"missing required scenarios: {sorted(REQUIRED_SCENARIOS - scenario_keys)}")
    customer_keys = {customer["key"] for customer in customers}
    for scenario in scenarios:
        if scenario.get("customerKey") and scenario["customerKey"] not in customer_keys:
            raise DatasetError(f"scenario references unknown customer: {scenario['key']}")
        if scenario.get("sku") and scenario["sku"] not in skus:
            raise DatasetError(f"scenario references unknown SKU: {scenario['sku']}")
        for item in scenario.get("items", []):
            if item["sku"] not in skus or int(item["quantity"]) < 1:
                raise DatasetError(f"scenario contains invalid cart/order SKU: {scenario['key']}")
    order_scenarios = [scenario for scenario in scenarios if scenario.get("orderReference")]
    if not 10 <= len(order_scenarios) <= 15:
        raise DatasetError(f"expected 10-15 order scenarios, found {len(order_scenarios)}")
    if len({scenario["orderReference"] for scenario in order_scenarios}) != len(order_scenarios):
        raise DatasetError("duplicate order reference")

    return {
        "products": len(products), "categories": len(categories), "topLevelCategories": len(roots),
        "subcategories": len(categories) - len(roots), "variants": len(skus), "skus": len(skus),
        "images": len(image_files), "locations": len(locations), "plannedInventoryUnits": planned_units,
        "customers": len(customers), "orderScenarios": len(order_scenarios), "scenarios": len(scenarios),
    }


def main() -> int:
    try:
        summary = validate()
        print(json.dumps(summary, indent=2))
        return 0
    except (OSError, KeyError, TypeError, ValueError) as exc:
        print(f"dataset validation failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
