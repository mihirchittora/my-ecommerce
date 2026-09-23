#!/usr/bin/env python3
"""Portable validation entry point for the development seed dataset."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from validate_dataset import main  # noqa: E402


if __name__ == "__main__":
    raise SystemExit(main())
