#!/usr/bin/env bash
# Calcula la versión semántica a partir de los commits convencionales desde el último tag.
#
# Reglas (ver docs/decisiones.md, versionado):
#   feat  -> minor        fix, perf -> patch        tipo! o "BREAKING CHANGE:" -> major
#   El resto de tipos (docs, chore, ci, test, refactor...) no sube la versión.
#
#   rama de release (main)  con cambios que suben versión -> X.Y.Z   (se publica tag y release)
#   rama de release (main)  sin cambios que suban versión -> X.Y.Z+<build>
#   cualquier otra rama                                   -> X.Y.Z-<rama>.<build>  (sin tag)
#
# Entorno (todo opcional):
#   BRANCH          rama (por defecto, la actual)
#   BUILD_NUMBER    nº de build; también GITHUB_RUN_NUMBER (por defecto 1). Es el versionCode.
#   RELEASE_BRANCH  rama que produce versiones estables (por defecto main)
#
# Salida: líneas clave=valor por stdout y, si existe GITHUB_OUTPUT, también ahí:
#   versionName, versionCode, isRelease, tag, bump, base
set -euo pipefail

branch="${BRANCH:-$(git rev-parse --abbrev-ref HEAD)}"
build="${BUILD_NUMBER:-${GITHUB_RUN_NUMBER:-1}}"
release_branch="${RELEASE_BRANCH:-main}"

if ! [[ "$build" =~ ^[0-9]+$ ]]; then
  echo "BUILD_NUMBER debe ser un entero, recibido: '$build'" >&2
  exit 1
fi

# Base: el tag estable vX.Y.Z más alto del repositorio (se ignoran los rc y similares); sin tags,
# 0.1.0 y se mira todo el historial. No se exige que el tag esté en el historial de la rama: al
# promocionar dev a main con un merge commit, el tag queda en ese commit y dev no lo contiene, pero
# los commits ya promocionados sí son ancestros del tag y `tag..HEAD` solo deja los posteriores.
base_tag=$(git tag --list 'v[0-9]*' --sort=-version:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n 1 || true)
if [[ -n "$base_tag" ]]; then
  base="${base_tag#v}"
  range="${base_tag}..HEAD"
else
  base="0.1.0"
  range="HEAD"
fi

if ! [[ "$base" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "El tag '$base_tag' no tiene formato vX.Y.Z" >&2
  exit 1
fi
IFS=. read -r major minor patch <<<"$base"

rank() {
  case "$1" in
    major) echo 3 ;;
    minor) echo 2 ;;
    patch) echo 1 ;;
    *) echo 0 ;;
  esac
}

commit_re='^(feat|fix|perf|refactor|docs|style|test|build|ci|chore|revert)(\([^)]+\))?(!)?:[[:space:]]'
bump=none
while IFS= read -r -d $'\x1e' record; do
  subject="${record%%$'\x1f'*}"
  body="${record#*$'\x1f'}"
  subject="${subject#"${subject%%[![:space:]]*}"}" # git separa los registros con un salto de línea
  [[ $subject =~ $commit_re ]] || continue
  type="${BASH_REMATCH[1]}"
  level=none
  case "$type" in
    feat) level=minor ;;
    fix | perf) level=patch ;;
  esac
  if [[ -n "${BASH_REMATCH[3]}" ]] || printf '%s' "$body" | grep -qE '^BREAKING[ -]CHANGE:'; then
    level=major
  fi
  if (($(rank "$level") > $(rank "$bump"))); then
    bump="$level"
  fi
done < <(git log --format='%s%x1f%b%x1e' "$range")

case "$bump" in
  major) next="$((major + 1)).0.0" ;;
  minor) next="${major}.$((minor + 1)).0" ;;
  patch) next="${major}.${minor}.$((patch + 1))" ;;
  *) next="$base" ;;
esac

slug=$(printf '%s' "$branch" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^0-9a-z]+/-/g; s/^-+//; s/-+$//')
[[ -n "$slug" ]] || slug="dev"

is_release=false
tag=""
if [[ "$branch" == "$release_branch" ]]; then
  if [[ "$bump" != none ]]; then
    version_name="$next"
    is_release=true
    tag="v$next"
  else
    version_name="${base}+${build}"
  fi
else
  # Un pre-release debe ordenarse por encima de la base: sin cambios que suban versión, patch.
  [[ "$bump" != none ]] || next="${major}.${minor}.$((patch + 1))"
  version_name="${next}-${slug}.${build}"
fi

emit() {
  echo "$1=$2"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "$1=$2" >>"$GITHUB_OUTPUT"
  fi
}

emit versionName "$version_name"
emit versionCode "$build"
emit isRelease "$is_release"
emit tag "$tag"
emit bump "$bump"
emit base "$base"
