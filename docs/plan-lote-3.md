# Plan del lote 3: automatización y calidad (CI, formato, cobertura, colaboración)

> **Estado:** aprobado el 2026-10-15; en ejecución. Los cuatro bloques (A a D) están hechos. Estado general del proyecto: [estado.md](estado.md). Decisiones vigentes: [decisiones.md](decisiones.md).

## Context
Los lotes 0-2 dejaron el entorno local, 165 tests y la CI en verde. El lote 3 convierte eso en una red de seguridad automática: que la CI deje **evidencias** cuando algo falla, que el código tenga un **formato y un lint exigibles**, que se **mida la cobertura**, y que las dependencias, los commits y las PR sigan **reglas comprobables**. Ya están hechos `AGENTS.md` y la estrategia de tests (lote 2), así que no entran aquí. Estado de partida (`dev` en `2c51f9c`): un solo workflow `build.yml` (jobs `build`, `guard`, `release`) sin `concurrency` ni `timeout-minutes`; sin informes subidos como artefactos; sin `.editorconfig`, linter ni Dependabot; `gradle.properties` con solo 4 líneas; lint con 6 avisos y 0 errores.

## Decisiones recomendadas (se pueden cambiar al aprobar)
| Tema | Recomendación | Por qué |
|---|---|---|
| Formato / estática | **ktlint vía Spotless**, no detekt | detekt estable (1.23.8) apunta a Kotlin 2.0 y el proyecto usa 2.4.21; la línea que soporta Kotlin 2.x es la `2.0.0-alpha.6`. Spotless actúa sobre los archivos y no depende de cómo AGP 9 modela las fuentes de Kotlin (el plugin `ktlint-gradle` sí) |
| Cobertura | **Informar primero, bloquear después** | Con la cobertura medida se fija un umbral realista; bloquear a ciegas genera ruido |
| Lint | **Estricto**: `warningsAsErrors` tras arreglar o justificar los 6 avisos actuales | Los avisos nuevos fallan la CI en vez de acumularse |
| Dependabot | Semanal, **hacia `dev`**, agrupado, mensajes `build(deps)` / `ci(deps)` | Casan con los commits convencionales (no suben versión) y con la regla de que `main`/`dev` solo cambian por PR |
| Orden | A → B → C → D, **una PR por bloque** | Cada bloque se verifica solo y es reversible; el formato (B) va pronto para no chocar con los módulos que vienen |

## Bloques (una rama `fase-*` y una PR a `dev` cada uno)

### A. CI: evidencias, límites y velocidad
Archivos: `.github/workflows/build.yml`, `gradle.properties`, `scripts/`.
1. **Informes como artefactos**: subir con `if: always()` y `retention-days: 14` un artefacto `reports-<versión>` con `app/build/reports/tests/`, `app/build/test-results/`, `app/build/reports/lint-results-debug.{html,sarif,xml}`. Hoy, si un test falla, hay que leer el log.
2. **Resumen en el job** (`$GITHUB_STEP_SUMMARY`): un script `scripts/test-summary.py` lee los XML de resultados y escribe tests totales/fallos/omitidos y los nombres de los que fallan; el lint, como recuento de avisos por regla.
3. **`concurrency`**: grupo por PR o por rama; `cancel-in-progress` solo para PR y `fase-*`, **nunca para `main` ni `dev`** (el guardia y la release deben terminar).
4. **`timeout-minutes`**: 25 en `build`, 5 en `guard`, 10 en `release`.
5. **Caché de Robolectric**: `actions/cache` sobre `~/.m2/repository/org/robolectric` (~200 MB que hoy se descargan en cada ejecución), con la versión de Robolectric del catálogo en la clave.
6. **Rendimiento de Gradle**: `org.gradle.parallel=true`, `org.gradle.configuration-cache=true` y subir `-Xmx` a 3 GB; **solo se adoptan si** tests, lint y APK pasan en local con la configuration cache y se ve "Configuration cache entry reused". Si algún plugin es incompatible, se deja solo `parallel`.
6 bis. **`--continue`** en la invocación de Gradle, para que el lint se ejecute aunque falle un test (descubierto en la prueba negativa).
Verificación: ejecución en verde; y una **prueba negativa** en una rama desechable con un test que falla a propósito, para comprobar que se suben los informes y que el resumen lista el test.

### B. Calidad de código: formato y lint estricto
Archivos: `.editorconfig`, `gradle/libs.versions.toml`, `build.gradle.kts`/`app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/`, `app/lint.xml`.
**Nota de ejecución:** Spotless no lee `ktlint_code_style` del `.editorconfig` (sí `max_line_length`), así que el estilo va en `build.gradle.kts` con `editorConfigOverride`; con `ktlint_official` el reformateo era de ~760 líneas y con `android_studio` más las comas finales, de ~65.
1. **`.editorconfig`** con `ktlint_code_style = android_studio`, indentación de 4, y ajustes para Compose (`ktlint_function_naming_ignore_when_annotated_with = Composable`). El `max_line_length` se decide viendo cuántas infracciones da el código actual, para minimizar el ruido.
2. **Spotless** con `ktlint` (1.8.0) sobre `**/*.kt` y `*.kts`: tareas `spotlessCheck` (en CI, en la misma invocación de Gradle) y `spotlessApply` (local).
3. **Un único commit `style:`** con el reformateo del código existente, separado de los demás para que el diff sea revisable y no suba versión.
4. **Lint estricto**: `warningsAsErrors = true` y `lint.xml`. De los 6 avisos actuales: `DataExtractionRules` se **arregla** (`android:dataExtractionRules` excluyendo todo, coherente con `allowBackup="false"`); `InlinedApi` se **suprime con comentario** en `AppPermission` (son constantes de cadena, ya condicionadas por `sinceSdk`); `OldTargetApi` y `ReportShortcutUsage` se **documentan como aceptados** en `lint.xml` (subir `targetSdk` exige revisar cambios de comportamiento; `reportShortcutUsed` es una mejora de ordenación de atajos, no un fallo).
Verificación: `./gradlew spotlessCheck lintDebug` limpio en local; prueba negativa con un archivo mal formateado en una rama desechable (la CI debe fallar con un mensaje claro).

### C. Cobertura con Jacoco
Archivos: `app/build.gradle.kts`, `scripts/coverage-summary.py`, `.github/workflows/build.yml`.
1. Activar `enableUnitTestCoverage` en `debug` y fijar `testCoverage { jacocoVersion = "0.8.15" }` (compatible con Java 21). Tarea: `createDebugUnitTestCoverageReport`.
2. **Compatibilidad con Robolectric**: `isIncludeNoLocationClasses = true` y `excludes = ["jdk.internal.*"]` en las tareas de test; sin esto, Jacoco da cobertura vacía o errores con Robolectric y JDK 21.
3. **Exclusiones** del informe: `BuildConfig`, `R`, `ComposableSingletons*`, y `RouteMap` (osmdroid sin test, ya documentado como hueco).
4. `scripts/coverage-summary.py` extrae del XML el porcentaje de líneas/ramas, por paquete y total, lo escribe en el resumen del job y sube el HTML con los demás informes.
**Nota de ejecución:** medido 87,1 % de líneas y 74 % de ramas (80 % de líneas con `RouteMap`); el umbral se fijó ya en el mismo bloque, 85 % en `app/coverage-min.txt`.
5. **Sin umbral al principio.** Con el valor medido (se espera por encima del 60 % en la lógica y bastante menos en UI), se añade en un paso posterior un umbral de tipo trinquete (el mínimo sube y nunca baja) en el propio script, con margen de 2 puntos.
Verificación: informe generado en local, con cifras coherentes con lo que sabemos (la lógica pura casi completa, `RouteMap` excluido); mismo resultado en CI.

### D. Colaboración: Dependabot, plantilla de PR y hook `commit-msg`
Archivos: `.github/dependabot.yml`, `.github/pull_request_template.md`, `.githooks/commit-msg`, `scripts/conventional-commits.sh` (nuevo), `scripts/compute-version.sh`, `scripts/test-commit-msg-hook.sh` (nuevo).
1. **Dependabot**: ecosistemas `gradle` (lee `libs.versions.toml` y el wrapper) y `github-actions`; semanal; `target-branch: dev`; `open-pull-requests-limit: 5`; `commit-message.prefix: build(deps)` / `ci(deps)`; **grupos** (`androidx`, `kotlin`, `test`); **ignorar actualizaciones mayores de AGP y Kotlin**, que se suben a mano porque van acopladas (compilador de Compose, KSP si lo hubiera, matriz de compatibilidad). Aviso: al activarlo abre varias PR a la vez; se van fusionando una a una por el flujo normal.
2. **Plantilla de PR** en español: qué cambia y por qué, tipo de commits y si sube versión, verificación (tests locales, CI, probado en dispositivo) y notas de ruptura.
3. **Hook `commit-msg`**: acepta el formato convencional `tipo(ámbito)!: descripción` con los mismos tipos que el versionado, y deja pasar `Merge …`, `Revert "…"` y `fixup!`; rechaza el resto con un mensaje que explica el formato. Se salta con `--no-verify`.
4. **Una sola fuente de verdad**: el patrón de tipos y la regla de bump pasan a `scripts/conventional-commits.sh`, que cargan `compute-version.sh` y el hook (hoy `compute-version.sh` lo lleva escrito). Las 33 comprobaciones de `test-compute-version.sh` son la red de seguridad del refactor.
5. **Aviso en CI** (no bloqueante) en las PR: lista en el resumen los commits que no siguen el formato, porque de ellos depende la versión.
**Nota de ejecución:** al probar `check-commit-format.sh` se vio que con un rango inválido salía con 0 en silencio (el error de `git log` se perdía dentro de la sustitución de procesos); está corregido y con test.
Verificación: `bash scripts/test-commit-msg-hook.sh` (casos válidos, inválidos, merge, revert, fixup) y los demás tests de scripts en verde; abrir la PR del propio bloque D con un commit mal formado para ver el aviso; confirmar que Dependabot aparece en la pestaña Insights.

## Documentación (en cada PR, solo los documentos vivos)
- `docs/estado.md`: sección CI y Próximos pasos al día, y los comandos nuevos (`spotlessApply`, informe de cobertura).
- `docs/decisiones.md`: decisiones de este lote (ktlint frente a detekt y por qué, política de cobertura, Dependabot, lint estricto).
- `AGENTS.md`: comandos `spotlessApply`/`spotlessCheck` y regla del hook. `docs/archivo/` no se toca.

## Riesgos
- **Jacoco + Robolectric + JDK 21**: puede dar informes vacíos o errores de instrumentación. Mitigación: probarlo en local (ya hay entorno) antes de subirlo, con las exclusiones indicadas.
- **Configuration cache con AGP 9.4 y Robolectric**: puede fallar. Mitigación: es opcional dentro del bloque A.
- **Reformateo grande** (B): se aísla en su propio commit y se afina `.editorconfig` antes para reducir el diff.
- **Aumento del tiempo de CI** (Jacoco y Spotless): se espera +10-20 %; lo compensa la caché de Robolectric.
- **Dependabot abre PR a la vez**: límite de 5 y agrupación.
- **Lint estricto**: un aviso nuevo de una versión de AGP/AndroidX puede fallar la CI sin que el código cambie; se resuelve en esa PR, no se desactiva.

## Fuera de alcance
Fijar acciones por SHA, subir SARIF a code scanning (requiere GitHub Advanced Security en repos privados), tests instrumentados o de capturas, R8 y E2E (lote 4), y migrar a detekt hasta que su línea 2.x sea estable.

## Verificación global
- Cada PR: CI en verde con el APK verificado por `aapt2`, y la prueba negativa que corresponda (test que falla → informes subidos; archivo mal formateado → CI roja; commit mal formado → aviso).
- Al terminar: los informes de tests, lint y cobertura se pueden descargar de cada ejecución, un push a una PR cancela la ejecución anterior, `./gradlew spotlessCheck lintDebug testDebugUnitTest` pasa en local, y `dev` queda con el guardia en verde tras cada fusión.
