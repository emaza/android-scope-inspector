# android-scope-inspector

Android Scope Inspector es una app demo (Kotlin + Jetpack Compose) que enseña qué puede hacer y qué puede saber una aplicación Android. Es el equivalente móvil de [Browser Scope Inspector](https://github.com/emaza/browser-scope-inspector).

- **Inspector del dispositivo**: todo lo que cualquier app lee **sin pedir permisos** — hardware, sistema, pantalla, red, idioma y zona horaria, accesibilidad, identificadores y huella digital combinada, sensores en vivo y portapapeles. Cada sección explica para qué se puede usar ese dato.
- **Módulos con permisos**: ubicación y actividad, cámara y vídeo, biometría, segundo plano y notificaciones. Cada uno muestra el estado real de sus permisos y una demo del uso del permiso.

Todo se calcula en el propio móvil; nada sale del dispositivo (salvo los tiles del mapa de OpenStreetMap).

## Compilar

```
source scripts/dev-env.sh
./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug
```

El APK de cada release está en GitHub Releases. Estado del proyecto: [docs/estado.md](docs/estado.md).
