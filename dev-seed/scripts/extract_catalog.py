#!/usr/bin/env python3
"""Extract small, reviewable public reference hints from the Meesho homepage.

This command is an optional audit aid.  Its output is never imported by the
application or seed runtime; the frozen synthetic JSON remains the source of
truth until a developer reviews and regenerates it.
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


DEFAULT_URL = "https://www.meesho.com/"
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "data" / "source-extracted.json"


def clean(value: str) -> str:
    return re.sub(r"\s+", " ", unescape(value or "")).strip()


def parse_reference_html(html: str, source_url: str = DEFAULT_URL) -> dict:
    """Extract visible category/product-link hints without copying page copy."""
    categories: list[str] = []
    seen_categories: set[str] = set()
    for match in re.finditer(r"<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", html, re.I | re.S):
        href = unescape(match.group(1))
        label = clean(re.sub(r"<[^>]+>", " ", match.group(2)))
        if not label or len(label) > 80 or href.startswith("#"):
            continue
        if any(token in href.lower() for token in ("category", "saree", "kurti", "fashion", "footwear", "beauty", "electronics", "home", "bag")):
            if label.lower() not in seen_categories:
                categories.append(label)
                seen_categories.add(label.lower())

    product_links = []
    seen_products: set[str] = set()
    for match in re.finditer(r"href=[\"']([^\"']*(?:/p/|/product)[^\"']*)[\"'][^>]*>(.*?)</a>", html, re.I | re.S):
        href = unescape(match.group(1)).split("?")[0]
        label = clean(re.sub(r"<[^>]+>", " ", match.group(2)))
        if label and label.lower() not in seen_products:
            product_links.append({"label": label, "sourceProductUrl": href})
            seen_products.add(label.lower())

    return {
        "sourceName": "Meesho public marketplace reference",
        "sourceUrl": source_url,
        "sourceAccessedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "visibleCategoryHints": categories,
        "visibleProductLinkHints": product_links,
        "notes": [
            "Reference metadata only; this artifact is not a production catalog.",
            "The frozen dataset uses original names, copy, prices, SKUs, and locally generated images.",
        ],
    }


# Kept as a small compatibility alias for existing local fixtures.
parse_collection_html = parse_reference_html


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
        payload = parse_reference_html(html, args.url)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Extracted {len(payload['visibleCategoryHints'])} category hints and "
              f"{len(payload['visibleProductLinkHints'])} product-link hints to {args.output}")
        return 0
    except Exception as exc:
        print(f"source extraction failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
