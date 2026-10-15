#!/usr/bin/env bash
# Pruebas de scripts/compute-version.sh sobre repositorios git temporales.
# Uso: bash scripts/test-compute-version.sh
set -euo pipefail

SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/compute-version.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

failures=0
checks=0

new_repo() {
  rm -rf "$WORK/repo"
  mkdir "$WORK/repo"
  cd "$WORK/repo"
  git init -q -b main
  git config user.name test
  git config user.email test@example.com
  git config commit.gpgsign false
  git commit -q --allow-empty -m "Initial commit"
}

commit() { git commit -q --allow-empty -m "$@"; }

# version <rama> <build> -> imprime la salida del script
version() {
  env -u GITHUB_OUTPUT -u GITHUB_RUN_NUMBER BRANCH="$1" BUILD_NUMBER="$2" bash "$SCRIPT"
}

# expect <descripción> <salida> <clave> <esperado>
expect() {
  local description="$1" output="$2" key="$3" expected="$4" actual
  actual=$(printf '%s\n' "$output" | sed -n "s/^${key}=//p")
  checks=$((checks + 1))
  if [[ "$actual" == "$expected" ]]; then
    echo "ok   - $description"
  else
    echo "FAIL - $description: $key esperado '$expected', obtenido '$actual'"
    failures=$((failures + 1))
  fi
}

# --- sin tags -------------------------------------------------------------------------------
new_repo
commit "Mensaje sin formato convencional"
out=$(version main 7)
expect "sin tags y sin commits convencionales: base 0.1.0 con build" "$out" versionName "0.1.0+7"
expect "sin tags y sin commits convencionales: no es release" "$out" isRelease "false"

new_repo
commit "feat: primera funcionalidad"
out=$(version main 3)
expect "sin tags, feat en main: 0.2.0" "$out" versionName "0.2.0"
expect "sin tags, feat en main: es release" "$out" isRelease "true"
expect "sin tags, feat en main: tag v0.2.0" "$out" tag "v0.2.0"

# --- con tag base ---------------------------------------------------------------------------
new_repo
git tag v1.2.3
commit "fix: corregir un fallo"
out=$(version main 10)
expect "fix en main: patch" "$out" versionName "1.2.4"
expect "fix en main: bump patch" "$out" bump "patch"

new_repo
git tag v1.2.3
commit "fix: corregir un fallo"
commit "feat(ubicacion): detectar actividad"
out=$(version main 11)
expect "fix + feat: gana minor y reinicia patch" "$out" versionName "1.3.0"

new_repo
git tag v1.2.3
commit "feat(api)!: cambiar el contrato"
out=$(version main 12)
expect "tipo! con scope: major" "$out" versionName "2.0.0"

new_repo
git tag v1.2.3
git commit -q --allow-empty -m "refactor: reorganizar" -m "BREAKING CHANGE: se elimina la API antigua"
out=$(version main 13)
expect "BREAKING CHANGE en el cuerpo: major" "$out" versionName "2.0.0"

new_repo
git tag v1.2.3
commit "perf: acelerar el mapa"
out=$(version main 14)
expect "perf: patch" "$out" versionName "1.2.4"

new_repo
git tag v1.2.3
commit "docs: actualizar documentación"
commit "chore: tareas varias"
commit "ci: ajustar el workflow"
commit "test: más pruebas"
commit "refactor: limpiar código"
out=$(version main 15)
expect "solo docs/chore/ci/test/refactor en main: versión base con build" "$out" versionName "1.2.3+15"
expect "solo docs/chore/ci/test/refactor en main: no es release" "$out" isRelease "false"
expect "solo docs/chore/ci/test/refactor en main: sin tag" "$out" tag ""

new_repo
git tag v1.2.3
out=$(version main 16)
expect "HEAD justo en el tag: versión base con build" "$out" versionName "1.2.3+16"

new_repo
git tag v1.2.3
commit "feature: no es un tipo convencional"
commit "Fix: con mayúscula no cuenta"
out=$(version main 17)
expect "tipos no convencionales no suben versión" "$out" bump "none"

# --- otras ramas ----------------------------------------------------------------------------
new_repo
git tag v1.2.3
commit "feat: algo nuevo"
out=$(version fase-2-ubicacion 58)
expect "rama de trabajo con feat: pre-release minor" "$out" versionName "1.3.0-fase-2-ubicacion.58"
expect "rama de trabajo: no es release" "$out" isRelease "false"
expect "rama de trabajo: sin tag" "$out" tag ""
expect "versionCode es el número de build" "$out" versionCode "58"

new_repo
git tag v1.2.3
commit "docs: solo documentación"
out=$(version dev 4)
expect "rama de trabajo sin bump: patch siguiente, por encima de la base" "$out" versionName "1.2.4-dev.4"

new_repo
git tag v1.2.3
commit "fix: algo"
out=$(version "Feature/Foo_Bar" 5)
expect "el nombre de la rama se normaliza" "$out" versionName "1.2.4-feature-foo-bar.5"

new_repo
git tag v1.2.3
commit "fix: algo"
out=$(version "pr-12" 6)
expect "pull request" "$out" versionName "1.2.4-pr-12.6"

# --- el tag más cercano manda -------------------------------------------------------------------
new_repo
git tag v1.0.0
commit "feat: antiguo"
git tag v1.1.0
commit "fix: reciente"
out=$(version main 20)
expect "se usa el tag más reciente como base" "$out" versionName "1.1.1"
expect "se usa el tag más reciente como base: base" "$out" base "1.1.0"

# --- GITHUB_OUTPUT --------------------------------------------------------------------------
new_repo
git tag v1.2.3
commit "feat: algo"
gh_out="$WORK/github_output"
: >"$gh_out"
GITHUB_OUTPUT="$gh_out" BRANCH=main BUILD_NUMBER=9 bash "$SCRIPT" >/dev/null
expect "escribe en GITHUB_OUTPUT" "$(cat "$gh_out")" versionName "1.3.0"

# --- entrada inválida -----------------------------------------------------------------------
new_repo
checks=$((checks + 1))
if BRANCH=main BUILD_NUMBER=abc bash "$SCRIPT" >/dev/null 2>&1; then
  echo "FAIL - un BUILD_NUMBER no numérico debe fallar"
  failures=$((failures + 1))
else
  echo "ok   - un BUILD_NUMBER no numérico falla"
fi

echo
echo "$((checks - failures))/$checks comprobaciones correctas"
[[ $failures -eq 0 ]]
