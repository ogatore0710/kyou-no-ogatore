#!/usr/bin/env python3
"""Update the video catalog and run QA in the expected order.

Default flow:
  1. scripts/check_public.py
  2. scripts/build_catalog.py
  3. npm test

Use --skip-public-check for offline/local verification.
"""
import argparse
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run_step(label, cmd):
    print(f"\n== {label} ==")
    start = time.time()
    result = subprocess.run(cmd, cwd=str(ROOT))
    elapsed = time.time() - start
    if result.returncode != 0:
        print(f"\nNG: {label} failed after {elapsed:.1f}s", file=sys.stderr)
        return result.returncode
    print(f"OK: {label} ({elapsed:.1f}s)")
    return 0


def main():
    parser = argparse.ArgumentParser(description="Update kyou-no-ogatore catalog and run QA.")
    parser.add_argument(
        "--skip-public-check",
        action="store_true",
        help="Skip YouTube oEmbed public/private check. Useful when offline or doing local QA.",
    )
    parser.add_argument(
        "--skip-qa",
        action="store_true",
        help="Skip npm test after catalog generation.",
    )
    args = parser.parse_args()

    steps = []
    if not args.skip_public_check:
        steps.append(("public video check", [sys.executable, "scripts/check_public.py"]))
    else:
        print("skip: public video check")
    steps.append(("catalog build", [sys.executable, "scripts/build_catalog.py"]))
    if not args.skip_qa:
        steps.append(("QA", ["npm", "test"]))
    else:
        print("skip: QA")

    for label, cmd in steps:
        code = run_step(label, cmd)
        if code:
            return code

    print("\nCatalog pipeline completed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
