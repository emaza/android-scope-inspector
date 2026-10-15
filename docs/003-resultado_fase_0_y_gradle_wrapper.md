# Resultado de la Fase 0 y explicación del Gradle Wrapper

## Resultado de la Fase 0

La Fase 0 está terminada: el build de CI en `dev` pasó en verde (5 min 31 s) y subió el APK debug.

**Resultado**
- **Compilación:** `testDebugUnitTest`, `lintDebug` y `assembleDebug` pasaron con Gradle 9.8.1, AGP 9.4.1 y Kotlin 2.4.21. Todavía no hay tests, así que el paso de tests no comprueba nada.
- **APK:** el artefacto `scope-inspector-debug-apk` pesa unos 11 MB. Lo bajas desde la pestaña Actions del run, o con `gh run download 38664117381 -n scope-inspector-debug-apk`. Para instalarlo en el teléfono hay que permitir orígenes desconocidos. Todavía no lo he probado en ningún dispositivo.
- **Rama:** `dev` está sincronizada con `origin/dev`, con 5 commits sobre el inicial.

**Qué falló por el camino**
- **`compileSdk`:** Compose 1.12.1 y core 1.19.1 exigen `compileSdk` 37 o superior. Lo subí a 37 y dejé `targetSdk` en 36.
- **Paquete del SDK:** el paquete de la plataforma se llama `platforms;android-37.0`, no `android-37`.
- **Push:** el push se bloqueó un rato porque el agente SSH había perdido la clave. Lo resolviste con `ssh-add`.

**Cosas a tener en cuenta**
- **Sin wrapper:** el proyecto sigue sin `gradlew`. CI lo compensa con `gradle-version: 9.8.1`, pero para abrirlo en Android Studio o compilar en local habrá que añadirlo más adelante.
- **Cambio de `ubuntu-latest`:** GitHub avisa de que `ubuntu-latest` pasa a Ubuntu 26 a partir del 19 de octubre de 2026. Si el build se rompe después de esa fecha, será por eso.
- **Sin merge a `main`:** `main` sigue en el commit inicial. El lanzamiento manual del workflow no estará disponible hasta que `build.yml` llegue a `main`.

La siguiente es la Fase 1 (tema, navegación, pantalla principal y el gestor de permisos reutilizable). ¿Empiezo?

---

## Sin wrapper: el proyecto sigue sin gradlew

### Qué es el Gradle Wrapper

Gradle es la herramienta que compila el proyecto. El **wrapper** es un conjunto de archivos que se guardan en el repo y que garantizan que todos compilen con la misma versión de Gradle, sin tenerla instalada:

- `gradlew` y `gradlew.bat`: scripts para Linux/macOS y Windows. Se ejecutan con `./gradlew assembleDebug`.
- `gradle/wrapper/gradle-wrapper.jar`: un programa pequeño que descarga la versión de Gradle indicada.
- `gradle/wrapper/gradle-wrapper.properties`: dice qué versión descargar.

Con el wrapper, cualquiera que clone el repo puede compilar, sin instalar Gradle y con la misma versión que los demás.

### Por qué no lo tiene este proyecto

Intenté bajar esos archivos del repositorio oficial de Gradle, pero el `.jar` no coincidía con el checksum que Gradle publica para ese release. No pude verificarlo, porque el jar oficial da 404. No quise commitear un binario sin verificar, así que lo dejé fuera.

### Cómo compila CI sin él

El workflow usa `gradle/actions/setup-gradle` con `gradle-version: 9.8.1`. La action descarga e instala esa versión en el runner, y el paso de build ejecuta `gradle ...` en vez de `./gradlew ...`. Por eso CI funciona.

### Qué limita en la práctica

- **Android Studio:** al abrir el proyecto espera encontrar el wrapper. Sin él, puede pedirte configurar la versión de Gradle a mano o ofrecerse a generarlo.
- **Compilar en local:** no puedes hacer `./gradlew assembleDebug` tras clonar el repo. Necesitarías tener Gradle instalado.
- **Versión de Gradle:** hoy está fijada solo en el workflow. Con el wrapper estaría fijada en el propio repo y valdría para todos los entornos.

Ahora mismo no te afecta, porque no tienes entorno local y todo se compila en CI. Se vuelve relevante si instalas Android Studio.

### Cómo añadirlo después

- **Desde Android Studio:** al abrir el proyecto, normalmente genera el wrapper o lo ofrece.
- **Con Gradle instalado:** `gradle wrapper --gradle-version 9.8.1`, y se commitean los archivos generados.
- **Desde CI:** un paso puntual que ejecute `gradle wrapper` y suba los archivos como artefacto, para commitearlos tú después.

Si quieres, lo dejo como tarea pendiente en el plan o preparo ese paso de CI.
