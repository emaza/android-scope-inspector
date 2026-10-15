#!/usr/bin/env bash
# Pruebas de scripts/test-summary.py con informes JUnit y de lint sintéticos.
# Uso: bash scripts/test-test-summary.sh
set -euo pipefail

SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/test-summary.py"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
failures=0

# expect <descripción> <texto que debe aparecer en la salida> <args...>
expect() {
  local description="$1" needle="$2" out
  shift 2
  out="$(python3 -I "$SCRIPT" "$@")"
  if [[ "$out" == *"$needle"* ]]; then
    echo "ok   - $description"
  else
    echo "FAIL - $description: no aparece '$needle' en:"
    echo "$out"
    failures=$((failures + 1))
  fi
}

mkdir -p "$tmp/res"
cat > "$tmp/res/TEST-a.xml" <<'XML'
<testsuite name="a.FooTest" tests="3">
  <testcase name="ok" classname="a.FooTest"/>
  <testcase name="skipped" classname="a.FooTest"><skipped/></testcase>
  <testcase name="boom" classname="a.FooTest"><failure message="expected 1 but was 2">trace</failure></testcase>
</testsuite>
XML
cat > "$tmp/lint.xml" <<'XML'
<issues>
  <issue id="InlinedApi" severity="Warning"/>
  <issue id="InlinedApi" severity="Warning"/>
  <issue id="MissingPermission" severity="Error"/>
</issues>
XML

expect "cuenta tests, fallos y omitidos" "Total: **3**, fallos: **1**, omitidos: **1**" "$tmp/res" "$tmp/lint.xml"
expect "nombra el test que falla con su mensaje" "FooTest.boom\`: expected 1 but was 2" "$tmp/res" "$tmp/lint.xml"
expect "agrupa el lint por regla" "InlinedApi\` x2" "$tmp/res" "$tmp/lint.xml"
expect "agrupa el lint por severidad" "Error: **1**" "$tmp/res" "$tmp/lint.xml"
expect "sin resultados de tests lo dice" "No hay resultados de tests" "$tmp/vacio" "$tmp/lint.xml"
expect "sin informe de lint lo dice" "No hay informe de lint" "$tmp/res" "$tmp/no-existe.xml"

if [[ "$failures" -gt 0 ]]; then
  echo "$failures comprobaciones fallidas"
  exit 1
fi
echo "Todas las comprobaciones pasan"
