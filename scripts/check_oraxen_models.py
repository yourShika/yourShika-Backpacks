#!/usr/bin/env python3
"""Validate the bundled Oraxen model JSONs.

Every cube element must define all six faces (north, south, east, west, up, down).
Missing faces lead to one-sided / invisible surfaces in-game. This guard keeps such
regressions out of the repo (#58). Exits non-zero when problems are found.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / "src" / "main" / "resources" / "oraxen" / "pack" / "models"
REQUIRED_FACES = {"north", "south", "east", "west", "up", "down"}


def main() -> int:
    if not MODELS.exists():
        print(f"No models directory found at {MODELS} - nothing to check.")
        return 0

    problems: list[str] = []
    files = sorted(MODELS.rglob("*.json"))
    for path in files:
        rel = path.relative_to(ROOT)
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - report any parse error clearly
            problems.append(f"{rel}: invalid JSON ({exc})")
            continue

        elements = data.get("elements", [])
        if not isinstance(elements, list):
            problems.append(f"{rel}: 'elements' is not a list")
            continue

        for index, element in enumerate(elements):
            faces = element.get("faces", {}) if isinstance(element, dict) else {}
            present = set(faces.keys())
            missing = REQUIRED_FACES - present
            if missing:
                problems.append(
                    f"{rel}: element #{index} is missing faces: "
                    f"{', '.join(sorted(missing))}"
                )

    print(f"Checked {len(files)} Oraxen model JSON file(s).")
    if problems:
        print(f"\n{len(problems)} problem(s) found (one-sided / invalid faces):")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print("OK - every element defines all six faces (no one-sided faces).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
