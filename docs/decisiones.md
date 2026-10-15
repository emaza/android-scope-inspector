# Decisiones vigentes

Resumen corto de lo que está decidido y por qué. Estado: [estado.md](estado.md). Si algo cambia, se edita aquí; el razonamiento original está en `archivo/` (solo si hace falta, y lo pide el usuario).

## Producto y stack
- **Kotlin + Jetpack Compose + Navigation Compose**, una sola actividad. `minSdk 26`, `compileSdk 37`, `targetSdk 36`. AGP 9.4.1, Kotlin 2.4.21, Gradle 9.8.1 con wrapper.
- **Cada módulo es una demo real del permiso**, con evidencia visible (mapa, ruta, estado), no solo texto. El permiso se pide al pulsar la acción, no al abrir la pantalla.
- **Mapa: OpenStreetMap con osmdroid** (sin API key). Obligatorio un User-Agent propio `ScopeInspector/<versión> (https://github.com/emaza/android-scope-inspector)` y la atribución "© OpenStreetMap contributors" visible; un `com.example.*` o un User-Agent genérico recibe la imagen "403 Access blocked". Caché de tiles en la caché de la app, sin permisos de almacenamiento.
- **Identificador:** `applicationId = io.github.emaza.scopeinspector`; el `namespace` del código sigue siendo `com.example.scopeinspector`.

## Permisos
- Estados: concedido, denegado, denegado permanentemente y no aplica (según versión de Android). La denegación permanente se deduce de "ya se pidió y no hay rationale", porque Android no la informa.
- Los permisos **normales** (p. ej. `USE_BIOMETRIC`) se muestran como "Concedido al instalar" con una explicación; no se piden en ejecución.
- Nada de pedir ubicación en segundo plano ni en la misma solicitud que la precisa.

## Accesos directos
- Android **no permite crear iconos al instalar**. La app ofrece el icono en la pantalla de inicio la **primera vez que se abre** (una sola vez; el sistema pide confirmación). Ese acceso directo fijado **no muestra los atajos**: estos salen solo en el icono real de la app (cajón de aplicaciones o arrastrado a la pantalla de inicio).
- Atajos dinámicos a los 4 módulos, generados desde `DemoModule`. Enlace interno `scopeinspector://module/<ruta>`, sin `intent-filter`. `MainActivity` es `singleTask` y está **exportada**: acepta intents explícitos de otras apps, así que los módulos sensibles no deben depender de llegar por navegación.
- Con la app abierta, `AppNavGraph` resuelve la ruta él mismo (solo módulos conocidos, `popUpTo(home)` + `launchSingleTop`), porque `handleDeepLink` con las flags de un atajo intenta relanzar la tarea y no navega. Verificado en dispositivo real.

## Flujo de trabajo
- **`main` y `dev` solo cambian mediante pull request**, fusionada con **merge commit** (nunca squash: el versionado lee los mensajes de los commits).
- Ramas de trabajo `fase-*`, siempre desde `dev`. `main` = releases con tag; `dev` = integración.
- El repo es **privado con cuenta gratuita**: GitHub no permite protección de ramas. Se hace cumplir por convención: job `guard` en CI (marca en rojo un push directo a `main`/`dev` y bloquea la release; no revierte) y hook local `.githooks/pre-push` (`git config core.hooksPath .githooks`; se salta con `--no-verify`).
- **Commits convencionales**, tipo en inglés y descripción en español, con `Co-Authored-By` al final.
- Comandos de una fase: rama → commits → `git push -u origin <rama>` → `gh pr create --base dev` → `gh pr checks --watch` → `gh pr merge <n> --merge`. Para una release, PR de `dev` a `main`.

## Versionado (`scripts/compute-version.sh`)
- `feat` sube minor, `fix` y `perf` patch, `tipo!` o `BREAKING CHANGE:` major; `docs`, `chore`, `ci`, `test`, `refactor`, `style`, `build` y `revert` no suben. Gana el mayor entre los commits desde la base.
- **Base:** el tag `vX.Y.Z` más alto del repositorio, aunque no esté en el historial de la rama (con merge commit, `dev` no contiene el tag). Sin tags, 0.1.0.
- `main` con cambios que suben versión → `X.Y.Z` con tag y GitHub Release con el APK. `main` sin ellos → `X.Y.Z+<build>`. Resto de ramas y PR → `X.Y.Z-<rama>.<build>`, sin tag. `versionCode` = número de ejecución del workflow.
- La versión se ve en el pie de la app y en Información de la app; la CI la verifica en el APK con `aapt2`.

## CI y release
- `ubuntu-24.04` fijado (`ubuntu-latest` migra a Ubuntu 26 el 19/10/2026), JDK 21, `./gradlew` con el checksum del wrapper validado.
- El APK de la release va **firmado con la clave de debug**: sirve para instalar a mano, no para Google Play.

## Tests
- **Sin framework de DI**: interfaz + constructor + fábrica manual + fake en test (`LocationSource`, `ElapsedClock`). Se migra a Hilt solo si la app crece mucho.
- JUnit 4, `kotlinx-coroutines-test`, Robolectric + Compose Test. **Robolectric exige JDK 21** para Android SDK 36.
- Un fallo se arregla con un test que lo reproduce y falla antes del arreglo.
- La lógica va en funciones puras fuera de Android (`RouteMath`, `ShortcutPolicy`, `planPermissionRequest`).

## Entorno de desarrollo
- Local en `~/.local/dev-tools` y `~/Android/Sdk`, sin Android Studio, sin sudo y sin tocar archivos de perfil (`scripts/dev-env.sh`).
- Con el móvil conectado, `adb` solo sobre esta app; sin capturas de pantalla salvo que se pidan; todo lo que se cambie en el móvil se restaura al terminar.

## Documentación
- `docs/estado.md` y `docs/decisiones.md` son lo vivo y se mantienen al día. `docs/archivo/` es el histórico detallado y no se lee ni se busca en él salvo petición explícita.
