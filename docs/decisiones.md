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

## Inspector del dispositivo
- Pantalla aparte de los 4 módulos (no es un `DemoModule`: no pide permisos, así que no tiene atajo ni entra en `DemoModuleTest`). Muestra solo lo que una app lee sin permisos en ejecución (con `ACCESS_NETWORK_STATE`, normal).
- Datos en bruto en `DeviceSnapshot` (fuente `AndroidDeviceInfoSource`, fake en tests); la presentación y la huella son puras en `InspectorReport`.
- La huella combinada es un SHA-256 (16 hex) de datos estables: no incluye `ANDROID_ID` (distinto por app desde Android 8), batería, RAM libre, orientación ni red. La RAM total se redondea a GB.
- No se declara `<queries>`: se enseña cuántas apps ve una app normal con la visibilidad de paquetes (se suprime `QueryPermissionsNeeded` y `HardwareIds` en ese método, con su motivo).
- El portapapeles solo se **lee** al pulsar el botón (desde Android 12 el sistema avisa al usuario); la descripción (tipos MIME) se consulta al abrir, porque no genera aviso.

## Accesos directos
- Android **no permite crear iconos al instalar**. La app ofrece el icono en la pantalla de inicio la **primera vez que se abre** (una sola vez; el sistema pide confirmación). Ese acceso directo fijado **no muestra los atajos**: estos salen solo en el icono real de la app (cajón de aplicaciones o arrastrado a la pantalla de inicio).
- Atajos dinámicos a los 4 módulos, generados desde `DemoModule`. Enlace interno `scopeinspector://module/<ruta>`, sin `intent-filter`. `MainActivity` es `singleTask` y está **exportada**: acepta intents explícitos de otras apps, así que los módulos sensibles no deben depender de llegar por navegación.
- Con la app abierta, `AppNavGraph` resuelve la ruta él mismo (solo módulos conocidos, `popUpTo(home)` + `launchSingleTop`), porque `handleDeepLink` con las flags de un atajo intenta relanzar la tarea y no navega. Verificado en dispositivo real.

## Flujo de trabajo
- **`main` y `dev` solo cambian mediante pull request**, fusionada con **merge commit** (nunca squash: el versionado lee los mensajes de los commits).
- Ramas de trabajo `fase-*`, siempre desde `dev`. `main` = releases con tag; `dev` = integración.
- El repo es **privado con cuenta gratuita**: GitHub no permite protección de ramas. Se hace cumplir por convención: job `guard` en CI (marca en rojo un push directo a `main`/`dev` y bloquea la release; no revierte) y hook local `.githooks/pre-push` (`git config core.hooksPath .githooks`; se salta con `--no-verify`).
- **Commits convencionales**, tipo en inglés y descripción en español, con `Co-Authored-By` al final.
- **Hook `commit-msg`** (mismo `core.hooksPath`): rechaza asuntos sin `tipo(ámbito)!: descripción`; deja pasar los `Merge`, `Revert` y `fixup!` que genera git. Las reglas (tipos y subida de versión) están en `scripts/conventional-commits.sh`, única fuente de verdad que cargan `compute-version.sh`, el hook y `check-commit-format.sh`. En las PR, la CI lista en el resumen y como aviso los commits sin formato (no bloquea).
- **Dependabot** (`.github/dependabot.yml`): Gradle y GitHub Actions, semanal (lunes), hacia `dev`, máx. 5 PR abiertas, agrupadas (androidx, kotlin, test), prefijo `build(deps)` / `ci(deps)` (no suben versión). Ignora los saltos **mayores** de AGP y Kotlin: van acoplados y se suben a mano. **Dependabot lee `dependabot.yml` de la rama por defecto (`main`)**: no actúa hasta que el archivo llegue a `main` (promoción de `dev`).
- Hay plantilla de PR (`.github/pull_request_template.md`).
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

## Calidad de código
- **Formato: ktlint con Spotless** (`./gradlew spotlessApply` / `spotlessCheck`, este último en la CI). No detekt: su versión estable (1.23.8) apunta a Kotlin 2.0 y el proyecto usa 2.4. Estilo `android_studio` con comas finales, definido en `build.gradle.kts` porque Spotless no lee `ktlint_code_style` de `.editorconfig` (sí `max_line_length`, 120).
- **Lint estricto:** `warningsAsErrors = true`; lo que se ignora a propósito está en `app/lint.xml` con su motivo (`OldTargetApi`, `ReportShortcutUsage`, y los avisos de versiones nuevas, que son cosa de Dependabot). `InlinedApi` se suprime en `AppPermission` porque `sinceSdk` ya condiciona el uso.
- **Cobertura con Jacoco 0.8.15** (tests locales, incluidos los de Robolectric: necesitan `isIncludeNoLocationClasses` y excluir `jdk.internal.*`). El resumen (`scripts/coverage-summary.py`) deja fuera lo generado y `RouteMap`. **Trinquete:** la CI falla si las líneas bajan del mínimo de `app/coverage-min.txt` (88 %, medido 90,9 %); el mínimo solo se sube.
- Reglas de copia de seguridad (`data_extraction_rules.xml`, `backup_rules.xml`): se excluye todo, coherente con `allowBackup="false"`.

## Entorno de desarrollo
- Local en `~/.local/dev-tools` y `~/Android/Sdk`, sin Android Studio, sin sudo y sin tocar archivos de perfil (`scripts/dev-env.sh`).
- Con el móvil conectado, `adb` solo sobre esta app; sin capturas de pantalla salvo que se pidan; todo lo que se cambie en el móvil se restaura al terminar.

## Documentación
- `docs/estado.md` y `docs/decisiones.md` son lo vivo y se mantienen al día. `docs/archivo/` es el histórico detallado y no se lee ni se busca en él salvo petición explícita.
