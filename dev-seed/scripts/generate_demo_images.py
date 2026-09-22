#!/usr/bin/env python3
"""Generate small, original PNG bag illustrations for local demo use."""

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
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def render(index: int) -> bytes:
    palette = [(38, 28, 24), (76, 48, 32), (108, 73, 42), (38, 70, 72), (128, 63, 50), (76, 91, 54), (150, 112, 56), (58, 56, 84)]
    primary = palette[(index - 1) % len(palette)]
    secondary = palette[index % len(palette)]
    rows = []
    for y in range(HEIGHT):
        row = bytearray()
        for x in range(WIDTH):
            if x < 34 or x >= WIDTH - 34 or y < 28 or y >= HEIGHT - 28:
                colour = (239, 232, 218)
            elif 90 <= x < WIDTH - 90 and 90 <= y < HEIGHT - 60:
                if 170 <= y < 255:
                    colour = secondary
                elif 255 <= y < 270:
                    colour = (235, 201, 132)
                else:
                    colour = primary
            else:
                colour = (248, 245, 237)
            row.extend(colour)
        rows.append(bytes(row))
    return png(rows)


def main() -> None:
    products = json.loads((DATA / "products.json").read_text(encoding="utf-8"))["products"]
    IMAGES.mkdir(parents=True, exist_ok=True)
    for index, product in enumerate(products, start=1):
        path = IMAGES / product["images"][0]["file"]
        path.write_bytes(render(index))
    print(f"Generated {len(products)} original local PNG assets in {IMAGES}")


if __name__ == "__main__":
    main()
