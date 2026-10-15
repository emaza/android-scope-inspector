#!/usr/bin/env python3
"""Resumen en Markdown de la cobertura (informe XML de Jacoco) para $GITHUB_STEP_SUMMARY.

Uso: python3 scripts/coverage-summary.py [report.xml] [--min-file app/coverage-min.txt]

Excluye del cálculo las clases generadas o que los tests no pueden ejercitar (ver EXCLUDED).
Con --min-file, sale con código 1 si la cobertura de líneas es menor que el número del archivo
(trinquete: ese mínimo solo se sube, nunca se baja).
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_REPORT = "app/build/reports/coverage/test/debug/report.xml"

# Clases fuera del cálculo y por qué.
EXCLUDED = [
    (r"/BuildConfig$", "BuildConfig", "generada"),
    (r"/R(\$.*)?$", "R", "generada"),
    (r"ComposableSingletons\$", "ComposableSingletons*", "generada por el compilador de Compose"),
    (r"/RouteMapKt($|\$)", "RouteMap", "mapa de osmdroid: no se prueba, se sustituye en los tests"),
]


def excluded(class_name):
    return any(re.search(pattern, class_name) for pattern, _, _ in EXCLUDED)


def counter(node, kind):
    for c in node.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("covered")), int(c.get("missed"))
    return 0, 0


def percent(covered, missed):
    total = covered + missed
    return 100.0 * covered / total if total else 100.0


def summarize(report):
    """Devuelve ({paquete: [líneas_cub, líneas_perd, ramas_cub, ramas_perd]}, total)."""
    packages = {}
    for pkg in report.findall("package"):
        acc = [0, 0, 0, 0]
        for cls in pkg.findall("class"):
            if excluded(cls.get("name", "")):
                continue
            lc, lm = counter(cls, "LINE")
            bc, bm = counter(cls, "BRANCH")
            acc = [acc[0] + lc, acc[1] + lm, acc[2] + bc, acc[3] + bm]
        if acc[0] + acc[1] > 0:
            packages[pkg.get("name", "?").replace("/", ".")] = acc
    total = [sum(v[i] for v in packages.values()) for i in range(4)]
    return packages, total


def main(argv):
    args = argv[1:]
    min_file = None
    if "--min-file" in args:
        i = args.index("--min-file")
        min_file = args[i + 1]
        del args[i : i + 2]
    path = args[0] if args else DEFAULT_REPORT
    if not Path(path).exists():
        print("### Cobertura\n\nNo hay informe de cobertura.")
        return 0
    packages, total = summarize(ET.parse(path).getroot())
    lines_pct = percent(total[0], total[1])

    out = ["### Cobertura (tests locales)", "", "| Paquete | Líneas | Ramas |", "|---|---|---|"]
    for name, v in sorted(packages.items()):
        short = name.removeprefix("com.example.scopeinspector") or "(raíz)"
        branches = f"{percent(v[2], v[3]):.0f} %" if v[2] + v[3] else "-"
        out.append(f"| `{short.lstrip('.') or '(raíz)'}` | {percent(v[0], v[1]):.0f} % ({v[0]}/{v[0] + v[1]}) | {branches} |")
    branches_total = f"{percent(total[2], total[3]):.0f} %" if total[2] + total[3] else "-"
    out.append(f"| **Total** | **{lines_pct:.1f} %** ({total[0]}/{total[0] + total[1]}) | **{branches_total}** |")
    out += ["", "Excluido del cálculo: " + "; ".join(f"{name} ({why})" for _, name, why in EXCLUDED) + "."]

    code = 0
    if min_file:
        minimum = float(Path(min_file).read_text().strip())
        if lines_pct < minimum:
            out += ["", f"**Cobertura de líneas {lines_pct:.1f} % por debajo del mínimo {minimum:g} %** ({min_file})."]
            code = 1
        else:
            out += ["", f"Mínimo exigido: {minimum:g} % de líneas (`{min_file}`, solo se sube)."]
            if lines_pct - minimum > 5:
                out.append(f"La cobertura supera el mínimo en más de 5 puntos: se puede subir a {int(lines_pct) - 2}.")
    print("\n".join(out))
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv))
