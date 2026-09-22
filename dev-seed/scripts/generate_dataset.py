#!/usr/bin/env python3
"""Generate the checked-in, deterministic development dataset.

This file is deliberately boring: the generated JSON is the reviewable source
of truth, while this script makes it easy to regenerate it after a reviewed
source refresh. No random values or live HTTP calls are used here.
"""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
SOURCE_URL = "https://bluetokaicoffee.com/collections/roasted-and-ground-coffee-beans"


CATEGORIES = [
    {"key": "coffee", "name": "Coffee", "slug": "coffee", "parentKey": None},
    {"key": "single-estate", "name": "Single Estate", "slug": "single-estate", "parentKey": "coffee"},
    {"key": "blends", "name": "Blends", "slug": "blends", "parentKey": "coffee"},
    {"key": "sampler-packs", "name": "Sampler Packs", "slug": "sampler-packs", "parentKey": "coffee"},
    {"key": "light-roast", "name": "Light Roast", "slug": "light-roast", "parentKey": "coffee"},
    {"key": "medium-roast", "name": "Medium Roast", "slug": "medium-roast", "parentKey": "coffee"},
    {"key": "medium-dark-roast", "name": "Medium Dark Roast", "slug": "medium-dark-roast", "parentKey": "coffee"},
    {"key": "dark-roast", "name": "Dark Roast", "slug": "dark-roast", "parentKey": "coffee"},
]


PRODUCT_DEFINITIONS = [
    ("Highland Cocoa", "single-estate", "MEDIUM_DARK", "WITH_MILK", "CHOCOLATEY_NUTTY", "FRENCH_PRESS", "cocoa, dried fruit, roasted almond", 799, "Attikan Estate"),
    ("Ember Valley", "single-estate", "DARK", "WITH_MILK", "BOLD_BITTER", "ESPRESSO", "cocoa, toasted wood, bittersweet finish", 650, "Vienna Roast"),
    ("Rainline Honey", "single-estate", "MEDIUM", "WITHOUT_MILK", "FRUITY_PUNCHY", "POUROVER", "red fruit, honey, gentle citrus", 800, "Sampigehoney Estate"),
    ("Orchard Peak", "single-estate", "LIGHT", "WITHOUT_MILK", "DELICATE_COMPLEX", "POUROVER", "pear, stone fruit, almond", 850, "Salawara Estate"),
    ("Cedar Bloom", "single-estate", "MEDIUM", "WITH_OR_WITHOUT_MILK", "BALANCED", "AEROPRESS", "orange, malt, roasted hazelnut", 800, "Elkhill Estates"),
    ("Monsoon Trail", "single-estate", "MEDIUM_DARK", "WITH_MILK", "CHOCOLATEY_NUTTY", "SOUTH_INDIAN_FILTER", "cocoa nib, nutmeg, raisin", 700, "Monsoon Malabar AA"),
    ("Sunlit Spice", "single-estate", "MEDIUM", "WITH_OR_WITHOUT_MILK", "FRESH_FLAVOURFUL", "MOKA_POT", "citrus peel, brown spice, tea", 800, "M.S. Estate"),
    ("Nightjar Roast", "single-estate", "DARK", "WITH_MILK", "BOLD_BITTER", "ESPRESSO", "dark cocoa, prune, smoky sugar", 750, "Sandalwood Estate"),
    ("Canyon Bloom", "single-estate", "MEDIUM_DARK", "WITH_OR_WITHOUT_MILK", "BALANCED", "FRENCH_PRESS", "grapefruit, clove, toffee", 700, "St. Joseph Estate"),
    ("Stonefruit Drift", "single-estate", "LIGHT", "WITHOUT_MILK", "FRUITY_PUNCHY", "POUROVER", "stone fruit, jasmine, brown sugar", 1000, "Terroir Culture"),
    ("Lantern House", "single-estate", "MEDIUM", "WITH_OR_WITHOUT_MILK", "CHOCOLATEY_NUTTY", "AEROPRESS", "molasses, walnut, dark chocolate", 700, "Kolli Berri Estate"),
    ("Riverbend Dark", "single-estate", "DARK", "WITH_MILK", "BOLD_BITTER", "MOKA_POT", "dark chocolate, raisin, spice", 800, "Unakki Estate"),
    ("Golden Canopy", "single-estate", "MEDIUM", "WITHOUT_MILK", "FRESH_FLAVOURFUL", "POUROVER", "orange marmalade, dried date, tea", 800, "M.S. Estate"),
    ("Cloudline Estate", "single-estate", "LIGHT", "WITHOUT_MILK", "DELICATE_COMPLEX", "INVERTED_AEROPRESS", "floral citrus, apple, honey", 700, "Krishnagiri Estate"),
    ("Hearth Blend", "blends", "MEDIUM_DARK", "WITH_OR_WITHOUT_MILK", "BALANCED", "FRENCH_PRESS", "cocoa, toasted grain, dried fruit", 700, "Dhak Blend"),
    ("Porchlight Blend", "blends", "DARK", "WITH_MILK", "BOLD_BITTER", "SOUTH_INDIAN_FILTER", "dark cocoa, fruit jam, caramel", 700, "Rich and Bold Trio Pack"),
    ("Alpine Moss", "single-estate", "LIGHT", "WITHOUT_MILK", "DELICATE_COMPLEX", "POUROVER", "bergamot, cacao nib, soft fruit", 850, "Kalledevarapura Estate"),
    ("Saffron Gate", "single-estate", "MEDIUM", "WITH_OR_WITHOUT_MILK", "EXPERIMENTAL", "CHANNI", "citrus, malt, warm spice", 700, "Customised Sampler Pack"),
    ("Afterglow", "blends", "MEDIUM_DARK", "WITH_MILK", "CHOCOLATEY_NUTTY", "ESPRESSO", "cocoa, hazelnut, dried cherry", 650, "French Roast"),
    ("Meadow Signal", "single-estate", "LIGHT", "WITHOUT_MILK", "FRESH_FLAVOURFUL", "COLD_BREW", "apple, raisin, cocoa nib", 750, "Sampigehoney Estate"),
    ("Explorer Flight", "sampler-packs", "MEDIUM", "WITH_OR_WITHOUT_MILK", "EXPERIMENTAL", "AEROPRESS", "a rotating set of bright and balanced cups", 1170, "5-in-1 Explorer Pack"),
    ("Bold Flight", "sampler-packs", "DARK", "WITH_MILK", "BOLD_BITTER", "MOKA_POT", "three dark profiles for side-by-side tasting", 700, "The Rich and Bold Trio Pack"),
    ("Weather Notes Trio", "sampler-packs", "MEDIUM_DARK", "WITH_OR_WITHOUT_MILK", "CHOCOLATEY_NUTTY", "FRENCH_PRESS", "three comforting profiles for rainy afternoons", 650, "The Monsoon Trio"),
    ("Weekend Brew Set", "sampler-packs", "MEDIUM", "WITHOUT_MILK", "FRESH_FLAVOURFUL", "POUROVER", "small-batch samples for an unhurried weekend", 700, "Customised Sampler Pack"),
    ("Everyday Ground Set", "sampler-packs", "MEDIUM_DARK", "WITH_OR_WITHOUT_MILK", "BALANCED", "SOUTH_INDIAN_FILTER", "three approachable grinds for everyday brewing", 700, "Customised Sampler Pack"),
]


CUSTOMERS = [
    {"key": "customer01", "email": "demo.customer01@example.test", "firstName": "Demo", "lastName": "Customer 01", "phone": "+919000000001", "city": "Mumbai", "state": "Maharashtra", "postalCode": "400001"},
    {"key": "customer02", "email": "demo.customer02@example.test", "firstName": "Demo", "lastName": "Customer 02", "phone": "+919000000002", "city": "New Delhi", "state": "Delhi", "postalCode": "110001"},
    {"key": "customer03", "email": "demo.customer03@example.test", "firstName": "Demo", "lastName": "Customer 03", "phone": "+919000000003", "city": "Bengaluru", "state": "Karnataka", "postalCode": "560001"},
    {"key": "customer04", "email": "demo.customer04@example.test", "firstName": "Demo", "lastName": "Customer 04", "phone": "+919000000004", "city": "Pune", "state": "Maharashtra", "postalCode": "411001"},
    {"key": "customer05", "email": "demo.customer05@example.test", "firstName": "Demo", "lastName": "Customer 05", "phone": "+919000000005", "city": "Hyderabad", "state": "Telangana", "postalCode": "500001"},
    {"key": "customer06", "email": "demo.customer06@example.test", "firstName": "Demo", "lastName": "Customer 06", "phone": "+919000000006", "city": "Chennai", "state": "Tamil Nadu", "postalCode": "600001"},
    {"key": "customer07", "email": "demo.customer07@example.test", "firstName": "Demo", "lastName": "Customer 07", "phone": "+919000000007", "city": "Kolkata", "state": "West Bengal", "postalCode": "700001"},
    {"key": "customer08", "email": "demo.customer08@example.test", "firstName": "Demo", "lastName": "Customer 08", "phone": "+919000000008", "city": "Jaipur", "state": "Rajasthan", "postalCode": "302001"},
]


def slugify(value: str) -> str:
    value = value.lower().replace("&", "and")
    value = re.sub(r"[^a-z0-9]+", "-", value)
    return value.strip("-")


def sku_base(name: str) -> str:
    return re.sub(r"[^A-Z0-9]+", "-", name.upper()).strip("-")[:40]


def variants_for(name: str, roast: str, preference: str, flavour: str, equipment: str, base_price: int) -> list[dict]:
    base = sku_base(name)
    variants = []
    for format_code, format_label in (("WB", "WHOLE_BEAN"), ("GR", "GROUND")):
        variants.append({
            "key": f"{base}-{format_code}-250",
            "sku": f"{base}-{format_code}-250",
            "price": base_price,
            "currency": "INR",
            "status": "ACTIVE",
            "attributes": {
                "format": format_label,
                "packSize": "250G",
                "roastLevel": roast,
                "drinkingPreference": preference,
                "flavourProfile": flavour,
                "equipment": equipment,
            },
        })
    variants.append({
        "key": f"{base}-WB-500",
        "sku": f"{base}-WB-500",
        "price": round(base_price * 1.8),
        "currency": "INR",
        "status": "ACTIVE",
        "attributes": {
            "format": "WHOLE_BEAN",
            "packSize": "500G",
            "roastLevel": roast,
            "drinkingPreference": preference,
            "flavourProfile": flavour,
            "equipment": equipment,
        },
    })
    return variants


def build_products() -> list[dict]:
    products = []
    for index, (name, category, roast, preference, flavour, equipment, notes, price, source_title) in enumerate(PRODUCT_DEFINITIONS, start=1):
        slug = slugify(name)
        variants = variants_for(name, roast, preference, flavour, equipment, price)
        products.append({
            "key": f"product-{index:02d}",
            "name": name,
            "slug": slug,
            "categoryKey": category,
            "brand": "Hearthline Demo Roasters",
            "status": "ACTIVE",
            "description": f"Original development catalog copy: a {roast.replace('_', ' ').lower()} profile with {notes}.",
            "metadata": {
                "roastLevel": roast,
                "drinkingPreference": preference,
                "flavourProfile": flavour,
                "equipment": equipment,
                "tastingNotes": notes,
            },
            "pricing": {
                "demoCurrentPrice250g": price,
                "sourceReferencePrice250g": 700 if name == "Highland Cocoa" else price,
                "priceChangeDemo": {"before": 700, "after": 799} if name == "Highland Cocoa" else None,
            },
            "source": {
                "sourceName": "Blue Tokai reference catalog",
                "sourceUrl": SOURCE_URL,
                "sourceProductUrl": None,
                "sourceReferenceTitle": source_title,
                "sourceExtractedAt": "2026-09-22T00:00:00Z",
                "sourceImageUrl": None,
                "usage": "Public metadata reference only; product identity, copy, and image are original development data.",
            },
            "variants": variants,
            "images": [{
                "file": f"{slug}-01.png",
                "sourceUrl": None,
                "sourceImageUrl": None,
                "alt": f"{name} development coffee bag",
                "sortOrder": 0,
                "primary": True,
                "mediaType": "image/png",
                "width": 480,
                "height": 600,
                "provenance": "Generated locally by dev-seed; no third-party image is redistributed.",
            }],
        })
    return products


def build_inventory(products: list[dict]) -> dict:
    return {
        "locations": [
            {"key": "mumbai", "code": "WH-MUM", "name": "Mumbai Warehouse", "status": "ACTIVE"},
            {"key": "delhi", "code": "WH-DEL", "name": "Delhi Warehouse", "status": "ACTIVE"},
            {"key": "bengaluru", "code": "WH-BLR", "name": "Bengaluru Warehouse", "status": "ACTIVE"},
        ],
        "unitsPerSkuByLocation": {"WH-MUM": 2, "WH-DEL": 1, "WH-BLR": 1},
        "receiptReferencePrefix": "DEMO-RECEIPT",
        "reservationScenarios": [{
            "key": "reserved-cart-checkout",
            "sku": products[-1]["variants"][0]["sku"],
            "locationCode": "WH-MUM",
            "quantity": 2,
            "referenceId": "DEMO-RESERVATION-001",
        }],
        "adjustmentScenarios": [
            {"key": "damaged-unit", "sku": products[0]["variants"][0]["sku"], "locationCode": "WH-MUM", "reason": "DAMAGE", "referenceId": "DEMO-DAMAGE-001"},
            {"key": "lost-unit", "sku": products[1]["variants"][1]["sku"], "locationCode": "WH-DEL", "reason": "LOSS", "referenceId": "DEMO-LOSS-001"},
        ],
    }


def build_scenarios(products: list[dict]) -> list[dict]:
    sku = lambda product_index, variant_index=0: products[product_index - 1]["variants"][variant_index]["sku"]
    return [
        {"key": "catalog-browsing", "description": "Browse products, categories, filters, variants, and local images."},
        {"key": "cart", "customerKey": "customer03", "items": [{"sku": sku(3), "quantity": 1}, {"sku": sku(4, 1), "quantity": 2}]},
        {"key": "checkout-success-delivered", "customerKey": "customer01", "orderReference": "DEMO-ORDER-001", "items": [{"sku": sku(1), "quantity": 1}, {"sku": sku(1, 1), "quantity": 1}], "payment": "CAPTURED", "tracking": ["IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"], "refund": True},
        {"key": "payment-failure", "customerKey": "customer02", "orderReference": "DEMO-ORDER-002", "sku": sku(2), "quantity": 1, "payment": "FAILED", "tracking": []},
        {"key": "shipment-in-transit", "customerKey": "customer03", "orderReference": "DEMO-ORDER-003", "sku": sku(5), "quantity": 1, "payment": "CAPTURED", "tracking": ["IN_TRANSIT"]},
        {"key": "shipment-out-for-delivery", "customerKey": "customer04", "orderReference": "DEMO-ORDER-004", "sku": sku(6), "quantity": 1, "payment": "CAPTURED", "tracking": ["IN_TRANSIT", "OUT_FOR_DELIVERY"]},
        {"key": "pending-payment", "customerKey": "customer05", "orderReference": "DEMO-ORDER-005", "sku": sku(7), "quantity": 1, "payment": "PENDING", "tracking": []},
        {"key": "historical-price-change", "sku": sku(1), "before": 700, "after": 799, "description": "Highland Cocoa demonstrates a reference price of INR 700 and current demo price of INR 799; order snapshots are immutable."},
    ]


def write(name: str, payload: object) -> None:
    (DATA / name).write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    products = build_products()
    write("categories.json", CATEGORIES)
    write("products.json", {
        "seedVersion": "1.0.0",
        "datasetType": "DEVELOPMENT_DEMO_ONLY",
        "source": {"name": "Blue Tokai reference catalog", "url": SOURCE_URL, "extractedAt": "2026-09-22T00:00:00Z"},
        "products": products,
    })
    write("variants.json", {
        "seedVersion": "1.0.0",
        "variants": [
            {"productKey": product["key"], "productSlug": product["slug"], **variant}
            for product in products for variant in product["variants"]
        ],
    })
    write("skus.json", {
        "seedVersion": "1.0.0",
        "skus": [
            {"sku": variant["sku"], "productKey": product["key"], "variantKey": variant["key"]}
            for product in products for variant in product["variants"]
        ],
    })
    write("images.json", {
        "seedVersion": "1.0.0",
        "images": [
            {"productKey": product["key"], "productSlug": product["slug"], **image}
            for product in products for image in product["images"]
        ],
    })
    write("customers.json", {"seedVersion": "1.0.0", "customers": CUSTOMERS})
    write("inventory.json", build_inventory(products))
    write("scenarios.json", {"seedVersion": "1.0.0", "scenarios": build_scenarios(products)})
    print(f"Generated {len(CATEGORIES)} categories, {len(products)} products, {sum(len(p['variants']) for p in products)} variants/SKUs and {len(CUSTOMERS)} customers")


if __name__ == "__main__":
    main()
