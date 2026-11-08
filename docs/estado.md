# Estado actual

Última actualización: 2026-11-08 (Inspector del dispositivo, sin publicar en `dev`). Decisiones vigentes: [decisiones.md](decisiones.md). El detalle histórico está en `archivo/` (no hace falta leerlo para trabajar).

## Versión y ramas

| | |
|---|---|
| Última release | `v0.2.1` en `main` (PR #15), 2026-10-15 |
| `dev` | compila como `0.2.2-dev.N` (sin cambios que suban versión desde la release) |
| Pendiente | nada por promocionar |
| Ramas | las `fase-*` ya fusionadas se borraron (locales y en `origin`); cada fase nueva sale de `dev`. Dependabot abre PR a `dev` desde ramas `dependabot/*` |

## Módulos de la app

| Módulo | Estado |
|---|---|
| **Inspector del dispositivo** | Hecho (equivalente a Browser Scope Inspector). Lo que cualquier app lee **sin permisos en ejecución**: hardware, sistema, pantalla, red, región y accesibilidad, identificadores (`ANDROID_ID`, apps visibles, huella combinada SHA-256), sensores en vivo y portapapeles (descripción pasiva y lectura al pulsar). Tarjeta destacada en inicio, ruta `inspector` (sin atajo ni deep link). Ideas para ampliarlo: biometría del comportamiento (toques y gestos), lista completa de sensores, apps instaladas con `<queries>` |
| **Ubicación** | Mapa de OpenStreetMap con tu posición y la ruta en vivo (distancia, velocidad, tiempo, precisión). **Falta** la detección de actividad física (`ActivityRecognitionClient`, interruptor opcional con `ACTIVITY_RECOGNITION`) |
| **Cámara y vídeo** | Solo pantalla genérica de permisos. Pendiente: CameraX (vista previa, foto, vídeo) y Media3 para reproducir |
| **Biometría** | Solo pantalla genérica. Pendiente: `BiometricPrompt` + Keystore (nota secreta). Exigirá pasar `MainActivity` a `FragmentActivity` |
| **Segundo plano** | Solo pantalla genérica. Pendiente: WorkManager con progreso y notificación final (`POST_NOTIFICATIONS`) |

Orden propuesto para lo que falta: segundo plano → biometría → cámara. Después: pulido (fase 6) y, opcional, Play Integrity (fase 7). Cada demo debe ser un uso real del permiso, no solo texto: el usuario quiere ver y poder comprobar lo que se hace con él.

Ya hecho: pantalla principal con una tarjeta por módulo, gestor de permisos reutilizable (rationale, denegación permanente, "Concedido al instalar"), pie con la versión, icono en pantalla de inicio y atajos a los 4 módulos.

## Verificado en el dispositivo

motorola edge 40 neo, Android 15 (API 35), Play Services presente. Se comprobó con `adb` (15/10/2026): atajos con la app abierta, cerrada y en segundo plano, ignorar enlaces no válidos, estados de permisos al revocar y conceder, y ningún cierre ni ANR en el log. Los diálogos del sistema (permisos, rationale, salida a Ajustes, "Añadir a la pantalla de inicio") no se pueden automatizar por `adb`: están cubiertos por tests de Robolectric, y el usuario comprobó a mano que se piden los permisos y que funcionan los atajos.

## Tests y calidad

- **206 tests** JVM + Robolectric (0 fallos) y comprobaciones de scripts bash.
- **Cobertura de líneas 90,9 %** (ramas 71 %, sin `RouteMap` ni código generado). Lo peor: `shortcuts` 15 % (`AppShortcuts` sin test), `MainActivity` 0 % y `LocationRepository` (Play Services). La CI exige un mínimo (`app/coverage-min.txt`, 88 %).
- Comprobación de mutaciones: 8 errores introducidos a mano, los 8 detectados.
- Huecos conocidos (ver cobertura): el mapa real (osmdroid) no se prueba (se sustituye en los tests), `AppShortcuts` solo en su lógica, sin tests instrumentados ni de capturas. Robolectric corre en API 36 y el móvil lleva API 35.
- Formato con ktlint (Spotless) y lint estricto: 0 avisos; lo ignorado a propósito está en `app/lint.xml`.

## CI

Un solo workflow (`.github/workflows/build.yml`), en `ubuntu-24.04` con JDK 21 y `./gradlew`: jobs `build` (tests de scripts, cálculo de versión, formato, tests, lint, APK y verificación con `aapt2`), `guard` (detecta pushes directos a `main`/`dev`) y `release` (tag y GitHub Release solo desde `main`). Tarda 2-6 minutos.

- **Informes**: cada ejecución sube el artefacto `reports-<versión>` (informes HTML/XML de tests y lint, 14 días) y escribe en el resumen del job el recuento de tests y avisos de lint (`scripts/test-summary.py`), también cuando falla. Gradle corre con `--continue` para que el lint se ejecute aunque falle un test.
- `concurrency`: un push nuevo a una PR o a una rama `fase-*` cancela la ejecución anterior; `main` y `dev` nunca se cancelan. `timeout-minutes`: 25 (`build`), 5 (`guard`), 10 (`release`).
- Caché de la descarga de Robolectric (~200 MB). Gradle con `org.gradle.parallel` y configuration cache.

## Entorno

Local, sin Android Studio y sin sudo:

```
source scripts/dev-env.sh                    # JDK 21 + Android SDK del HOME; no toca archivos de perfil
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew --stop                             # libera el daemon al terminar
```

Si falta el entorno: JDK 21 Temurin y Gradle 9.8.1 en `~/.local/dev-tools` (comprobando sus sha256), el binario `android` de `https://dl.google.com/android/cli/latest/linux_x86_64/android` en `~/.local/bin`, y `android sdk install platforms/android-37.0 build-tools/36.0.0 platform-tools`. La primera compilación tarda unos 6 minutos (descarga dependencias) y las siguientes, segundos. Sin ese entorno, la verificación es la CI.

Dos cosas del `android` CLI: en su primer arranque **aceptó automáticamente la licencia del SDK** (para revertirlo, `rm ~/Android/Sdk/licenses/android-sdk-license`), y `android layout` **instala en el móvil** el paquete `com.android.cli.interact.instrumentation`, que hay que desinstalar al terminar (`adb shell pm uninstall com.android.cli.interact.instrumentation`).

Depuración USB: `adb` está en `~/Android/Sdk/platform-tools`. Autorizar el ordenador en el móvil lo deja controlable por `adb` hasta revocarlo en Opciones de desarrollador. Para instalar un APK de otra ejecución de CI puede hacer falta desinstalar antes (cada ejecución firma con una clave de debug distinta; no verificado).

## Próximos pasos

1. Revisar y fusionar las PR de Dependabot (la primera, #16).
2. Módulos pendientes (ver tabla).
3. ~~Lote 3 de automatización~~ **hecho** (CI con informes, ktlint y lint estricto, cobertura con mínimo, Dependabot, plantilla de PR y hook `commit-msg`); plan y decisiones en [plan-lote-3.md](plan-lote-3.md). Dependabot, activo desde que su configuración llegó a `main` (la lee de la rama por defecto), abre PR a `dev` los lunes; se fusionan una a una por el flujo normal.
4. **Lote 4, a valorar:** capturas de pantalla (con AGP 9.4.1 sería el plugin antiguo), E2E mínimo, R8.
5. Subir `targetSdk` a 37 y revisar los avisos de lint cuando convenga.
