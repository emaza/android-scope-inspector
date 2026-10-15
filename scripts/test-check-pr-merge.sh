#!/usr/bin/env bash
# Pruebas de scripts/check-pr-merge.sh con un `gh` falso.
# Uso: bash scripts/test-check-pr-merge.sh
set -euo pipefail

SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-pr-merge.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

mkdir "$WORK/bin"
# gh falso: FAKE_GH_OUTPUTS es una lista "a,b,c" con el resultado de cada llamada
# (el último se repite); "err" simula un fallo de la API.
cat >"$WORK/bin/gh" <<'EOF'
#!/usr/bin/env bash
n=$(cat "$FAKE_GH_COUNTER" 2>/dev/null || echo 0)
n=$((n + 1))
echo "$n" >"$FAKE_GH_COUNTER"
IFS=, read -ra outs <<<"$FAKE_GH_OUTPUTS"
idx=$((n - 1))
((idx >= ${#outs[@]})) && idx=$((${#outs[@]} - 1))
out="${outs[$idx]}"
if [[ "$out" == err ]]; then
  echo "boom" >&2
  exit 1
fi
echo "$out"
EOF
chmod +x "$WORK/bin/gh"

failures=0
checks=0

# run_case <descripción> <salidas del gh falso> <código esperado> <texto esperado en la salida> <llamadas esperadas> [RETRIES]
run_case() {
  local description="$1" outputs="$2" want_code="$3" want_text="$4" want_calls="$5" retries="${6:-3}"
  local counter="$WORK/counter" output code=0 calls
  rm -f "$counter"
  output=$(
    PATH="$WORK/bin:$PATH" FAKE_GH_OUTPUTS="$outputs" FAKE_GH_COUNTER="$counter" \
      GITHUB_REPOSITORY=owner/repo GITHUB_SHA=0123456789abcdef GITHUB_REF_NAME=main \
      RETRIES="$retries" DELAY=0 bash "$SCRIPT" 2>&1
  ) || code=$?
  calls=$(cat "$counter" 2>/dev/null || echo 0)
  checks=$((checks + 1))
  if [[ "$code" == "$want_code" && "$output" == *"$want_text"* && "$calls" == "$want_calls" ]]; then
    echo "ok   - $description"
  else
    echo "FAIL - $description: código $code (esperado $want_code), llamadas $calls (esperadas $want_calls)"
    echo "       salida: $output"
    failures=$((failures + 1))
  fi
}

run_case "commit con una PR asociada: pasa a la primera" "1" 0 "OK: 0123456 procede de 1 pull request" 1
run_case "commit con varias PR asociadas: pasa" "2" 0 "procede de 2 pull request" 1
run_case "sin PR en ningún intento: push directo detectado" "0" 1 "Push directo a main sin pull request" 3
run_case "la PR aparece en el segundo intento: pasa tras reintentar" "0,1" 0 "OK:" 2
run_case "la API falla y luego responde: pasa" "err,1" 0 "OK:" 2
run_case "la API falla siempre: error de consulta, no de push directo" "err" 1 "No se pudo consultar la API" 3
run_case "un solo intento sin PR falla sin reintentar" "0" 1 "Push directo" 1 1

# Variables obligatorias
checks=$((checks + 1))
if env -u GITHUB_SHA PATH="$WORK/bin:$PATH" GITHUB_REPOSITORY=o/r GITHUB_REF_NAME=main bash "$SCRIPT" >/dev/null 2>&1; then
  echo "FAIL - sin GITHUB_SHA debe fallar"
  failures=$((failures + 1))
else
  echo "ok   - sin GITHUB_SHA falla"
fi

echo
echo "$((checks - failures))/$checks comprobaciones correctas"
[[ $failures -eq 0 ]]
