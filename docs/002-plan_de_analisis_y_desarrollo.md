# Plan: Android Scope Inspector (app demo de APIs y permisos)

## Context
El repo `android-scope-inspector` está vacío (solo README, LICENSE, .gitignore y `docs/001-...md`). La guía define una app demo con 4 módulos (ubicación/actividad, cámara/vídeo, biometría, WorkManager/notificaciones), cada uno con sus permisos en tiempo de ejecución y una estrategia de solicitud concreta. Objetivo: app pedagógica que muestre cada API y justifique cada permiso (Google Play). No hay JDK, adb ni ANDROID_HOME en la máquina.

## Supuestos (cambiables antes de implementar)
- Kotlin + Jetpack Compose + Navigation Compose, single-activity, `minSdk 26`, `targetSdk` el más reciente estable.
- Mapa con **osmdroid** (sin API key). Alternativa: Maps Compose (requiere key de Google Cloud).
- Build con Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`).
- Sin DI framework: ViewModels simples con factory manual (es una demo).
- Play Integrity (punto 4 de la guía, opcional) queda como fase final opcional.

## Análisis (resumen de la guía)
| Módulo | APIs | Permisos | Disparador del permiso |
|---|---|---|---|
| 1 Ubicación + actividad | FusedLocationProviderClient, ActivityRecognitionClient | FINE/COARSE_LOCATION, ACTIVITY_RECOGNITION (API 29+) | Botón "Iniciar Rastreo de Ruta" (no en arranque) |
| 2 Cámara/vídeo | CameraX, Media3/ExoPlayer | CAMERA, RECORD_AUDIO | Al abrir la cámara; rationale tras primera denegación |
| 3 Biometría | BiometricPrompt, Keystore/EncryptedSharedPreferences | USE_BIOMETRIC | `canAuthenticate()` antes de mostrar el botón |
| 4 Background | WorkManager, NotificationManager | POST_NOTIFICATIONS (API 33+) | Al programar la tarea; notificación al terminar |

Riesgos/notas a resolver en implementación:
- `EncryptedSharedPreferences` (security-crypto) está deprecada: usar Keystore + `BiometricPrompt.CryptoObject` directamente, o DataStore cifrado con una clave de Keystore.
- Pedir FINE y COARSE juntos; manejar que el usuario conceda solo aproximada.
- `ACTIVITY_RECOGNITION` solo existe como permiso de runtime desde API 29; condicionar por versión.
- `POST_NOTIFICATIONS` solo desde API 33; en inferiores no se pide.
- Rastreo en primer plano: si la ruta debe seguir con la app minimizada, haría falta un foreground service (`FOREGROUND_SERVICE_LOCATION`); **fuera de alcance** salvo que se pida (la guía solo habla de rastreo con la app abierta).
- La guía menciona `registerForActivityResult`; en Compose el equivalente es `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission/RequestMultiplePermissions)`.

## Arquitectura propuesta
Paquete base `com.example.scopeinspector` (ajustar a gusto):
```
app/src/main/
  AndroidManifest.xml          # permisos declarados + <uses-feature required="false">
  java/.../
    MainActivity.kt            # setContent + NavHost
    navigation/AppNavGraph.kt  # home, location, camera, biometric, background
    permissions/
      PermissionState.kt       # enum Granted/Denied/PermanentlyDenied/NotApplicable (por API level)
      PermissionHandler.kt     # composable helper: launcher + rationale + ir a Ajustes
      RationaleDialog.kt
    ui/home/HomeScreen.kt      # lista de módulos + estado de permisos de cada uno (el "inspector")
    feature/location/          # LocationScreen, LocationViewModel, LocationRepository, ActivityRecognitionReceiver
    feature/camera/            # CameraScreen (CameraX preview+captura+vídeo), PlaybackScreen (Media3)
    feature/biometric/         # BiometricScreen, BiometricAuthenticator, SecureStore (Keystore)
    feature/background/        # BackgroundScreen, DemoWorker, NotificationHelper
    ui/theme/
  res/                         # strings (es), iconos, canal de notificación
```
Pieza transversal clave: `PermissionHandler` reutilizable para cumplir "manejo de negativa con degradación elegante" en los 4 módulos (denegado → mensaje + funcionalidad reducida, nunca crash; denegado permanente → botón a Ajustes de la app).

## Fases de desarrollo
0. **Esqueleto + CI (sin entorno local)**: no se instala JDK/SDK en la máquina. Crear proyecto Gradle (`settings.gradle.kts`, `build.gradle.kts`, `app/`, version catalog). Sin wrapper por ahora: el `gradle-wrapper.jar` del repo oficial de Gradle (tag v9.8.1) no coincide con el checksum publicado y el jar oficial no es descargable, así que no se commitea un binario sin verificar; CI usa `gradle/actions/setup-gradle` con `gradle-version: 9.8.1` y ejecuta `gradle` (el wrapper se podrá añadir después con `gradle wrapper` o desde Android Studio). Verificar que `.gitignore` cubra `local.properties`, `build/`, `.gradle/`. Crear `.github/workflows/build.yml`:
   - Triggers: `push` y `pull_request` a `main`, más `workflow_dispatch`.
   - Pasos: `actions/checkout` → `actions/setup-java` (temurin 17) → `gradle/actions/setup-gradle` (caché) → `gradle testDebugUnitTest lintDebug assembleDebug` → `actions/upload-artifact` con `app/build/outputs/apk/debug/*.apk`.
   - (El runner `ubuntu-latest` ya trae el Android SDK.)
   - Opcional: job de release al empujar un tag `v*` que adjunte el APK a un GitHub Release; firma de release con keystore en GitHub Secrets (solo si se quiere distribuir algo más que el APK debug).
   - Requisito: el repo debe tener remoto en GitHub (hoy solo hay el commit inicial local). Bucle de trabajo: push → `gh run watch` / `gh run view --log-failed` (si `gh` está autenticado) para leer errores de compilación sin salir del terminal.
1. **Base transversal**: tema, navegación, HomeScreen con 4 tarjetas, `PermissionHandler` + `RationaleDialog` + tests unitarios de la lógica de estado (API-level gating).
2. **Módulo 4 – Background/notificaciones** (el más simple, valida el patrón de permisos): canal de notificación, `DemoWorker` (`OneTimeWorkRequest` con delay), permiso POST_NOTIFICATIONS en API 33+, notificación al terminar; degradación: sin permiso el worker corre igual y se informa en pantalla.
3. **Módulo 3 – Biometría**: `BiometricManager.canAuthenticate()` decide si se muestra el botón; `BiometricPrompt` desbloquea una pantalla/valor protegido con clave Keystore; estados: no soportado, no enrolado, fallo, éxito.
4. **Módulo 1 – Ubicación y actividad**: botón "Iniciar Rastreo de Ruta" → pide FINE+COARSE; `FusedLocationProviderClient` con `LocationRequest`; dibujar polilínea en osmdroid; `ActivityRecognitionClient` (transitions o updates) como solicitud separada y opcional; degradación a ubicación aproximada o sin actividad.
5. **Módulo 2 – Cámara**: CameraX (Preview + ImageCapture + VideoCapture); rationale tras primera denegación de CAMERA; RECORD_AUDIO opcional (vídeo sin audio si se deniega); guardar en almacenamiento de app (cache/`MediaStore` propio, sin permisos de storage); `PlaybackScreen` con Media3 ExoPlayer.
6. **Pulido**: textos explicativos por módulo ("por qué pedimos este permiso"), strings en español, accesibilidad, modo oscuro, README con capturas y requisitos.
7. **Opcional**: Play Integrity API (requiere proyecto Google Cloud/Play Console; solo si se quiere).

## Archivos críticos a crear/editar
- `.github/workflows/build.yml` (CI + APK como artefacto)
- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml` (permisos: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACTIVITY_RECOGNITION, CAMERA, RECORD_AUDIO, USE_BIOMETRIC, POST_NOTIFICATIONS, INTERNET para tiles del mapa)
- `permissions/PermissionHandler.kt` (reutilizado por los 4 módulos)
- `README.md` (actualizar)

## Verificación
- Build: lo ejecuta GitHub Actions (`testDebugUnitTest`, `lintDebug`, `assembleDebug`); cada fase termina con CI en verde y APK debug como artefacto descargable.
- Limitación asumida: sin entorno local no hay compilación instantánea ni emulador; los errores de compilación se descubren en CI (ciclo de ~3-6 min). Los tests instrumentados/UI de permisos no se ejecutan en CI (emulador en runner es lento y frágil); la verificación funcional es manual en tu teléfono instalando el APK (activar "instalar apps de orígenes desconocidos"; el APK debug va firmado con clave de debug).
- Manual en dispositivo, por módulo: (a) primera vez → diálogo del sistema solo al pulsar la acción; (b) denegar → la app sigue funcionando degradada; (c) denegar con "no volver a preguntar" → aparece botón a Ajustes; (d) conceder → funciona.
- Probar en el teléfono real disponible (indicar su versión de Android); el gating por API level (<29, <33) queda cubierto por tests unitarios en CI.
- Biometría: probar con huella enrolada y, si es posible, sin enrolar. Ubicación: caminar o usar app de ubicación simulada. Cámara: dispositivo físico.
- Lint: `lintDebug` en CI sin errores de permisos/manifest.
