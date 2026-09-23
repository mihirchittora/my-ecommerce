#!/usr/bin/env python3
"""Generate the deterministic, synthetic marketplace demo dataset.

The generated JSON is the reviewable seed source of truth.  The public
marketplace reference is used only for breadth of category/product ideas;
names, copy, SKUs, prices, customers, and images belong to this application.
No network calls or random values are used here.
"""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
SOURCE_URL = "https://www.meesho.com/"
SOURCE_ACCESSED_AT = "2026-09-23T00:00:00Z"
BRANDS = {
    "fashion": "Loom & Leaf",
    "mens": "Northline Studio",
    "kids": "BrightNest",
    "footwear": "StrideCraft",
    "bags": "CarryKind",
    "beauty": "Petal & Pure",
    "home": "Hearth & Hue",
    "electronics": "OrbitLane",
    "decor": "Mysa Living",
    "tools": "Everyday Forge",
}


def category(key: str, name: str, slug: str, parent_key: str | None = None) -> dict:
    return {"key": key, "name": name, "slug": slug, "parentKey": parent_key}


CATEGORIES = [
    category("womens-fashion", "Women's Fashion", "womens-fashion"),
    category("womens-sarees", "Sarees", "womens-sarees", "womens-fashion"),
    category("womens-kurtis", "Kurtis", "womens-kurtis", "womens-fashion"),
    category("womens-dresses", "Dresses", "womens-dresses", "womens-fashion"),
    category("womens-ethnic-sets", "Ethnic Sets", "womens-ethnic-sets", "womens-fashion"),
    category("mens-fashion", "Men's Fashion", "mens-fashion"),
    category("mens-tshirts", "T-Shirts", "mens-tshirts", "mens-fashion"),
    category("mens-shirts", "Shirts", "mens-shirts", "mens-fashion"),
    category("mens-trousers", "Trousers", "mens-trousers", "mens-fashion"),
    category("mens-jeans", "Jeans", "mens-jeans", "mens-fashion"),
    category("kids", "Kids", "kids"),
    category("kids-clothing", "Kids Clothing", "kids-clothing", "kids"),
    category("toys-games", "Toys & Games", "toys-games", "kids"),
    category("school-essentials", "School Essentials", "school-essentials", "kids"),
    category("footwear", "Footwear", "footwear"),
    category("womens-flats", "Women's Flats", "womens-flats", "footwear"),
    category("mens-sneakers", "Men's Sneakers", "mens-sneakers", "footwear"),
    category("sandals", "Sandals", "sandals", "footwear"),
    category("bags-accessories", "Bags & Accessories", "bags-accessories"),
    category("handbags", "Handbags", "handbags", "bags-accessories"),
    category("backpacks", "Backpacks", "backpacks", "bags-accessories"),
    category("wallets", "Wallets", "wallets", "bags-accessories"),
    category("travel-accessories", "Travel Accessories", "travel-accessories", "bags-accessories"),
    category("beauty-personal-care", "Beauty & Personal Care", "beauty-personal-care"),
    category("makeup", "Makeup", "makeup", "beauty-personal-care"),
    category("skincare", "Skincare", "skincare", "beauty-personal-care"),
    category("haircare", "Haircare", "haircare", "beauty-personal-care"),
    category("personal-care", "Personal Care", "personal-care", "beauty-personal-care"),
    category("home-kitchen", "Home & Kitchen", "home-kitchen"),
    category("cookware", "Cookware", "cookware", "home-kitchen"),
    category("storage", "Storage", "storage", "home-kitchen"),
    category("dining", "Dining", "dining", "home-kitchen"),
    category("cleaning", "Cleaning", "cleaning", "home-kitchen"),
    category("electronics", "Electronics", "electronics"),
    category("smartphones", "Smartphones", "smartphones", "electronics"),
    category("earphones", "Earphones", "earphones", "electronics"),
    category("smartwatches", "Smartwatches", "smartwatches", "electronics"),
    category("electronics-accessories", "Accessories", "electronics-accessories", "electronics"),
    category("home-decor", "Home Decor", "home-decor"),
    category("lighting", "Lighting", "lighting", "home-decor"),
    category("wall-decor", "Wall Decor", "wall-decor", "home-decor"),
    category("decorative-items", "Decorative Items", "decorative-items", "home-decor"),
    category("organizers", "Organizers", "organizers", "home-decor"),
    category("tools-lifestyle", "Tools & Lifestyle", "tools-lifestyle"),
    category("hand-tools", "Hand Tools", "hand-tools", "tools-lifestyle"),
    category("fitness-accessories", "Fitness Accessories", "fitness-accessories", "tools-lifestyle"),
    category("travel-utility", "Travel Accessories", "travel-utility", "tools-lifestyle"),
    category("utility-items", "Utility Items", "utility-items", "tools-lifestyle"),
]


# name, category, brand family, SKU prefix, base price, variant definitions
# Variant definitions are (suffix, price, attributes).  All fields are
# represented through Catalog's supported variant attribute map.
PRODUCT_DEFINITIONS = [
    ("Printed Cotton Saree", "womens-sarees", "fashion", "WFT-SAR-COTTON", 999, [
        ("BLU-FS", 999, {"color": "Blue", "size": "Free Size", "material": "Cotton"}),
        ("GRN-FS", 999, {"color": "Green", "size": "Free Size", "material": "Cotton"}),
        ("MAR-FS", 1099, {"color": "Maroon", "size": "Free Size", "material": "Cotton"}),
        ("YLW-FS", 999, {"color": "Yellow", "size": "Free Size", "material": "Cotton"}),
    ]),
    ("Embroidered Everyday Kurti", "womens-kurtis", "fashion", "WFT-KUR-EMB", 799, [
        ("PNK-S", 799, {"color": "Pink", "size": "S", "material": "Rayon"}),
        ("PNK-M", 799, {"color": "Pink", "size": "M", "material": "Rayon"}),
        ("PNK-L", 799, {"color": "Pink", "size": "L", "material": "Rayon"}),
        ("BLU-M", 849, {"color": "Blue", "size": "M", "material": "Rayon"}),
        ("BLU-XL", 849, {"color": "Blue", "size": "XL", "material": "Rayon"}),
    ]),
    ("Casual Floral Midi Dress", "womens-dresses", "fashion", "WFT-DRS-FLORAL", 899, [
        ("YLW-S", 899, {"color": "Yellow", "size": "S", "material": "Viscose"}),
        ("YLW-M", 899, {"color": "Yellow", "size": "M", "material": "Viscose"}),
        ("BLK-M", 949, {"color": "Black", "size": "M", "material": "Viscose"}),
    ]),
    ("Linen Blend Ethnic Set", "womens-ethnic-sets", "fashion", "WFT-SET-LINEN", 1299, [
        ("IVY-S", 1299, {"color": "Ivory", "size": "S", "material": "Linen Blend"}),
        ("IVY-M", 1299, {"color": "Ivory", "size": "M", "material": "Linen Blend"}),
        ("IVY-L", 1299, {"color": "Ivory", "size": "L", "material": "Linen Blend"}),
    ]),
    ("Soft Lounge Co-ord Set", "womens-dresses", "fashion", "WFT-COORD-LOUNGE", 899, [
        ("SAGE-S", 899, {"color": "Sage", "size": "S", "material": "Cotton"}),
        ("SAGE-M", 899, {"color": "Sage", "size": "M", "material": "Cotton"}),
        ("SAGE-L", 899, {"color": "Sage", "size": "L", "material": "Cotton"}),
    ]),
    ("Casual Cotton T-Shirt", "mens-tshirts", "mens", "MFT-TSH-COTTON", 399, [
        ("BLK-S", 399, {"color": "Black", "size": "S", "material": "Cotton"}),
        ("BLK-M", 399, {"color": "Black", "size": "M", "material": "Cotton"}),
        ("BLK-L", 399, {"color": "Black", "size": "L", "material": "Cotton"}),
        ("BLK-XL", 399, {"color": "Black", "size": "XL", "material": "Cotton"}),
        ("BLU-M", 449, {"color": "Blue", "size": "M", "material": "Cotton"}),
        ("BLU-L", 449, {"color": "Blue", "size": "L", "material": "Cotton"}),
    ]),
    ("Slim Fit Casual Shirt", "mens-shirts", "mens", "MFT-SHT-SLIM", 899, [
        ("WHT-M", 899, {"color": "White", "size": "M", "material": "Cotton"}),
        ("WHT-L", 899, {"color": "White", "size": "L", "material": "Cotton"}),
        ("BLU-M", 949, {"color": "Blue", "size": "M", "material": "Cotton"}),
        ("BLU-L", 949, {"color": "Blue", "size": "L", "material": "Cotton"}),
    ]),
    ("Regular Fit Everyday Trousers", "mens-trousers", "mens", "MFT-TRS-REG", 1099, [
        ("GRY-30", 1099, {"color": "Grey", "size": "30", "material": "Cotton Twill"}),
        ("GRY-32", 1099, {"color": "Grey", "size": "32", "material": "Cotton Twill"}),
        ("GRY-34", 1099, {"color": "Grey", "size": "34", "material": "Cotton Twill"}),
        ("GRY-36", 1099, {"color": "Grey", "size": "36", "material": "Cotton Twill"}),
    ]),
    ("Classic Denim Jeans", "mens-jeans", "mens", "MFT-JNS-CLASSIC", 1399, [
        ("IND-30", 1399, {"color": "Indigo", "size": "30", "material": "Denim"}),
        ("IND-32", 1399, {"color": "Indigo", "size": "32", "material": "Denim"}),
        ("IND-34", 1399, {"color": "Indigo", "size": "34", "material": "Denim"}),
        ("IND-36", 1399, {"color": "Indigo", "size": "36", "material": "Denim"}),
    ]),
    ("Textured Polo Tee", "mens-tshirts", "mens", "MFT-PLO-TEXT", 599, [
        ("NAV-M", 599, {"color": "Navy", "size": "M", "material": "Cotton Pique"}),
        ("NAV-L", 599, {"color": "Navy", "size": "L", "material": "Cotton Pique"}),
        ("OLV-XL", 649, {"color": "Olive", "size": "XL", "material": "Cotton Pique"}),
    ]),
    ("Kids Cotton Play Set", "kids-clothing", "kids", "KDS-SET-COTTON", 699, [
        ("RED-2Y", 699, {"color": "Red", "size": "2-3Y", "material": "Cotton"}),
        ("BLU-4Y", 699, {"color": "Blue", "size": "4-5Y", "material": "Cotton"}),
        ("GRE-6Y", 749, {"color": "Green", "size": "6-7Y", "material": "Cotton"}),
        ("PCH-8Y", 749, {"color": "Peach", "size": "8-9Y", "material": "Cotton"}),
    ]),
    ("Build & Learn Blocks", "toys-games", "kids", "KDS-TOY-BLOCKS", 499, [
        ("48PC", 499, {"color": "Multi", "size": "48 Pieces", "material": "ABS Plastic"}),
        ("96PC", 799, {"color": "Multi", "size": "96 Pieces", "material": "ABS Plastic"}),
    ]),
    ("School Day Backpack", "school-essentials", "kids", "KDS-BAG-SCHOOL", 699, [
        ("BLU-12L", 699, {"color": "Blue", "size": "12L", "material": "Polyester"}),
        ("RED-18L", 799, {"color": "Red", "size": "18L", "material": "Polyester"}),
        ("GRN-20L", 899, {"color": "Green", "size": "20L", "material": "Polyester"}),
    ]),
    ("Rainbow Art Kit", "toys-games", "kids", "KDS-ART-RNBW", 299, [
        ("24PC", 299, {"color": "Multi", "size": "24 Pieces", "material": "Non-toxic"}),
        ("48PC", 499, {"color": "Multi", "size": "48 Pieces", "material": "Non-toxic"}),
    ]),
    ("Women's Everyday Flats", "womens-flats", "footwear", "FTW-FLT-EVERYDAY", 699, [
        ("BLK-07", 699, {"color": "Black", "size": "7", "material": "Synthetic"}),
        ("BLK-08", 699, {"color": "Black", "size": "8", "material": "Synthetic"}),
        ("BLK-09", 699, {"color": "Black", "size": "9", "material": "Synthetic"}),
        ("BEG-08", 749, {"color": "Beige", "size": "8", "material": "Synthetic"}),
    ]),
    ("Men's Street Sneakers", "mens-sneakers", "footwear", "FTW-SNK-STREET", 1599, [
        ("BLK-07", 1599, {"color": "Black", "size": "7", "material": "Mesh"}),
        ("BLK-08", 1599, {"color": "Black", "size": "8", "material": "Mesh"}),
        ("BLK-09", 1599, {"color": "Black", "size": "9", "material": "Mesh"}),
        ("WHT-08", 1699, {"color": "White", "size": "8", "material": "Mesh"}),
    ]),
    ("Casual Comfort Sandals", "sandals", "footwear", "FTW-SND-COMFORT", 599, [
        ("TAN-07", 599, {"color": "Tan", "size": "7", "material": "EVA"}),
        ("TAN-08", 599, {"color": "Tan", "size": "8", "material": "EVA"}),
        ("TAN-09", 599, {"color": "Tan", "size": "9", "material": "EVA"}),
    ]),
    ("Kids Velcro Sneakers", "sandals", "footwear", "FTW-KID-VELCRO", 799, [
        ("BLU-11", 799, {"color": "Blue", "size": "11", "material": "Mesh"}),
        ("BLU-12", 799, {"color": "Blue", "size": "12", "material": "Mesh"}),
        ("BLU-13", 849, {"color": "Blue", "size": "13", "material": "Mesh"}),
    ]),
    ("Everyday Handbag", "handbags", "bags", "BAG-HANDBAG-EVERY", 899, [
        ("BLK-STD", 899, {"color": "Black", "size": "Standard", "material": "Faux Leather"}),
        ("TAN-STD", 899, {"color": "Tan", "size": "Standard", "material": "Faux Leather"}),
        ("WNE-STD", 949, {"color": "Wine", "size": "Standard", "material": "Faux Leather"}),
        ("MNT-STD", 899, {"color": "Mint", "size": "Standard", "material": "Faux Leather"}),
    ]),
    ("Laptop Backpack", "backpacks", "bags", "BAG-BACKPACK-LAP", 1299, [
        ("BLK-15L", 1299, {"color": "Black", "size": "15L", "material": "Polyester"}),
        ("GRY-15L", 1299, {"color": "Grey", "size": "15L", "material": "Polyester"}),
        ("NAV-17L", 1499, {"color": "Navy", "size": "17L", "material": "Polyester"}),
    ]),
    ("Travel Wallet", "wallets", "bags", "BAG-WALLET-TRAVEL", 499, [
        ("BLK-STD", 499, {"color": "Black", "size": "Standard", "material": "Vegan Leather"}),
        ("TAN-STD", 499, {"color": "Tan", "size": "Standard", "material": "Vegan Leather"}),
        ("TEA-STD", 549, {"color": "Teal", "size": "Standard", "material": "Vegan Leather"}),
    ]),
    ("Dewdrop Face Serum", "skincare", "beauty", "BPC-SERUM-DEWDROP", 599, [
        ("10ML", 599, {"color": "Clear", "size": "10ml", "material": "Plant-based blend", "skinType": "All Skin"}),
        ("30ML", 999, {"color": "Clear", "size": "30ml", "material": "Plant-based blend", "skinType": "All Skin"}),
    ]),
    ("Velvet Lip Color Set", "makeup", "beauty", "BPC-LIP-VELVET", 699, [
        ("ROSE-3PC", 699, {"color": "Rose", "size": "3 Shades", "material": "Cream Finish"}),
        ("NUDE-3PC", 699, {"color": "Nude", "size": "3 Shades", "material": "Cream Finish"}),
        ("BERRY-3PC", 749, {"color": "Berry", "size": "3 Shades", "material": "Cream Finish"}),
    ]),
    ("Herbal Shine Shampoo", "haircare", "beauty", "BPC-SHAMPOO-HERB", 349, [
        ("250ML", 349, {"color": "Amber", "size": "250ml", "material": "Herbal blend", "hairType": "All Hair"}),
        ("500ML", 599, {"color": "Amber", "size": "500ml", "material": "Herbal blend", "hairType": "All Hair"}),
    ]),
    ("Granite Non-Stick Cookware Set", "cookware", "home", "HOM-COOK-GRANITE", 1299, [
        ("2PC", 1299, {"color": "Black", "size": "2 Pieces", "material": "Aluminium"}),
        ("5PC", 2199, {"color": "Black", "size": "5 Pieces", "material": "Aluminium"}),
        ("7PC", 2999, {"color": "Black", "size": "7 Pieces", "material": "Aluminium"}),
    ]),
    ("ClearSpace Storage Box Set", "storage", "home", "HOM-BOX-STORAGE", 699, [
        ("3PC", 699, {"color": "Clear", "size": "3 Pieces", "material": "Food-safe Plastic"}),
        ("6PC", 1199, {"color": "Clear", "size": "6 Pieces", "material": "Food-safe Plastic"}),
    ]),
    ("Meadow Dinner Set", "dining", "home", "HOM-DINE-MEADOW", 1599, [
        ("12PC", 1599, {"color": "Ivory", "size": "12 Pieces", "material": "Stoneware"}),
        ("18PC", 2299, {"color": "Ivory", "size": "18 Pieces", "material": "Stoneware"}),
    ]),
    ("FreshStart Cleaning Kit", "cleaning", "home", "HOM-CLEAN-FRESH", 299, [
        ("BASIC", 299, {"color": "Multi", "size": "Basic Kit", "material": "Microfiber"}),
        ("PLUS", 499, {"color": "Multi", "size": "Plus Kit", "material": "Microfiber"}),
    ]),
    ("Nova Android Smartphone", "smartphones", "electronics", "ELE-PHN-NOVA", 14999, [
        ("BLK-128", 14999, {"color": "Black", "size": "128GB", "storage": "128GB", "ram": "8GB"}),
        ("BLK-256", 16999, {"color": "Black", "size": "256GB", "storage": "256GB", "ram": "8GB"}),
        ("BLU-128", 14999, {"color": "Blue", "size": "128GB", "storage": "128GB", "ram": "8GB"}),
        ("BLU-256", 16999, {"color": "Blue", "size": "256GB", "storage": "256GB", "ram": "8GB"}),
    ]),
    ("AirLoop Wireless Earbuds", "earphones", "electronics", "ELE-EAR-AIRLOOP", 1999, [
        ("WHT-STD", 1999, {"color": "White", "size": "Standard", "material": "ABS Plastic"}),
        ("BLK-STD", 1999, {"color": "Black", "size": "Standard", "material": "ABS Plastic"}),
    ]),
    ("PulseFit Smartwatch", "smartwatches", "electronics", "ELE-WAT-PULSE", 2499, [
        ("BLK-STD", 2499, {"color": "Black", "size": "Standard", "material": "Aluminium"}),
        ("BLU-STD", 2499, {"color": "Blue", "size": "Standard", "material": "Aluminium"}),
        ("ROS-STD", 2599, {"color": "Rose", "size": "Standard", "material": "Aluminium"}),
    ]),
    ("Halo Table Lamp", "lighting", "decor", "DEC-LMP-HALO", 1299, [
        ("WHT-STD", 1299, {"color": "White", "size": "Standard", "material": "Ceramic"}),
        ("AMB-STD", 1399, {"color": "Amber", "size": "Standard", "material": "Ceramic"}),
    ]),
    ("Gallery Wall Art Set", "wall-decor", "decor", "DEC-ART-GALLERY", 899, [
        ("3PC", 899, {"color": "Multi", "size": "3 Pieces", "material": "Canvas"}),
        ("5PC", 1299, {"color": "Multi", "size": "5 Pieces", "material": "Canvas"}),
    ]),
    ("Terracotta Accent Vase", "decorative-items", "decor", "DEC-VASE-TERRA", 599, [
        ("SML", 599, {"color": "Terracotta", "size": "Small", "material": "Ceramic"}),
        ("MED", 799, {"color": "Terracotta", "size": "Medium", "material": "Ceramic"}),
    ]),
    ("Forge Multi-purpose Tool Kit", "hand-tools", "tools", "TLS-TOOL-FORGE", 999, [
        ("12PC", 999, {"color": "Red", "size": "12 Pieces", "material": "Chrome Vanadium"}),
        ("24PC", 1599, {"color": "Red", "size": "24 Pieces", "material": "Chrome Vanadium"}),
    ]),
    ("Flexi Resistance Band Set", "fitness-accessories", "tools", "TLS-FIT-BANDS", 399, [
        ("LIGHT", 399, {"color": "Multi", "size": "Light Resistance", "material": "Latex"}),
        ("HEAVY", 599, {"color": "Multi", "size": "Heavy Resistance", "material": "Latex"}),
    ]),
    ("PackRight Travel Organizer", "travel-utility", "tools", "TLS-ORG-PACK", 699, [
        ("3PC", 699, {"color": "Grey", "size": "3 Pieces", "material": "Nylon"}),
        ("6PC", 999, {"color": "Grey", "size": "6 Pieces", "material": "Nylon"}),
    ]),
]


CUSTOMERS = [
    {"key": f"customer{index:02d}", "email": f"demo.customer{index:02d}@example.test", "firstName": "Demo", "lastName": f"Customer {index:02d}",
     "phone": f"+919000000{index:03d}", "city": city, "state": state, "postalCode": postal}
    for index, (city, state, postal) in enumerate([
        ("Mumbai", "Maharashtra", "400001"), ("New Delhi", "Delhi", "110001"), ("Bengaluru", "Karnataka", "560001"),
        ("Pune", "Maharashtra", "411001"), ("Hyderabad", "Telangana", "500001"), ("Chennai", "Tamil Nadu", "600001"),
        ("Kolkata", "West Bengal", "700001"), ("Jaipur", "Rajasthan", "302001"), ("Lucknow", "Uttar Pradesh", "226001"),
        ("Ahmedabad", "Gujarat", "380001")
    ], start=1)
]


def slugify(value: str) -> str:
    value = value.lower().replace("&", "and")
    return re.sub(r"[^a-z0-9]+", "-", value).strip("-")


def build_products() -> list[dict]:
    products = []
    for index, (name, category_key, brand_key, sku_prefix, base_price, definitions) in enumerate(PRODUCT_DEFINITIONS, start=1):
        slug = slugify(name)
        variants = []
        for suffix, price, attributes in definitions:
            variants.append({
                "key": f"{sku_prefix}-{suffix}", "sku": f"{sku_prefix}-{suffix}", "price": price,
                "currency": "INR", "status": "ACTIVE", "attributes": attributes,
            })
        image_count = 1 + ((index - 1) % 4)
        images = []
        for image_index in range(1, image_count + 1):
            images.append({
                "file": f"{slug}/{slug}-{image_index:02d}.png", "sourceUrl": None,
                "alt": f"{name} demo image {image_index}", "sortOrder": image_index - 1,
                "primary": image_index == 1, "mediaType": "image/png", "width": 480, "height": 600,
                "sourceName": "Generated local demo asset", "sourceAccessedAt": SOURCE_ACCESSED_AT,
                "provenance": "Original deterministic artwork generated locally; no third-party image hosting.",
            })
        products.append({
            "key": f"product-{index:02d}", "name": name, "slug": slug, "categoryKey": category_key,
            "brand": BRANDS[brand_key], "status": "ACTIVE",
            "description": f"A practical {name.lower()} designed for everyday use, with easy-to-compare options and dependable demo availability.",
            "source": {"sourceName": "Meesho public marketplace reference", "sourceUrl": SOURCE_URL,
                        "sourceAccessedAt": SOURCE_ACCESSED_AT,
                        "usage": "Category and product variety reference only; identity, copy, prices, and images are original demo data."},
            "variants": variants, "images": images,
            "priceRange": {"min": min(v["price"] for v in variants), "max": max(v["price"] for v in variants), "base": base_price},
        })
    return products


def build_inventory(products: list[dict]) -> dict:
    all_skus = [variant["sku"] for product in products for variant in product["variants"]]
    # Two physical units per SKU are enough for API demonstrations. Selected
    # SKUs get one extra unit at a third location so location distribution and
    # multi-quantity reservations are both exercised without exceeding 300.
    extra = [{"sku": sku, "locationCode": ("WH-MUM" if sku == "MFT-TSH-COTTON-BLK-M" else "WH-BLR"), "quantity": 1}
             for sku in all_skus[:20]]
    return {
        "locations": [
            {"key": "mumbai", "code": "WH-MUM", "name": "Mumbai Warehouse", "status": "ACTIVE"},
            {"key": "delhi", "code": "WH-DEL", "name": "Delhi Warehouse", "status": "ACTIVE"},
            {"key": "bengaluru", "code": "WH-BLR", "name": "Bengaluru Warehouse", "status": "ACTIVE"},
        ],
        "unitsPerSkuByLocation": {"WH-MUM": 1, "WH-DEL": 1, "WH-BLR": 0},
        "extraUnits": extra,
        "receiptReferencePrefix": "DEMO-RECEIPT-V2",
        "reservationScenarios": [
            {"key": "reserved-stock", "sku": "BAG-BACKPACK-LAP-BLK-15L", "locationCode": "WH-MUM", "quantity": 1, "referenceId": "DEMO-MARKETPLACE-RESERVATION-001"}
        ],
        "adjustmentScenarios": [
            {"key": "damaged-unit", "sku": "ELE-PHN-NOVA-BLU-128", "locationCode": "WH-MUM", "quantity": -1, "reason": "DAMAGE", "referenceId": "DEMO-DAMAGE-001"},
            {"key": "lost-unit", "sku": "BPC-SHAMPOO-HERB-250ML", "locationCode": "WH-DEL", "quantity": -1, "reason": "LOSS", "referenceId": "DEMO-LOSS-001"},
            {"key": "low-stock", "sku": "BAG-HANDBAG-EVERY-BLK-STD", "locationCode": "WH-MUM", "quantity": -1, "reason": "LOSS", "referenceId": "DEMO-LOW-STOCK-001"},
            {"key": "out-of-stock", "sku": "ELE-PHN-NOVA-BLK-256", "locationCode": "WH-MUM", "quantity": -1, "reason": "DAMAGE", "referenceId": "DEMO-OOS-MUM-001"},
            {"key": "out-of-stock", "sku": "ELE-PHN-NOVA-BLK-256", "locationCode": "WH-DEL", "quantity": -1, "reason": "DAMAGE", "referenceId": "DEMO-OOS-DEL-001"},
        ],
    }


def sku(products: list[dict], product_index: int, variant_index: int = 0) -> str:
    return products[product_index - 1]["variants"][variant_index]["sku"]


def build_scenarios(products: list[dict]) -> list[dict]:
    return [
        {"key": "womens-fashion-browsing", "description": "Browse Women's Fashion products, color/size facets, galleries, and prices."},
        {"key": "out-of-stock-variant", "sku": sku(products, 29, 1), "description": "Nova Android Smartphone Black / 256GB has zero available physical units."},
        {"key": "low-stock-product", "sku": sku(products, 19, 0), "description": "Everyday Handbag Black / Standard retains one available unit."},
        {"key": "cart-01", "customerKey": "customer01", "items": [{"sku": sku(products, 6, 1), "quantity": 2}]},
        {"key": "cart-02", "customerKey": "customer02", "items": [{"sku": sku(products, 19, 0), "quantity": 1}]},
        {"key": "cart-03", "customerKey": "customer03", "items": [{"sku": sku(products, 29, 0), "quantity": 1}]},
        {"key": "cart-04", "customerKey": "customer04", "items": [{"sku": sku(products, 1, 0), "quantity": 1}]},
        {"key": "cart-05", "customerKey": "customer05", "items": [{"sku": sku(products, 22, 0), "quantity": 1}]},
        {"key": "cart-06", "customerKey": "customer06", "items": [{"sku": sku(products, 25, 0), "quantity": 1}]},
        {"key": "cart-07", "customerKey": "customer07", "items": [{"sku": sku(products, 35, 0), "quantity": 1}]},
        {"key": "checkout-online-delivered", "customerKey": "customer01", "orderReference": "DEMO-MARKETPLACE-ORDER-001",
         "items": [{"sku": sku(products, 6, 1), "quantity": 2}], "paymentMethod": "ONLINE", "payment": "CAPTURED",
         "tracking": ["LABEL_CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"], "refund": True},
        {"key": "online-payment-failed", "customerKey": "customer02", "orderReference": "DEMO-MARKETPLACE-ORDER-002",
         "items": [{"sku": sku(products, 7, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "FAILED", "tracking": []},
        {"key": "online-payment-pending", "customerKey": "customer03", "orderReference": "DEMO-MARKETPLACE-ORDER-003",
         "items": [{"sku": sku(products, 8, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "PENDING", "tracking": []},
        {"key": "online-in-transit", "customerKey": "customer04", "orderReference": "DEMO-MARKETPLACE-ORDER-004",
         "items": [{"sku": sku(products, 9, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "CAPTURED", "tracking": ["IN_TRANSIT"]},
        {"key": "online-out-for-delivery", "customerKey": "customer05", "orderReference": "DEMO-MARKETPLACE-ORDER-005",
         "items": [{"sku": sku(products, 10, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "CAPTURED", "tracking": ["IN_TRANSIT", "OUT_FOR_DELIVERY"]},
        {"key": "cod-delivered-collected", "customerKey": "customer06", "orderReference": "DEMO-MARKETPLACE-ORDER-006",
         "items": [{"sku": sku(products, 11, 1), "quantity": 1}], "paymentMethod": "CASH_ON_DELIVERY", "payment": "COD_PENDING_COLLECTION",
         "tracking": ["IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"], "collectCod": True},
        {"key": "cancelled-order", "customerKey": "customer07", "orderReference": "DEMO-MARKETPLACE-ORDER-007",
         "items": [{"sku": sku(products, 12, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "PENDING", "tracking": [], "cancel": True},
        {"key": "online-shipped", "customerKey": "customer08", "orderReference": "DEMO-MARKETPLACE-ORDER-008",
         "items": [{"sku": sku(products, 15, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "CAPTURED", "tracking": []},
        {"key": "cod-shipped", "customerKey": "customer09", "orderReference": "DEMO-MARKETPLACE-ORDER-009",
         "items": [{"sku": sku(products, 16, 0), "quantity": 1}], "paymentMethod": "CASH_ON_DELIVERY", "payment": "COD_PENDING_COLLECTION",
         "tracking": ["IN_TRANSIT"], "collectCod": False},
        {"key": "online-confirmed", "customerKey": "customer10", "orderReference": "DEMO-MARKETPLACE-ORDER-010",
         "items": [{"sku": sku(products, 21, 0), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "CAPTURED", "tracking": [], "createShipment": False},
        {"key": "online-refund", "customerKey": "customer03", "orderReference": "DEMO-MARKETPLACE-ORDER-011",
         "items": [{"sku": sku(products, 22, 1), "quantity": 1}], "paymentMethod": "ONLINE", "payment": "CAPTURED", "tracking": [], "refund": True},
        {"key": "cod-pending", "customerKey": "customer04", "orderReference": "DEMO-MARKETPLACE-ORDER-012",
         "items": [{"sku": sku(products, 37, 0), "quantity": 1}], "paymentMethod": "CASH_ON_DELIVERY", "payment": "COD_PENDING_COLLECTION",
         "tracking": [], "createShipment": False},
        {"key": "historical-price-change", "sku": sku(products, 6, 1), "before": 349, "after": 399,
         "description": "Casual Cotton T-Shirt Black / M is staged at INR 349 during DEMO-MARKETPLACE-ORDER-001 creation, then restored to INR 399."},
        {"key": "partial-shipment", "supportedByCurrentService": False,
         "description": "Documented scenario only: itemized Shipping requires the exact full reservation unit set, so partial itemized shipment is not executable without an API change."},
        {"key": "multiple-inventory-locations", "description": "Stock is distributed across WH-MUM, WH-DEL, and selected WH-BLR receipts."},
    ]


def write(name: str, payload: object) -> None:
    (DATA / name).write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    products = build_products()
    write("categories.json", CATEGORIES)
    write("products.json", {"seedVersion": "2.0.0", "datasetType": "DEVELOPMENT_DEMO_ONLY",
                             "source": {"name": "Meesho public marketplace reference", "url": SOURCE_URL,
                                        "accessedAt": SOURCE_ACCESSED_AT,
                                        "note": "Reference only; all catalog identity, copy, prices, and images are original synthetic data."},
                             "products": products})
    write("variants.json", {"seedVersion": "2.0.0", "variants": [
        {"productKey": product["key"], "productSlug": product["slug"], **variant}
        for product in products for variant in product["variants"]
    ]})
    write("skus.json", {"seedVersion": "2.0.0", "skus": [
        {"sku": variant["sku"], "productKey": product["key"], "variantKey": variant["key"]}
        for product in products for variant in product["variants"]
    ]})
    write("images.json", {"seedVersion": "2.0.0", "images": [
        {"productKey": product["key"], "productSlug": product["slug"], **image}
        for product in products for image in product["images"]
    ]})
    write("customers.json", {"seedVersion": "2.0.0", "customers": CUSTOMERS})
    write("inventory.json", build_inventory(products))
    write("scenarios.json", {"seedVersion": "2.0.0", "scenarios": build_scenarios(products)})
    print(f"Generated {len(CATEGORIES)} categories, {len(products)} products, "
          f"{sum(len(p['variants']) for p in products)} variants/SKUs, "
          f"{sum(len(p['images']) for p in products)} images, and {len(CUSTOMERS)} customers")


if __name__ == "__main__":
    main()
