# AGENTS.md

Guía para agentes y colaboradores. Solo lo imprescindible; el resto está en `docs/`.

## Documentación

- Estado actual: `docs/estado.md` (léelo al retomar trabajo).
- Decisiones vigentes: `docs/decisiones.md`.
- `docs/archivo/` contiene histórico detallado. NO lo leas ni busques en él salvo que te lo pida explícitamente.

## Qué es

Android Scope Inspector: app demo (Kotlin + Jetpack Compose) con cuatro módulos que muestran de forma práctica qué se puede hacer con cada permiso de Android: ubicación y actividad, cámara y vídeo, biometría, segundo plano y notificaciones. Cada módulo debe ser un uso real del permiso, con algo que el usuario pueda ver y comprobar, no solo texto. Qué módulos están hechos y cuáles faltan: `docs/estado.md`.

## Entorno

```
source scripts/dev-env.sh     # JDK 21 y Android SDK del HOME del usuario; no toca archivos de perfil
./gradlew spotlessApply       # formatea el código con ktlint (la CI exige spotlessCheck)
./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug
./gradlew createDebugUnitTestCoverageReport   # cobertura: app/build/reports/coverage/test/debug/index.html
./gradlew --stop              # libera la memoria del daemon al terminar
```

Si falta el entorno, o para el uso de `adb` y el `android` CLI: `docs/estado.md`, sección Entorno. Sin él, la verificación es la CI (2-6 min por comprobación).

## Tests

Antes de subir, `./gradlew spotlessCheck testDebugUnitTest lintDebug` debe estar en verde (el lint falla con cualquier aviso nuevo); los tests de scripts son `bash scripts/test-*.sh`.

- Un fallo se arregla con un test que lo reproduce y falla antes del arreglo.
- Lo que depende de un servicio del sistema se inyecta (interfaz + fake + fábrica manual); no hay framework de DI.
- La lógica va en funciones puras fuera de Android. Robolectric exige JDK 21.
- La CI falla si la cobertura de líneas baja del mínimo de `app/coverage-min.txt` (trinquete: solo se sube; resumen con `python3 scripts/coverage-summary.py --min-file app/coverage-min.txt`).

## Ramas, commits y versión

- **`main` y `dev` solo cambian mediante pull request**, fusionada con **merge commit** (nunca squash: el versionado lee los mensajes). Trabajo en `fase-*`, siempre desde `dev`.
- El repo es privado y GitHub no puede bloquear el push directo: lo detectan el job `guard` de CI y el hook `pre-push` (`git config core.hooksPath .githooks`).
- **Commits convencionales** (`feat:`, `fix:`, `perf:`, `docs:`, `ci:`, `test:`, `chore:`, `build:`, `refactor:`), tipo en inglés y descripción en español. `feat` sube minor, `fix`/`perf` patch, `!` o `BREAKING CHANGE:` major; el resto no sube versión.
- Al final de cada commit, el `Co-Authored-By` del agente que lo escribió.
- Detalle del flujo y del versionado: `docs/decisiones.md`.

## Cosas que ya han dado problemas

- `applicationId` es `io.github.emaza.scopeinspector`; el `namespace` del código sigue siendo `com.example.scopeinspector`.
- El mapa usa OpenStreetMap: User-Agent propio y atribución visible, o el servidor sirve "403 Access blocked".
- Los atajos solo salen en el icono real de la app, no en el acceso directo que crea el diálogo de Android.
- `MainActivity` está exportada y acepta intents explícitos: los módulos sensibles no deben depender de llegar por navegación.
- El APK de la release va firmado con la clave de debug: vale para instalar a mano, no para Google Play.
- Con el móvil conectado por USB: `adb` solo sobre esta app, sin capturas de pantalla salvo petición, y restaurar al terminar todo lo que se cambie.
