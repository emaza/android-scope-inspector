# AGENTS.md

Guía para agentes y colaboradores. Detalles en `docs/`; aquí solo lo imprescindible.

## Qué es

Android Scope Inspector: app demo (Kotlin + Jetpack Compose) con cuatro módulos que muestran de forma práctica qué se puede hacer con cada permiso de Android: ubicación y actividad, cámara y vídeo, biometría, segundo plano y notificaciones. Plan de fases: `docs/004-plan_de_fases_detallado.md`.

## Entorno

```
source scripts/dev-env.sh     # JDK 21 y Android SDK del HOME del usuario; no toca archivos de perfil
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew --stop              # libera la memoria del daemon al terminar
```

Instalación y verificación: `docs/011-entorno_de_desarrollo_local.md`. Sin ese entorno, la verificación es la CI (4-6 min por comprobación).

## Tests

Antes de subir, `./gradlew testDebugUnitTest` debe estar en verde. Estrategia, patrones y huecos conocidos: **`docs/012-estrategia_de_tests.md`**. Los tests de scripts: `bash scripts/test-*.sh`.

- Un fallo se arregla con un test que lo reproduce y falla antes del arreglo.
- Lo que depende de un servicio del sistema se inyecta (interfaz + fake + fábrica manual); no hay framework de DI.
- Robolectric exige JDK 21.

## Ramas, commits y versión

Flujo completo en `docs/009-flujo_de_ramas_y_pr.md`:

- **`main` y `dev` solo cambian mediante pull request**, fusionada con **merge commit** (nunca squash: el versionado lee los mensajes). Trabajo en `fase-*`, siempre desde `dev`.
- El repo es privado y GitHub no puede bloquear el push directo: lo detectan un job `guard` en CI y el hook `pre-push` (`git config core.hooksPath .githooks`).
- **Commits convencionales** (`feat:`, `fix:`, `perf:`, `docs:`, `ci:`, `test:`, `chore:`, `build:`, `refactor:`), tipo en inglés y descripción en español. `feat` sube minor, `fix`/`perf` patch, `!` o `BREAKING CHANGE:` major. La versión sale de ellos (`scripts/compute-version.sh`, `docs/007-versionado_semantico.md`).
- Al final de cada commit: `Co-Authored-By` del agente que lo escribió.

## Cosas que ya han dado problemas

- `applicationId` es `io.github.emaza.scopeinspector`; el `namespace` del código sigue siendo `com.example.scopeinspector`.
- El mapa usa OpenStreetMap: hace falta un User-Agent propio y la atribución visible (`docs/005`), o el servidor sirve "403 Access blocked".
- Los atajos solo salen en el icono real de la app, no en el acceso directo que crea el diálogo de Android (`docs/008`).
- `MainActivity` está exportada y acepta intents explícitos: los módulos sensibles no deben depender de llegar por navegación (`docs/010`).
- El APK de la release va firmado con la clave de debug: vale para instalar a mano, no para Google Play.
