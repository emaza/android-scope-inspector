# Accesos directos

La app ofrece dos tipos de acceso directo: un **icono en la pantalla de inicio** y **atajos a los 4 módulos** al mantener pulsado el icono de la app.

## Por qué no se crea "al instalar"

Android no permite que una app ejecute nada en el momento de la instalación. El broadcast `INSTALL_SHORTCUT`, que antes servía para crear iconos, se eliminó en Android 8 y no existe ningún permiso equivalente. Lo más cercano es pedir el acceso directo la **primera vez que se abre la app**: Android muestra su propio diálogo "¿Añadir a la pantalla de inicio?" y el usuario acepta o rechaza.

Se revisaron las 30 skills del plugin `android-skills` instalado: ninguna trata accesos directos de aplicación ni acciones al instalar. La más parecida, `glance-canonical-layouts`, fija un widget con `requestPinGlanceAppWidget` desde un botón, es decir, también en ejecución y con confirmación del usuario.

## Qué hace la app

### Icono en la pantalla de inicio

- Al abrir la app por primera vez se llama a `ShortcutManagerCompat.requestPinShortcut`. Solo se pide si el lanzador lo soporta, si no se ofreció antes y si no hay ya un acceso directo fijado con el mismo id.
- La marca "ya ofrecido" se guarda antes de pedirlo: si el usuario rechaza, no se le vuelve a preguntar. Para añadirlo después hay que hacerlo desde el lanzador del móvil.
- No necesita permisos nuevos.

### Atajos a los módulos

- Al mantener pulsado el icono de la app salen **Ubicación, Cámara, Biometría y Tareas**; cada uno abre su pantalla directamente y "atrás" lleva a la pantalla principal.
- Se publican como atajos dinámicos a partir de `DemoModule` en cada arranque, así que añadir un módulo nuevo lo añade también a los atajos (el límite habitual de los lanzadores es 4).
- Cada atajo lanza `scopeinspector://module/<ruta>` contra `MainActivity` con un intent explícito, y Navigation lo resuelve con un deep link. No hay `intent-filter`, así que el enlace no se puede abrir desde un navegador ni con un intent implícito. **Pero `MainActivity` está exportada y acepta intents explícitos: otra app podría abrir un módulo enviándole ese URI** (no probado en un dispositivo). El riesgo es bajo, porque solo abre pantallas de la demo; los módulos sensibles, como la nota biométrica, no deben depender de llegar por navegación (ver [010](010-analisis_de_tests_y_entorno.md)).
- `MainActivity` es `singleTask`: con la app ya abierta, el atajo llega como nuevo intent. Navigation no lo procesa por sí sola (se comprobó en el código compilado de `navigation-runtime` 2.10.2: no registra ningún `OnNewIntentListener`), así que `AppNavGraph` lo reenvía con `handleDeepLink`.

## Dónde se ven los atajos (comprobado en el móvil)

Android distingue dos clases de icono en la pantalla de inicio:

| Icono | Cómo se obtiene | ¿Muestra los 4 atajos al mantenerlo pulsado? |
|---|---|---|
| **Icono real de la app** | Está en el cajón de aplicaciones; lo añade el lanzador al instalar (si ese ajuste está activo) o se arrastra desde el cajón | **Sí**, también si se arrastra a la pantalla de inicio |
| **Acceso directo fijado** | Es el que crea el diálogo "Añadir a la pantalla de inicio" de la app | **No**: es solo un atajo que abre la app, sin menú |

Esto se detectó al probarlo: los atajos no salían al mantener pulsado el icono creado por el diálogo, pero sí en el icono del cajón y, al arrastrarlo a la pantalla de inicio, también allí. No era un fallo de la publicación de atajos.

Android no permite que una app coloque por sí sola el icono real en la pantalla de inicio, así que se decidió **mantener el diálogo** y **avisar en la app**: la pantalla principal incluye una tarjeta "Atajos a los módulos" que explica dónde pulsar y que el acceso directo del diálogo no muestra los atajos.

## Código

| Archivo | Función |
|---|---|
| `shortcuts/ShortcutPolicy.kt` | Reglas puras: cuándo ofrecer el icono, URI e id de cada atajo (con tests) |
| `shortcuts/AppShortcuts.kt` | Publica los atajos y pide el icono a Android |
| `navigation/DemoModule.kt` | Etiqueta corta e icono de cada módulo |
| `navigation/AppNavGraph.kt` | Deep links y reenvío de nuevos intents |
| `MainActivity.kt` | Llama a `AppShortcuts` en el primer arranque de la actividad |
| `res/drawable/ic_shortcut_*.xml` | Iconos de los 4 atajos |

## Cómo probarlo en el móvil

1. Desinstalar versiones anteriores e instalar el APK de la rama.
2. Abrir la app: debe salir el diálogo del sistema "Añadir a la pantalla de inicio". Al aceptar aparece un icono en el escritorio que abre la app (sin atajos al mantenerlo pulsado). Al cerrar y volver a abrir, el diálogo no reaparece.
3. Ir al **cajón de aplicaciones** y mantener pulsado el icono de Scope Inspector: salen los 4 atajos y cada uno abre su pantalla. Si se arrastra ese icono a la pantalla de inicio, los atajos también salen allí.
4. Con la app abierta en otro módulo, usar un atajo: navega al módulo elegido.
5. En la pantalla principal, al final de la lista, debe verse la tarjeta "Atajos a los módulos".

## Límites

- **Posible icono duplicado:** muchos lanzadores ya ponen el icono real de la app en el escritorio al instalar. El acceso directo fijado es independiente y podría haber dos iconos, uno con atajos y otro sin ellos. La app no puede detectarlo.
- **El usuario decide:** puede rechazar el diálogo, y los lanzadores sin soporte de accesos directos fijados no lo muestran.
- El icono de la app es un vector sin variante adaptable; los lanzadores lo muestran tal cual.
