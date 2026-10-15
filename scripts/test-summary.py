#!/usr/bin/env python3
"""Resumen en Markdown de los informes de tests (JUnit XML) y de lint para $GITHUB_STEP_SUMMARY.

Uso: python3 scripts/test-summary.py [directorio-resultados-tests] [lint-results.xml]
Por defecto, los de app/build. Siempre sale con código 0: informa, no decide.
"""
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path


def parse(path):
    try:
        return ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return None


def tests_section(results_dir):
    files = sorted(Path(results_dir).glob("TEST-*.xml"))
    if not files:
        return ["### Tests", "", "No hay resultados de tests."]
    total = failed = skipped = 0
    broken = []
    for f in files:
        root = parse(f)
        if root is None:
            continue
        for case in root.iter("testcase"):
            total += 1
            name = f"{case.get('classname', '?').rsplit('.', 1)[-1]}.{case.get('name', '?')}"
            if case.find("skipped") is not None:
                skipped += 1
            elif case.find("failure") is not None or case.find("error") is not None:
                failed += 1
                node = case.find("failure") if case.find("failure") is not None else case.find("error")
                broken.append((name, (node.get("message") or "").strip().splitlines()[:1]))
    lines = ["### Tests", "", f"- Total: **{total}**, fallos: **{failed}**, omitidos: **{skipped}**"]
    if broken:
        lines += ["", "Tests que fallan:", ""]
        for name, msg in broken:
            lines.append(f"- `{name}`" + (f": {msg[0][:200]}" if msg else ""))
    return lines


def lint_section(lint_xml):
    root = parse(lint_xml) if Path(lint_xml).exists() else None
    if root is None:
        return ["### Lint", "", "No hay informe de lint."]
    issues = [(i.get("severity", "?"), i.get("id", "?")) for i in root.iter("issue")]
    if not issues:
        return ["### Lint", "", "Sin avisos."]
    by_severity = Counter(sev for sev, _ in issues)
    by_rule = Counter(f"{sev} {rule}" for sev, rule in issues)
    lines = ["### Lint", "", ", ".join(f"{sev}: **{n}**" for sev, n in sorted(by_severity.items())), ""]
    lines += [f"- `{rule}` x{n}" for rule, n in sorted(by_rule.items())]
    return lines


def main(argv):
    results_dir = argv[1] if len(argv) > 1 else "app/build/test-results/testDebugUnitTest"
    lint_xml = argv[2] if len(argv) > 2 else "app/build/reports/lint-results-debug.xml"
    print("\n".join(tests_section(results_dir) + [""] + lint_section(lint_xml)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
