#!/usr/bin/env bash
# Lista los commits de un rango que no siguen el formato convencional (los merge, revert y fixup
# generados por git se ignoran). Sale con 1 si hay alguno; la CI lo usa solo como aviso.
# Uso: bash scripts/check-commit-format.sh <rango>      p. ej. origin/dev..HEAD
set -euo pipefail

# shellcheck source=scripts/conventional-commits.sh
source "$(dirname "${BASH_SOURCE[0]}")/conventional-commits.sh"

range="${1:?uso: check-commit-format.sh <rango>}"
# Antes del bucle, para que un rango inválido haga fallar el script (en un `< <(...)` el error se perdería).
log="$(git log --no-merges --format='%h%x09%s' "$range")"
bad=0
while IFS=$'\t' read -r sha subject; do
  [[ -n "$sha" ]] || continue # rango sin commits
  if ! cc_subject_ok "$subject"; then
    echo "- \`${sha}\` ${subject}"
    bad=$((bad + 1))
  fi
done <<<"$log"

if [[ "$bad" -gt 0 ]]; then
  echo
  echo "$bad commit(s) no siguen el formato convencional y no se tendrán en cuenta para la versión."
  echo
  echo '```'
  echo "$CC_FORMAT_HELP"
  echo '```'
  exit 1
fi
