#!/usr/bin/env python3
"""Protected development reset entry point.

Reset remains implemented by seed.py so the command-line safety checks and
named Compose project allow-list have one source of truth.
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from seed import load_dotenv, main  # noqa: E402


if __name__ == "__main__":
    load_dotenv(ROOT.parent / ".env")
    raise SystemExit(main(["--reset", *sys.argv[1:]]))
