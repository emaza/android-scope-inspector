#!/usr/bin/env bash
# Entorno de desarrollo local: JDK 17 y Android SDK instalados en el HOME del usuario (sin sudo).
#
# Uso (desde la raíz del repo):   source scripts/dev-env.sh
# Instalación y detalles:         docs/011-entorno_de_desarrollo_local.md
#
# Rutas por defecto, sobrescribibles con variables de entorno antes de hacer `source`:
#   DEV_TOOLS     herramientas descargadas (JDK, Gradle)   ~/.local/dev-tools
#   ANDROID_HOME  Android SDK                              ~/Android/Sdk
# No modifica ningún archivo de perfil: solo exporta variables en la shell actual.

export DEV_TOOLS="${DEV_TOOLS:-$HOME/.local/dev-tools}"
export JAVA_HOME="$DEV_TOOLS/jdk-17"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

for required in "$JAVA_HOME/bin/java" "$ANDROID_HOME/platforms" "$ANDROID_HOME/build-tools"; do
  if [ ! -e "$required" ]; then
    echo "dev-env: falta $required; consulta docs/011-entorno_de_desarrollo_local.md" >&2
  fi
done

export PATH="$JAVA_HOME/bin:$HOME/.local/bin:$ANDROID_HOME/platform-tools:$PATH"
