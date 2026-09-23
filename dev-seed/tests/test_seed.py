from __future__ import annotations

import contextlib
import io
import json
import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch
from urllib.error import URLError


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

import generate_dataset  # noqa: E402
import seed  # noqa: E402
import validate_dataset  # noqa: E402
from extract_catalog import parse_reference_html  # noqa: E402


class SeedDatasetTest(unittest.TestCase):
    def test_frozen_dataset_has_expected_shape_and_local_images(self):
        summary = validate_dataset.validate()
        self.assertEqual(summary["products"], 37)
        self.assertEqual(summary["topLevelCategories"], 10)
        self.assertEqual(summary["variants"], 111)
        self.assertEqual(summary["images"], 91)
        self.assertEqual(summary["plannedInventoryUnits"], 242)
        self.assertEqual(summary["customers"], 10)
        self.assertEqual(summary["orderScenarios"], 12)

    def test_generated_data_is_deterministic(self):
        self.assertEqual(generate_dataset.build_products(), generate_dataset.build_products())
        products = generate_dataset.build_products()
        self.assertEqual(len({variant["sku"] for product in products for variant in product["variants"]}), 111)
        self.assertEqual(len({product["slug"] for product in products}), 37)

    def test_source_parser_keeps_reference_hints_only(self):
        html = """
        <html><body>
          <a href='/category/sarees'>Sarees</a>
          <a href='/category/electronics'>Electronics</a>
          <a href='/p/original-lamp'>Original Lamp</a>
        </body></html>
        """
        result = parse_reference_html(html)
        self.assertEqual(result["sourceName"], "Meesho public marketplace reference")
        self.assertEqual(result["visibleCategoryHints"], ["Sarees", "Electronics"])
        self.assertEqual(result["visibleProductLinkHints"][0]["label"], "Original Lamp")

    def test_no_reference_brand_is_used_as_local_brand(self):
        products = json.loads((ROOT / "data" / "products.json").read_text(encoding="utf-8"))["products"]
        self.assertTrue(all("meesho" not in product["brand"].lower() for product in products))
        self.assertTrue(all(product["source"]["sourceName"] == "Meesho public marketplace reference" for product in products))

    def test_image_metadata_has_alt_text_provenance_and_primary(self):
        products = json.loads((ROOT / "data" / "products.json").read_text(encoding="utf-8"))["products"]
        for product in products:
            self.assertEqual(sum(image["primary"] for image in product["images"]), 1)
            for image in product["images"]:
                self.assertTrue(image["alt"])
                self.assertTrue(image["sourceName"])
                self.assertTrue(image["sourceAccessedAt"])

    def test_duplicate_and_orphan_detection(self):
        original_load = validate_dataset.load
        docs = {
            name: original_load(name)
            for name in ("categories.json", "products.json", "variants.json", "skus.json", "images.json", "customers.json", "inventory.json", "scenarios.json")
        }
        docs["skus.json"]["skus"].append(docs["skus.json"]["skus"][0].copy())
        with patch.object(validate_dataset, "load", side_effect=lambda name: docs[name]):
            with self.assertRaises(validate_dataset.DatasetError):
                validate_dataset.validate()

        docs = {name: original_load(name) for name in docs}
        docs["scenarios.json"]["scenarios"].append({"key": "orphan", "sku": "NOT-A-SKU"})
        with patch.object(validate_dataset, "load", side_effect=lambda name: docs[name]):
            with self.assertRaises(validate_dataset.DatasetError):
                validate_dataset.validate()

    def test_broken_source_image_is_rejected(self):
        original_load = validate_dataset.load
        docs = {name: original_load(name) for name in ("categories.json", "products.json", "variants.json", "skus.json", "images.json", "customers.json", "inventory.json", "scenarios.json")}
        docs["products.json"]["products"][0]["images"][0]["file"] = "missing/not-found.png"
        with patch.object(validate_dataset, "load", side_effect=lambda name: docs[name]):
            with self.assertRaises(validate_dataset.DatasetError):
                validate_dataset.validate()


class SeedSafetyTest(unittest.TestCase):
    def test_reset_protection(self):
        with patch.dict(os.environ, {"SEED_ENV": "production"}, clear=False):
            with self.assertRaises(seed.SeedError):
                seed.reset_development()
        with patch.dict(os.environ, {"APP_ENV": "production", "SEED_ENV": "development"}, clear=False):
            with self.assertRaises(seed.SeedError):
                seed.reset_development()

    def test_dry_run_does_not_require_service_urls_or_write_manifest(self):
        before = seed.MANIFEST.read_bytes() if seed.MANIFEST.exists() else None
        with patch.dict(os.environ, {"SEED_ENV": "production"}, clear=False):
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                self.assertEqual(seed.main(["--dry-run"]), 0)
        self.assertIn("no service or manifest mutations", output.getvalue())
        after = seed.MANIFEST.read_bytes() if seed.MANIFEST.exists() else None
        self.assertEqual(before, after)

    def test_service_unavailable_is_explicit(self):
        client = seed.ApiClient("catalog", "http://localhost:1")
        with patch("seed.urllib.request.urlopen", side_effect=URLError("connection refused")):
            with self.assertRaises(seed.SeedError) as error:
                client.request("GET", "/actuator/health")
        self.assertIn("catalog GET /actuator/health is unavailable", str(error.exception))

    def test_invalid_sku_and_category_are_rejected(self):
        original_load = validate_dataset.load
        docs = {name: original_load(name) for name in ("categories.json", "products.json", "variants.json", "skus.json", "images.json", "customers.json", "inventory.json", "scenarios.json")}
        docs["products.json"]["products"][0]["variants"][0]["sku"] = "bad sku"
        with patch.object(validate_dataset, "load", side_effect=lambda name: docs[name]):
            with self.assertRaises(validate_dataset.DatasetError):
                validate_dataset.validate()

        docs = {name: original_load(name) for name in docs}
        docs["products.json"]["products"][0]["categoryKey"] = "not-a-category"
        with patch.object(validate_dataset, "load", side_effect=lambda name: docs[name]):
            with self.assertRaises(validate_dataset.DatasetError):
                validate_dataset.validate()


if __name__ == "__main__":
    unittest.main()
