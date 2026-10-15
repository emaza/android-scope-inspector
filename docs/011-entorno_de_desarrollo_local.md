# Entorno de desarrollo local

Lote 1 de la [hoja de ruta (010)](010-analisis_de_tests_y_entorno.md): compilar, pasar los tests y el lint en esta máquina, sin Android Studio y sin permisos de administrador. Todo se instala bajo el `HOME` del usuario.

## Resultado

| | Antes | Ahora |
|---|---|---|
| Comprobación completa (tests + lint + APK) | 4-6 min en CI por cada cambio | **~4 s** con la caché caliente, en local |
| Primera ejecución | — | 5 min 54 s (descarga las dependencias una vez) |
| Wrapper de Gradle | No existía | `./gradlew`, con checksum de la distribución fijado |

Con el entorno, 41 tests pasan en local (0 fallos) y el lint termina sin errores.

## Qué se instaló y dónde

| Componente | Versión | Ruta | Tamaño | Verificación |
|---|---|---|---|---|
| JDK Temurin | 21.0.12.1 (antes 17; ver nota) | `~/.local/dev-tools/jdk-21` | ~200 MB | sha256 contra el de la API de Adoptium |
| Gradle (solo para generar el wrapper) | 9.8.1 | `~/.local/dev-tools/gradle-9.8.1` | ~130 MB | sha256 `dce76f55…dc01f` publicado por Gradle |
| Android CLI | 1.0.16500706 | `~/.local/bin/android` y `~/.android/` | ~5 MB | descargado por HTTPS de `dl.google.com` (no publican checksum) |
| Android SDK | platform-tools 37.0.1, `platforms/android-37.0`, `build-tools/36.0.0` | `~/Android/Sdk` | ~330 MB | gestor del SDK |

El wrapper se generó con la distribución verificada, y el `gradle-wrapper.jar` resultante coincide con el checksum que Gradle publica para esa versión (`3b8a2577…8e98`). Es el jar que no pude verificar al principio: el mismatch era del jar del repositorio de Gradle, no del proceso.

No se modificó ningún archivo de perfil (`~/.bashrc` y similares): el instalador oficial del CLI añade una línea al perfil si `~/.local/bin` no está en el `PATH`, aquí ya estaba, y de todas formas se omitió el script y se descargó solo el binario.

> **Nota sobre el JDK:** el lote 1 instaló el JDK 17. Al añadir los tests de UI (lote 2) se vio que **Robolectric exige Java 21 para Android SDK 36** (`Android SDK 36 requires Java 21 (have Java 17)`). El entorno local y la CI pasaron a JDK 21; el bytecode de la app sigue en Java 17. El `jdk-17` de `~/.local/dev-tools` ya no se usa y se puede borrar.

## Cómo usarlo

```
source scripts/dev-env.sh          # exporta JAVA_HOME, ANDROID_HOME y PATH solo en esa shell

./gradlew testDebugUnitTest        # tests unitarios
./gradlew lintDebug                # lint (informes en app/build/reports/)
./gradlew assembleDebug            # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest lintDebug assembleDebug -PappVersionName=0.2.1-local -PappVersionCode=1

./gradlew --stop                   # libera la memoria del daemon de Gradle al terminar
```

`scripts/dev-env.sh` usa por defecto `~/.local/dev-tools` y `~/Android/Sdk`; se pueden cambiar con `DEV_TOOLS` y `ANDROID_HOME` antes de hacer `source`. Sin `-PappVersionName` y `-PappVersionCode`, Gradle usa `0.1.0-dev` y `1`.

Reproducir la instalación en otra máquina (Linux x86_64):

1. Descargar el JDK 21 Temurin y la distribución de Gradle 9.8.1, comprobar sus checksums y descomprimirlos en `~/.local/dev-tools` (el JDK en `jdk-21`).
2. Descargar `https://dl.google.com/android/cli/latest/linux_x86_64/android` a `~/.local/bin/android` y darle permiso de ejecución.
3. `android sdk install platforms/android-37.0 build-tools/36.0.0 platform-tools`.
4. `source scripts/dev-env.sh` y `./gradlew testDebugUnitTest`.

## ⚠ La licencia del SDK se aceptó automáticamente

En su primer arranque, el CLI de Android muestra los *Términos de Servicio y el Acuerdo de Licencia del SDK de Android* y los da por aceptados: crea `~/Android/Sdk/licenses/android-sdk-license`. Es el comportamiento normal del instalador oficial, pero **ocurrió sin que se te mostrara**, porque lo ejecuté yo como parte de la instalación. La CI hace lo mismo con `yes | sdkmanager --licenses`.

Para revertirlo, borra ese archivo (y el SDK si no lo quieres):

```
rm ~/Android/Sdk/licenses/android-sdk-license
```

Sin él, el SDK no se puede actualizar ni ampliar hasta que se acepte de nuevo (`android sdk install …` o `sdkmanager --licenses`).

Sobre las **analíticas** del CLI: `~/.android/analytics.settings` tiene `"hasOptedIn": false`, es decir, no envía nada.

## Desinstalar

```
rm -rf ~/.local/dev-tools ~/Android/Sdk ~/.android ~/.local/bin/android
rm -rf ~/.gradle          # caché de dependencias; ocupa varios cientos de MB
```

## Lint: estado inicial

6 avisos, ningún error:

| Aviso | Veces | Qué es |
|---|---|---|
| `InlinedApi` | 3 | Constantes de permisos de APIs más nuevas que `minSdk 26` (`ACTIVITY_RECOGNITION`, `USE_BIOMETRIC`, `POST_NOTIFICATIONS`); son cadenas, no rompen en versiones antiguas y la app ya las condiciona por versión con `sinceSdk` |
| `OldTargetApi` | 1 | `targetSdk 36` aunque ya hay API 37; subirlo exige revisar los cambios de comportamiento |
| `DataExtractionRules` | 1 | Falta `android:dataExtractionRules` (Android 12+); la app ya declara `allowBackup="false"` |
| `ReportShortcutUsage` | 1 | Se recomienda llamar a `reportShortcutUsed` al abrir un atajo dinámico, para que el lanzador los ordene por uso |

Se revisarán con la automatización del lote 3.

## Pendiente de este lote

- El teléfono por USB (`adb`, `android run`, `android layout`) queda para cuando se quiera: `platform-tools` ya está instalado y el emulador no, a propósito.
- El Android Studio sigue sin hacer falta: el proyecto se abre con el wrapper, y Studio lo reconoce.
