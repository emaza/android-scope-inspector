#!/usr/bin/env bash
# Reglas de commits convencionales, compartidas por compute-version.sh (versión), el hook
# .githooks/commit-msg y scripts/check-commit-format.sh (aviso en la CI). Se carga con `source`.
#
#   feat -> minor    fix, perf -> patch    tipo! o "BREAKING CHANGE:" -> major
#   El resto de tipos (docs, chore, ci, test, refactor, style, build, revert) no sube la versión.

CC_TYPES='feat|fix|perf|refactor|docs|style|test|build|ci|chore|revert'
# tipo(ámbito)!: descripción  -> BASH_REMATCH[1]=tipo, [3]="!" si es ruptura
CC_SUBJECT_RE="^(${CC_TYPES})(\([^)]+\))?(!)?:[[:space:]]+[^[:space:]]"
# Asuntos que git genera solo y que no siguen el formato ni cuentan para la versión.
CC_GENERATED_RE='^(Merge |Revert "|fixup! |squash! |amend! )'

# cc_bump_level <tipo> <ruptura: "!" o vacío> <cuerpo del commit> -> major | minor | patch | none
cc_bump_level() {
  local type="$1" breaking="$2" body="$3" level=none
  case "$type" in
    feat) level=minor ;;
    fix | perf) level=patch ;;
  esac
  if [[ -n "$breaking" ]] || printf '%s' "$body" | grep -qE '^BREAKING[ -]CHANGE:'; then
    level=major
  fi
  echo "$level"
}

# cc_rank <nivel> -> número para comparar niveles
cc_rank() {
  case "$1" in
    major) echo 3 ;;
    minor) echo 2 ;;
    patch) echo 1 ;;
    *) echo 0 ;;
  esac
}

# cc_subject_ok <asunto> -> código 0 si sigue el formato o lo ha generado git
cc_subject_ok() {
  [[ "$1" =~ $CC_SUBJECT_RE || "$1" =~ $CC_GENERATED_RE ]]
}

CC_FORMAT_HELP="Formato: tipo(ámbito)!: descripción
  tipos: ${CC_TYPES//|/, }
  ejemplos: feat(camara): vista previa con CameraX
            fix: corrige el redondeo de la distancia
  \`!\` tras el tipo, o un pie \"BREAKING CHANGE:\", marca un cambio incompatible (sube la versión mayor)."
