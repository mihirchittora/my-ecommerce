#!/usr/bin/env python3
"""Extract reviewable public reference metadata once; never used at runtime.

The source is a Shopify collection page. The parser intentionally keeps only
small, public reference fields and writes a human-reviewable JSON artifact.
The frozen application dataset remains ``data/products.json`` until a person
reviews and regenerates it.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from html import unescape
from pathlib import Path
from urllib.request import Request, urlopen


DEFAULT_URL = "https://bluetokaicoffee.com/collections/roasted-and-ground-coffee-beans"
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "data" / "source-extracted.json"

FILTERS = {
    "roastLevel": ["Dark", "Light", "Medium", "Medium Dark"],
    "drinkingPreference": ["With Milk", "With or Without Milk", "Without Milk"],
    "flavourProfile": ["Balanced", "Bold and Bitter", "Chocolatey and Nutty", "Delicate and Complex", "Experimental", "Fresh and Flavourful", "Fruity and Punchy"],
    "equipment": ["Aeropress", "Channi", "Cold Brew", "Espresso", "French Press", "Inverted Aeropress", "Moka Pot", "Pourover", "South Indian Filter"],
}


def clean(value: str) -> str:
    return re.sub(r"\s+", " ", unescape(value or "")).strip()


def price_from(text: str, center: int | None = None) -> int | None:
    matches = list(re.finditer(r"₹\s*([\d,]+)", text))
    if not matches:
        return None
    match = min(matches, key=lambda item: abs(item.start() - center)) if center is not None else matches[-1]
    return int(match.group(1).replace(",", ""))


def parse_collection_html(html: str, source_url: str = DEFAULT_URL) -> dict:
    """Parse product-card-like anchors from a collection HTML document.

    This is deliberately tolerant of Shopify theme markup changes. It first
    looks for JSON-LD product objects, then falls back to visible product
    links and a nearby price. It does not copy descriptions or branding.
    """

    products: list[dict] = []
    seen: set[str] = set()

    for block in re.findall(r"<script[^>]+type=[\"']application/ld\+json[\"'][^>]*>(.*?)</script>", html, re.I | re.S):
        try:
            payload = json.loads(unescape(block.strip()))
        except json.JSONDecodeError:
            continue
        values = payload if isinstance(payload, list) else [payload]
        for value in values:
            if not isinstance(value, dict) or value.get("@type") != "Product":
                continue
            name = clean(str(value.get("name", "")))
            if not name or name.lower() in seen:
                continue
            offers = value.get("offers") if isinstance(value.get("offers"), dict) else {}
            products.append({
                "name": name,
                "price": price_from(str(offers.get("price", ""))) or value.get("price"),
                "currency": offers.get("priceCurrency") or "INR",
                "sourceProductUrl": value.get("url") or source_url,
                "sourceImageUrl": value.get("image"),
                "visibleMetadata": {},
            })
            seen.add(name.lower())

    for match in re.finditer(r"href=[\"']([^\"']*/products/[^\"']+)[\"'][^>]*>(.*?)</a>", html, re.I | re.S):
        href = unescape(match.group(1)).split("?")[0]
        visible_name = clean(re.sub(r"<[^>]+>", " ", match.group(2)))
        if not visible_name:
            continue
        context_start = max(0, match.start() - 1200)
        context = html[context_start:min(len(html), match.end() + 1200)]
        name = visible_name
        if name.lower() in {"buy now", "add", "quick view"}:
            continue
        if name.lower() in seen:
            continue
        products.append({
            "name": name,
            "price": price_from(context, match.start() - context_start),
            "currency": "INR",
            "sourceProductUrl": href if href.startswith("http") else "https://bluetokaicoffee.com" + href,
            "sourceImageUrl": None,
            "visibleMetadata": {},
        })
        seen.add(name.lower())

    products = [product for product in products if product["price"] is not None and int(product["price"]) > 0]
    return {
        "sourceName": "Blue Tokai reference catalog",
        "sourceUrl": source_url,
        "extractedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "filters": FILTERS,
        "products": products,
        "notes": [
            "Reference metadata only; this artifact is not a production catalog.",
            "Review product links, prices, and provenance before regenerating the frozen demo dataset.",
        ],
    }


def fetch(url: str) -> str:
    request = Request(url, headers={"User-Agent": "my-ecommerce-dev-seed/1.0"})
    with urlopen(request, timeout=15) as response:
        content_type = response.headers.get("Content-Type", "")
        if "text/html" not in content_type.lower():
            raise RuntimeError(f"reference source did not return HTML: {content_type}")
        body = response.read(5 * 1024 * 1024 + 1)
        if len(body) > 5 * 1024 * 1024:
            raise RuntimeError("reference page exceeds the 5 MiB safety limit")
        return body.decode(response.headers.get_content_charset() or "utf-8", errors="replace")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default=DEFAULT_URL)
    parser.add_argument("--html-file", type=Path, help="Parse a saved HTML fixture without network access")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args(argv)
    try:
        html = args.html_file.read_text(encoding="utf-8") if args.html_file else fetch(args.url)
        payload = parse_collection_html(html, args.url)
        if not payload["products"]:
            raise RuntimeError("no priced product cards were extracted; keep the frozen dataset unchanged and review the source markup")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Extracted {len(payload['products'])} reference products to {args.output}")
        return 0
    except Exception as exc:
        print(f"source extraction failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
