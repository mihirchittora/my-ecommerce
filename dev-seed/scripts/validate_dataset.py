#!/usr/bin/env python3
"""Offline validation for the checked-in demo dataset."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
IMAGES = ROOT / "images"
SKU_RE = re.compile(r"^[A-Z0-9][A-Z0-9._-]{0,79}$")


class DatasetError(ValueError):
    pass


def load(name: str):
    return json.loads((DATA / name).read_text(encoding="utf-8"))


def validate() -> dict:
    categories = load("categories.json")
    product_doc = load("products.json")
    customers = load("customers.json")["customers"]
    inventory = load("inventory.json")
    scenarios = load("scenarios.json")["scenarios"]
    category_keys = {category["key"] for category in categories}
    if len(category_keys) != len(categories):
        raise DatasetError("duplicate category key")
    for category in categories:
        if category["parentKey"] and category["parentKey"] not in category_keys:
            raise DatasetError(f"missing parent category: {category['key']}")

    products = product_doc["products"]
    flat_variants = load("variants.json")["variants"]
    flat_skus = load("skus.json")["skus"]
    flat_images = load("images.json")["images"]
    if not 20 <= len(products) <= 30:
        raise DatasetError(f"expected 20-30 products, found {len(products)}")
    names: set[str] = set()
    skus: set[str] = set()
    image_files: set[str] = set()
    for product in products:
        if not product["name"].strip() or product["name"].lower() in names:
            raise DatasetError(f"duplicate or empty product name: {product['name']}")
        names.add(product["name"].lower())
        if product["categoryKey"] not in category_keys:
            raise DatasetError(f"unknown category for {product['name']}")
        source = product.get("source", {})
        for field in ("sourceName", "sourceUrl", "sourceExtractedAt"):
            if not source.get(field):
                raise DatasetError(f"missing source provenance {field} for {product['name']}")
        if not product["variants"]:
            raise DatasetError(f"product has no variants: {product['name']}")
        for variant in product["variants"]:
            sku = variant["sku"]
            if not SKU_RE.fullmatch(sku) or sku in skus:
                raise DatasetError(f"invalid or duplicate SKU: {sku}")
            skus.add(sku)
            if float(variant["price"]) <= 0 or variant["currency"] not in {"INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD"}:
                raise DatasetError(f"invalid price/currency for {sku}")
        if not product["images"]:
            raise DatasetError(f"product has no image metadata: {product['name']}")
        for image in product["images"]:
            filename = image["file"]
            if filename in image_files or not (IMAGES / filename).is_file():
                raise DatasetError(f"missing or duplicate local image: {filename}")
            image_files.add(filename)
            if not (IMAGES / filename).read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
                raise DatasetError(f"local image is not a PNG: {filename}")
            if not image.get("alt") or not image.get("mediaType"):
                raise DatasetError(f"incomplete image metadata: {filename}")

    if len(customers) != 8 or len({customer["email"] for customer in customers}) != len(customers):
        raise DatasetError("expected 8 unique development customers")
    if len(flat_variants) != len(skus) or len(flat_skus) != len(skus) or len(flat_images) != len(image_files):
        raise DatasetError("flat normalized dataset files are out of sync with products.json")
    if len(inventory["locations"]) != 3:
        raise DatasetError("expected exactly three inventory locations")
    if sum(inventory["unitsPerSkuByLocation"].values()) * len(skus) not in range(100, 301):
        raise DatasetError("planned InventoryUnit count must be between 100 and 300")
    if not scenarios:
        raise DatasetError("demo scenarios are empty")
    return {
        "products": len(products),
        "categories": len(categories),
        "variants": len(skus),
        "skus": len(skus),
        "images": len(image_files),
        "locations": len(inventory["locations"]),
        "plannedInventoryUnits": sum(inventory["unitsPerSkuByLocation"].values()) * len(skus),
        "customers": len(customers),
        "scenarios": len(scenarios),
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
