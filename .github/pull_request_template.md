## Qué cambia y por qué
<!-- Resumen corto y el motivo. Enlaza el plan o el documento de decisiones si lo hay. -->

## Commits y versión
<!-- Los commits siguen el formato convencional (tipo en inglés, descripción en español).
     Di si la PR sube la versión: feat = minor, fix/perf = patch, `!` o BREAKING CHANGE: = major.
     Recuerda: se fusiona con merge commit, nunca squash. -->

## Verificación
- [ ] `./gradlew spotlessCheck testDebugUnitTest lintDebug` en verde en local
- [ ] Tests de scripts (`bash scripts/test-*.sh`) si se tocaron scripts
- [ ] CI en verde
- [ ] Probado en un dispositivo (si cambia comportamiento visible)

## Cambios incompatibles o notas
<!-- Migraciones, permisos nuevos, cambios de configuración. Vacío si no hay. -->
