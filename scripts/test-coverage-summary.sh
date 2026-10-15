#!/usr/bin/env bash
# Pruebas de scripts/coverage-summary.py con un informe de Jacoco sintético.
# Uso: bash scripts/test-coverage-summary.sh
set -euo pipefail

SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/coverage-summary.py"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
failures=0

# Una clase propia (8 de 10 líneas cubiertas) y tres que deben quedar fuera del cálculo.
cat > "$tmp/report.xml" <<'XML'
<report name="x">
  <package name="com/example/scopeinspector/feature">
    <class name="com/example/scopeinspector/feature/Logic">
      <counter type="LINE" missed="2" covered="8"/>
      <counter type="BRANCH" missed="1" covered="3"/>
    </class>
    <class name="com/example/scopeinspector/feature/RouteMapKt">
      <counter type="LINE" missed="90" covered="0"/>
    </class>
    <class name="com/example/scopeinspector/feature/ComposableSingletons$ScreenKt">
      <counter type="LINE" missed="50" covered="0"/>
    </class>
  </package>
  <package name="com/example/scopeinspector">
    <class name="com/example/scopeinspector/BuildConfig">
      <counter type="LINE" missed="5" covered="0"/>
    </class>
  </package>
</report>
XML
echo 80 > "$tmp/min-ok.txt"
echo 90 > "$tmp/min-high.txt"

# check <descripción> <código esperado> <texto esperado en la salida> <args...>
check() {
  local description="$1" want="$2" needle="$3" out code=0
  shift 3
  out="$(python3 -I "$SCRIPT" "$@")" || code=$?
  if [[ "$code" == "$want" && "$out" == *"$needle"* ]]; then
    echo "ok   - $description"
  else
    echo "FAIL - $description: código $code (esperado $want), buscando '$needle' en:"
    echo "$out"
    failures=$((failures + 1))
  fi
}

check "calcula las líneas sin las clases excluidas" 0 "**80.0 %** (8/10)" "$tmp/report.xml"
check "calcula las ramas" 0 "**75 %**" "$tmp/report.xml"
check "no cuenta como paquete uno que solo tiene clases excluidas" 0 "| **Total**" "$tmp/report.xml"
check "con el mínimo cumplido sale con 0" 0 "Mínimo exigido: 80 %" "$tmp/report.xml" --min-file "$tmp/min-ok.txt"
check "por debajo del mínimo sale con 1" 1 "por debajo del mínimo 90" "$tmp/report.xml" --min-file "$tmp/min-high.txt"
check "sin informe lo dice y no falla" 0 "No hay informe de cobertura" "$tmp/no-existe.xml"

if [[ "$failures" -gt 0 ]]; then
  echo "$failures comprobaciones fallidas"
  exit 1
fi
echo "Todas las comprobaciones pasan"
