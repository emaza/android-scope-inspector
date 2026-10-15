#!/usr/bin/env bash
# Pruebas de .githooks/commit-msg y scripts/check-commit-format.sh sobre un repositorio temporal.
# Uso: bash scripts/test-commit-msg-hook.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
failures=0
checks=0

# El hook usa git rev-parse --show-toplevel: se prueba en un repo con copia de scripts y hook.
cd "$WORK"
git init -q -b main
git config user.name test
git config user.email test@example.com
git config commit.gpgsign false
mkdir scripts .githooks
cp "$ROOT/scripts/conventional-commits.sh" "$ROOT/scripts/check-commit-format.sh" scripts/
cp "$ROOT/.githooks/commit-msg" .githooks/
git config core.hooksPath .githooks

# hook <descripción> <código esperado> <mensaje (puede tener varias líneas)>
hook() {
  local description="$1" want="$2" message="$3" code=0
  printf '%s\n' "$message" >msg.txt
  bash .githooks/commit-msg msg.txt >/dev/null 2>&1 || code=$?
  checks=$((checks + 1))
  if [[ "$code" == "$want" ]]; then
    echo "ok   - $description"
  else
    echo "FAIL - $description: código $code, esperado $want"
    failures=$((failures + 1))
  fi
}

hook "feat con ámbito" 0 "feat(camara): vista previa con CameraX"
hook "fix sin ámbito" 0 "fix: corrige el redondeo"
hook "ruptura con !" 0 "feat(api)!: cambia el formato"
hook "cada tipo del versionado" 0 "revert: deshace el cambio anterior"
hook "merge generado por git" 0 "Merge pull request #9 from emaza/fase-x"
hook "revert generado por git" 0 'Revert "feat: algo"'
hook "fixup de autosquash" 0 "fixup! feat: algo"
hook "ignora comentarios y líneas vacías iniciales" 0 $'\n# comentario de git\nfeat: algo'
hook "solo cuenta la primera línea" 0 $'fix: arregla\n\ncuerpo libre sin formato'
hook "sin tipo" 1 "arregla el mapa"
hook "tipo desconocido" 1 "feature: algo"
hook "mayúsculas en el tipo" 1 "Feat: algo"
hook "sin espacio tras los dos puntos" 1 "feat:algo"
hook "sin descripción" 1 "feat: "
hook "mensaje vacío" 1 ""
hook "solo comentarios" 1 "# nada más"

# Con el hook activo, git commit lo respeta y --no-verify lo salta.
git commit -q --allow-empty -m "feat: primero" && ok1=1 || ok1=0
git commit -q --allow-empty -m "mal formado" 2>/dev/null && bad=1 || bad=0
git commit -q --allow-empty --no-verify -m "mal formado a propósito" && skip=1 || skip=0
git commit -q --allow-empty -m "fix(x)!: último"
for pair in "git commit acepta un mensaje válido:$ok1:1" "git commit rechaza uno inválido:$bad:0" "--no-verify lo salta:$skip:1"; do
  checks=$((checks + 1))
  IFS=: read -r name got want <<<"$pair"
  if [[ "$got" == "$want" ]]; then echo "ok   - $name"; else echo "FAIL - $name"; failures=$((failures + 1)); fi
done

# check-commit-format: el rango contiene el commit mal formado.
out="$(bash scripts/check-commit-format.sh HEAD~2..HEAD 2>&1)" && code=0 || code=$?
checks=$((checks + 1))
if [[ "$code" == 1 && "$out" == *"mal formado a propósito"* && "$out" != *"último"* ]]; then
  echo "ok   - check-commit-format lista solo los commits mal formados y sale con 1"
else
  echo "FAIL - check-commit-format: código $code, salida: $out"
  failures=$((failures + 1))
fi
checks=$((checks + 1))
if bash scripts/check-commit-format.sh no-existe..HEAD >/dev/null 2>&1; then
  echo "FAIL - check-commit-format debería fallar con un rango inválido"
  failures=$((failures + 1))
else
  echo "ok   - check-commit-format falla con un rango inválido en vez de pasar en silencio"
fi
checks=$((checks + 1))
if bash scripts/check-commit-format.sh HEAD~1..HEAD >/dev/null 2>&1; then
  echo "ok   - check-commit-format sale con 0 si todo está bien"
else
  echo "FAIL - check-commit-format debería salir con 0"
  failures=$((failures + 1))
fi

checks=$((checks + 1))
if bash scripts/check-commit-format.sh HEAD..HEAD >/dev/null 2>&1; then
  echo "ok   - check-commit-format sale con 0 con un rango sin commits"
else
  echo "FAIL - un rango vacío no debería dar error"
  failures=$((failures + 1))
fi

echo
if [[ "$failures" -gt 0 ]]; then
  echo "$failures de $checks comprobaciones fallidas"
  exit 1
fi
echo "$checks/$checks comprobaciones correctas"
