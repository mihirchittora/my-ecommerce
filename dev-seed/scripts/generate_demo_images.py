#!/usr/bin/env python3
"""Generate deterministic, original PNG product-card artwork for local demos.

The repository intentionally uses locally generated geometric artwork instead
of third-party hosted product photography.  It is sufficient to exercise
catalog galleries, thumbnails, mobile swipes, and broken-image handling.
"""

from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
IMAGES = ROOT / "images"
WIDTH, HEIGHT = 480, 600


def chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def png(rows: list[bytes]) -> bytes:
    raw = b"".join(b"\x00" + row for row in rows)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 6)) + chunk(b"IEND", b"")


PALETTE = [
    (35, 58, 82), (105, 64, 56), (65, 92, 77), (132, 91, 49),
    (83, 70, 111), (48, 91, 104), (133, 78, 83), (69, 89, 58),
    (115, 83, 67), (64, 74, 91),
]


def render(index: int, image_index: int) -> bytes:
    primary = PALETTE[(index + image_index - 2) % len(PALETTE)]
    secondary = PALETTE[(index + image_index + 2) % len(PALETTE)]
    rows = []
    for y in range(HEIGHT):
        row = bytearray()
        for x in range(WIDTH):
            if x < 24 or x >= WIDTH - 24 or y < 24 or y >= HEIGHT - 24:
                colour = (242, 237, 229)
            else:
                # Warm off-white backdrop plus a centered product-card shape.
                colour = (251, 248, 242)
                if 86 <= x < WIDTH - 86 and 74 <= y < HEIGHT - 72:
                    colour = primary
                if 126 <= x < WIDTH - 126 and 120 <= y < 220:
                    colour = secondary
                if 126 <= x < WIDTH - 126 and 244 <= y < 260:
                    colour = (244, 207, 125)
                if 160 <= x < WIDTH - 160 and 345 <= y < 365:
                    colour = (242, 226, 190)
                # A small variant marker changes the composition of gallery
                # images without copying any external brand or photography.
                if image_index > 1 and 150 + image_index * 18 <= x < 190 + image_index * 18 and 420 <= y < 460:
                    colour = secondary
            row.extend(colour)
        rows.append(bytes(row))
    return png(rows)


def main() -> None:
    products = json.loads((DATA / "products.json").read_text(encoding="utf-8"))["products"]
    IMAGES.mkdir(parents=True, exist_ok=True)
    for index, product in enumerate(products, start=1):
        for image_index, image in enumerate(product["images"], start=1):
            path = IMAGES / image["file"]
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(render(index, image_index))
    print(f"Generated {sum(len(product['images']) for product in products)} original local PNG assets in {IMAGES}")


if __name__ == "__main__":
    main()
