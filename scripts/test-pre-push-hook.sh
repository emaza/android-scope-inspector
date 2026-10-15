#!/usr/bin/env bash
# Pruebas de .githooks/pre-push alimentándolo por stdin como lo hace git.
# Uso: bash scripts/test-pre-push-hook.sh
set -euo pipefail

HOOK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.githooks/pre-push"
sha=0123456789abcdef0123456789abcdef01234567
zero=0000000000000000000000000000000000000000
failures=0
checks=0

# expect <descripción> <código esperado> <líneas de stdin...>
expect() {
  local description="$1" want="$2" code=0
  shift 2
  printf '%s\n' "$@" | bash "$HOOK" origin git@github.com:o/r.git >/dev/null 2>&1 || code=$?
  checks=$((checks + 1))
  if [[ "$code" == "$want" ]]; then
    echo "ok   - $description"
  else
    echo "FAIL - $description: código $code, esperado $want"
    failures=$((failures + 1))
  fi
}

expect "push a main: rechazado" 1 "refs/heads/main $sha refs/heads/main $zero"
expect "push a dev: rechazado" 1 "refs/heads/dev $sha refs/heads/dev $sha"
expect "una rama local cualquiera empujada a main: rechazado" 1 "refs/heads/fase-3 $sha refs/heads/main $sha"
expect "borrar main en el remoto: rechazado" 1 "(delete) $zero refs/heads/main $sha"
expect "push de una rama fase-*: permitido" 0 "refs/heads/fase-3 $sha refs/heads/fase-3 $zero"
expect "push de una rama que solo se parece (main-feature): permitido" 0 "refs/heads/x $sha refs/heads/main-feature $zero"
expect "push de un tag: permitido" 0 "refs/tags/v1.0.0 $sha refs/tags/v1.0.0 $zero"
expect "varias ramas y una es main: rechazado" 1 \
  "refs/heads/fase-3 $sha refs/heads/fase-3 $zero" "refs/heads/main $sha refs/heads/main $sha"
expect "sin referencias: permitido" 0

echo
echo "$((checks - failures))/$checks comprobaciones correctas"
[[ $failures -eq 0 ]]
