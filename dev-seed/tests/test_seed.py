from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from extract_catalog import parse_collection_html  # noqa: E402
from validate_dataset import validate  # noqa: E402


class SeedDatasetTest(unittest.TestCase):
    def test_frozen_dataset_has_expected_shape_and_local_images(self):
        summary = validate()
        self.assertEqual(summary["products"], 25)
        self.assertEqual(summary["categories"], 8)
        self.assertEqual(summary["skus"], 75)
        self.assertEqual(summary["images"], 25)
        self.assertEqual(summary["plannedInventoryUnits"], 300)

    def test_source_parser_normalizes_visible_product_cards(self):
        html = """
        <html><body>
          <a href='/products/sample-cocoa'><img alt='Sample Cocoa'></a>
          <span>₹ 700</span>
          <a href='/products/sample-cocoa'>Sample Cocoa</a>
          <a href='/products/bright-orchard'>Bright Orchard</a>
          <span>₹ 850</span>
        </body></html>
        """
        result = parse_collection_html(html)
        self.assertEqual([item["name"] for item in result["products"]], ["Sample Cocoa", "Bright Orchard"])
        self.assertEqual([item["price"] for item in result["products"]], [700, 850])
        self.assertEqual(result["filters"]["roastLevel"], ["Dark", "Light", "Medium", "Medium Dark"])

    def test_no_source_brand_is_used_as_local_brand(self):
        products = json.loads((ROOT / "data" / "products.json").read_text(encoding="utf-8"))["products"]
        self.assertTrue(all(product["brand"] == "Hearthline Demo Roasters" for product in products))
        self.assertTrue(all(product["source"]["sourceName"] == "Blue Tokai reference catalog" for product in products))


if __name__ == "__main__":
    unittest.main()
