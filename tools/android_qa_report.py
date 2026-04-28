#!/usr/bin/env python3
"""Generate Android QA JSON and red/green Markdown reports."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def status_label(ok: bool) -> str:
    return "GREEN" if ok else "RED"


def load_results(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8-sig") as handle:
        return json.load(handle)


def write_json(results: dict[str, Any], output: Path) -> None:
    output.write_text(
        json.dumps(results, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def write_report(results: dict[str, Any], output: Path) -> None:
    steps = results.get("steps", [])
    failures = [step for step in steps if not step.get("ok")]
    overall_ok = bool(results.get("ok")) and not failures

    lines: list[str] = [
        f"# Android Full Flow QA: {status_label(overall_ok)}",
        "",
        f"- Timestamp: {results.get('timestamp', '')}",
        f"- APK: `{results.get('apkPath', '')}`",
        f"- Package: `{results.get('packageName', '')}`",
        f"- Device: `{results.get('deviceSerial', '')}`",
        f"- Output: `{results.get('outputDir', '')}`",
        "",
        "## Red / Green Summary",
        "",
        "| Step | Status | Detail |",
        "| --- | --- | --- |",
    ]

    for step in steps:
        name = step.get("name", "")
        ok = bool(step.get("ok"))
        detail = str(step.get("detail", "")).replace("|", "\\|").replace("\n", "<br>")
        lines.append(f"| {name} | {status_label(ok)} | {detail} |")

    lines.extend(["", "## Artifacts", ""])
    artifacts = results.get("artifacts", {})
    if artifacts:
        for name, path in artifacts.items():
            lines.append(f"- {name}: `{path}`")
    else:
        lines.append("- None")

    if failures:
        lines.extend(["", "## Failures", ""])
        for step in failures:
            lines.append(f"- {step.get('name', '')}: {step.get('detail', '')}")

    lines.append("")
    output.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--json-output", required=True, type=Path)
    parser.add_argument("--markdown-output", required=True, type=Path)
    args = parser.parse_args()

    results = load_results(args.input)
    write_json(results, args.json_output)
    write_report(results, args.markdown_output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
