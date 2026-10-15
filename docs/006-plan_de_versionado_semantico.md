# Plan: versionado semántico automático en cada push, visible en la app

> **Estado:** implementado en la rama `fase-versionado`; uso y detalles en [007](007-versionado_semantico.md). Relacionado con el [plan de fases](004-plan_de_fases_detallado.md) y con el pendiente del `applicationId` de [005](005-bloqueo_osm_tiles_y_correccion.md).

## Context
Hoy la versión está escrita a mano en `app/build.gradle.kts` (`versionName = "0.1.0"`, `versionCode = 1`) y nunca cambia; no hay tags en el repo. En *Información de la app* se ve `0.1.0` y el paquete `com.example.scopeinspector`. El usuario quiere que **cada push cambie la versión siguiendo semver**, que la versión se vea **en la parte inferior de la app**, y (decidido) que el `applicationId` pase a `io.github.emaza.scopeinspector`.

## Decisiones tomadas (respuestas del usuario)
1. **Criterio de bump:** Conventional Commits. `feat:` → minor, `fix:`/`perf:` → patch, `tipo!:` o `BREAKING CHANGE:` → major. El resto (`docs`, `chore`, `test`, `ci`, `refactor`…) no sube versión. A partir de ahora los commits se escriben con este formato (tipo en inglés, descripción en español, p. ej. `feat(ubicacion): detectar actividad física`).
2. **Ramas:** estable solo en `main` (tag `vX.Y.Z` + GitHub Release con el APK). En `dev`, `fase-*` y PRs: pre-release sin tag, `X.Y.Z-<rama>.<nº de build>` (p. ej. `0.2.0-fase-2-ubicacion.58`).
3. **applicationId:** `io.github.emaza.scopeinspector`; `namespace` y paquetes de código no cambian (el código no se mueve). Android lo instala como app nueva: hay que desinstalar la anterior y los permisos empiezan de cero.

## Diseño

**Cálculo de versión** (`scripts/compute-version.sh`, bash + git, sin dependencias):
1. Base = tag `v*` más cercano alcanzable desde HEAD (`git describe --tags --abbrev=0 --match 'v[0-9]*'`); si no hay tags, base `0.1.0` y se examina todo el historial (los commits antiguos no son convencionales, así que no cuentan).
2. Bump = el mayor entre los commits `base..HEAD` (major > minor > patch > ninguno), leyendo asunto y cuerpo.
3. `main` con bump → versión estable `X.Y.Z` (siguiente a la base) y se publica tag y release. `main` sin bump → no hay release; versión `base+<nº build>` (metadata de build).
4. Resto de ramas y PRs → siguiente versión (si no hay bump, patch) + `-<rama normalizada>.<nº build>`.
5. `versionCode` = `GITHUB_RUN_NUMBER` (entero creciente y único entre ramas). Sin CI: `1`.
6. Salida: `versionName`, `versionCode`, `isRelease`, `tag` (a `$GITHUB_OUTPUT` y por stdout).

**Gradle** (`app/build.gradle.kts`): `versionName`/`versionCode` leen las propiedades `appVersionName` / `appVersionCode` (`providers.gradleProperty`) con valores por defecto `0.1.0-dev` / `1`; CI las pasa con `-P`. `applicationId` → `io.github.emaza.scopeinspector`. `BuildConfig.VERSION_NAME` ya se usa en el User-Agent de OSM (`feature/location/RouteMap.kt`), así que seguirá la versión sin tocar nada (el producto `ScopeInspector` se mantiene fijo, como exige la política).

**CI** (`.github/workflows/build.yml`):
- `actions/checkout` con `fetch-depth: 0` (necesario para tags e historial).
- Paso previo: tests del script (`scripts/test-compute-version.sh`) y cálculo de versión.
- Gradle con `-PappVersionName=… -PappVersionCode=…`; nombre del artefacto y del APK con la versión (`scope-inspector-<versión>.apk`).
- Paso de verificación: `aapt2 dump badging` del APK (viene en build-tools) para dejar en el log paquete, `versionName` y `versionCode` reales; resumen de la versión en `$GITHUB_STEP_SUMMARY`.
- Solo en push a `main` con bump: `permissions: contents: write`, crear y subir el tag y `gh release create` con el APK y notas generadas. El push de tag con `GITHUB_TOKEN` no relanza el workflow.

**Versión visible en la app:** composable nuevo `ui/components/VersionFooter.kt` con el texto `v<versión> · build <código>` (string `version_label`), colocado en `MainActivity` bajo el `AppNavGraph` en una `Column`: el grafo con `weight(1f)` y `consumeWindowInsets(navigationBars)` para que los `Scaffold` internos no dupliquen el margen, y el pie con `navigationBarsPadding()` para no quedar bajo la barra de navegación. Visible en todas las pantallas. Datos de `BuildConfig` (ya habilitado).

**Documentación:** un documento de uso, `docs/007-versionado_semantico.md`, (convención de commits, cómo se calcula la versión, ejemplos, qué ocurre en `main` frente al resto, cómo desinstalar la app antigua) y actualizar el pendiente del `applicationId` en `docs/005-...`.

## Archivos
- Nuevos: `scripts/compute-version.sh`, `scripts/test-compute-version.sh`, `app/src/main/java/com/example/scopeinspector/ui/components/VersionFooter.kt`, `docs/007-versionado_semantico.md`.
- Modificados: `.github/workflows/build.yml`, `app/build.gradle.kts`, `app/src/main/java/com/example/scopeinspector/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `docs/005-bloqueo_osm_tiles_y_correccion.md`.

## Flujo de trabajo
- Rama nueva `fase-versionado` (casa con el trigger `fase-*`) desde `fase-2-ubicacion`, que ya contiene todo el código; la deja intacta para retomar la detección de actividad después. El `docs/005` (aún sin commitear) entra en el primer commit.
- Commits con el formato convencional; el commit principal será `feat(version): …` (hace que esta rama salga como `0.2.0-fase-versionado.N`).

## Riesgos y notas
- Los merges a `main` deben llevar mensajes convencionales: con "squash merge" cuenta el título del PR; con merge normal cuentan los commits de la rama.
- Con versión `0.x`, un `feat!:` llevaría a `1.0.0`; se usa semver estándar (usar `!` solo cuando se quiera dar ese salto).
- El APK de la release va firmado con la clave de debug (no hay keystore); sirve para instalar a mano, no para Google Play.
- Sin entorno local de Android: la compilación se verifica en CI; el script de versión sí se prueba aquí, porque solo necesita git.

## Verificación
1. `bash scripts/test-compute-version.sh` en local y en CI: repos git temporales con casos (sin tags, `feat`, `fix`, `feat!`, solo `docs`, rama no-main, `main`) y comprobación de la salida.
2. Ejecutar `scripts/compute-version.sh` sobre este repo y revisar que da la versión esperada.
3. CI en verde en `fase-versionado`; en el log, `aapt2 dump badging` muestra `package: io.github.emaza.scopeinspector` y el `versionName`/`versionCode` calculados; los tests unitarios y el lint pasan.
4. En el teléfono (tras desinstalar la app antigua): *Información de la app* muestra la versión nueva y el paquete nuevo; el pie `v… · build …` se ve en Home y en cada módulo, por encima de la barra de navegación; el mapa sigue cargando.
5. Al mergear a `main` (cuando el usuario lo decida): aparece el tag `vX.Y.Z` y la GitHub Release con el APK.
