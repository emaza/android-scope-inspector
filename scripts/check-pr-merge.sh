#!/usr/bin/env bash
# Comprueba que el commit de un push a main o dev procede de una pull request.
#
# En un repositorio privado con cuenta gratuita GitHub no puede bloquear los pushes directos,
# así que se detectan a posteriori: si el commit no tiene ninguna PR asociada, el job falla.
# Ver docs/decisiones.md (flujo de ramas y PR).
#
# Entorno obligatorio: GITHUB_REPOSITORY, GITHUB_SHA, GITHUB_REF_NAME; y `gh` autenticado (GH_TOKEN).
# Entorno opcional:    RETRIES (por defecto 6) y DELAY en segundos (por defecto 5): la asociación
#                      commit-PR puede tardar unos segundos en aparecer justo después del merge.
set -euo pipefail

: "${GITHUB_REPOSITORY:?falta GITHUB_REPOSITORY}"
: "${GITHUB_SHA:?falta GITHUB_SHA}"
: "${GITHUB_REF_NAME:?falta GITHUB_REF_NAME}"
retries="${RETRIES:-6}"
delay="${DELAY:-5}"
short_sha="${GITHUB_SHA:0:7}"

result=""
for ((attempt = 1; attempt <= retries; attempt++)); do
  if result=$(gh api "repos/${GITHUB_REPOSITORY}/commits/${GITHUB_SHA}/pulls" --jq 'length' 2>/dev/null); then
    if [[ "$result" =~ ^[0-9]+$ ]] && ((result > 0)); then
      echo "OK: ${short_sha} procede de ${result} pull request(s)."
      exit 0
    fi
  else
    result="error"
  fi
  if ((attempt < retries)); then
    echo "Intento ${attempt}/${retries}: sin PR asociada (resultado: ${result}); reintento en ${delay}s."
    sleep "$delay"
  fi
done

if [[ "$result" == "error" ]]; then
  echo "::error::No se pudo consultar la API de GitHub para el commit ${short_sha}."
else
  echo "::error::Push directo a ${GITHUB_REF_NAME} sin pull request (commit ${short_sha}). main y dev solo aceptan cambios mediante PR."
fi
exit 1
