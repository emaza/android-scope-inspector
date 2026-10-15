# Estrategia de tests

Resultado del lote 2 de la [hoja de ruta (010)](010-analisis_de_tests_y_entorno.md), siguiendo la skill `testing-setup` del plugin `android-skills`. Entorno para ejecutarlos: [011](011-entorno_de_desarrollo_local.md).

## Resumen

| | Antes (lote 0) | Ahora |
|---|---|---|
| Tests JVM/Robolectric | 41 | **165** (0 fallos) |
| Tests de scripts (bash) | 50 | 50 |
| Tests de UI, navegación y textos | 0 | 66 |
| Tiempo en local (caché caliente) | — | ~50 s la suite completa |
| Mutaciones detectadas | — | 8 de 8 |

## Capas

| Capa | Qué prueba | Herramientas | Dónde |
|---|---|---|---|
| **Lógica pura** | Cálculos, formatos, estados de permisos, reglas de atajos, plan de solicitud de permisos | JUnit 4 | `RouteMathTest`, `PermissionStateTest`, `PermissionRequestPlanTest`, `ShortcutPolicyTest`, `AppPermissionTest`, `TileUsagePolicyTest`, `DemoModuleTest` |
| **ViewModel** | `LocationViewModel`: ubicación puntual, rastreo, filtrado, cronómetro, fallos | JUnit 4, `kotlinx-coroutines-test`, `FakeLocationSource`, `FakeElapsedClock`, `MainDispatcherRule` | `LocationViewModelTest` |
| **UI (comportamiento)** | Pantallas completas: qué se ve y qué ocurre al pulsar | Robolectric + Compose Test (`createAndroidComposeRule`) | `HomeScreenTest`, `ModuleScreenTest`, `LocationScreenTest`, `PermissionStatusListTest`, `VersionFooterTest` |
| **Navegación** | Rutas, "atrás", deep links y atajos (en frío y con la app abierta) | `TestNavHostController` | `AppNavGraphTest` |
| **Recursos** | Textos reales: no vacíos, sin duplicados, etiquetas de atajos cortas | Robolectric | `ModuleTextsTest` |
| **Scripts** | Versionado, guardia de PR, hook `pre-push` | bash con repos git temporales, `gh` falso y stdin simulado | `scripts/test-*.sh` |

No hay `androidTest` (instrumentados), capturas de pantalla ni E2E: ver [Huecos conocidos](#huecos-conocidos).

## Cómo ejecutarlos

```
source scripts/dev-env.sh

./gradlew testDebugUnitTest                              # toda la suite JVM + Robolectric
./gradlew testDebugUnitTest --tests '*ModuleScreenTest'  # una clase
./gradlew testDebugUnitTest --tests '*LocationViewModelTest.startsEmptyAndIdle'  # un test

bash scripts/test-compute-version.sh                     # versionado (33 comprobaciones)
bash scripts/test-check-pr-merge.sh                      # guardia de PR (8)
bash scripts/test-pre-push-hook.sh                       # hook pre-push (9)
```

Informes: `app/build/reports/tests/testDebugUnitTest/index.html` y los XML en `app/build/test-results/`. La CI ejecuta lo mismo en cada push y en cada PR.

## Requisitos de Robolectric

- **JDK 21**: Robolectric exige Java 21 para Android SDK 36 y la API 37 (el entorno y la CI usan 21; el bytecode de la app sigue en 17).
- `testOptions.unitTests.isIncludeAndroidResources = true`.
- Opciones de JVM en `app/build.gradle.kts` para que acceda a internos del JDK: `--add-exports` y `--add-opens` de `java.base/jdk.internal.access`.
- `ui-test-manifest` como `debugImplementation` (declara la `ComponentActivity` de los tests).
- La primera ejecución descarga el `android-all` de Robolectric (~200 MB para una versión de Android) en `~/.m2`.
- Los tests de pantalla usan `@Config(qualifiers = "w411dp-h3000dp")` para que la `LazyColumn` componga todas las tarjetas.
- Timeout de 5 minutos por tarea de test (`unitTests.all`), para que un test colgado no bloquee la CI, y `Timeout` de 30 s en los tests del ViewModel.

## Patrones del proyecto

- **Dependencias sustituibles, sin framework de DI** (decisión de [010](010-analisis_de_tests_y_entorno.md)): interfaz + constructor + fábrica manual. `LocationViewModel(source: LocationSource, clock: ElapsedClock)` con `LocationViewModel.factory(context)`; el fake vive en el código de test (`FakeLocationSource`).
- **Lógica pura aparte de Android**: `RouteMath`, `ShortcutPolicy`, `planPermissionRequest`, `resolvePermissionState`… se prueban sin Robolectric. Lo que depende de `Activity` solo delega en ellas.
- **Pantallas con puntos de sustitución**: `LocationScreen` recibe el mapa como parámetro (`mapContent`) para no cargar un `MapView` real de osmdroid en los tests.
- **Coroutines**: `MainDispatcherRule` sustituye `Dispatchers.Main`; `runTest` comparte su planificador y se avanza el tiempo con `advanceTimeBy` + `runCurrent`. Los bucles infinitos con `delay` (el cronómetro) se detienen al acabar el test (`runVmTest`), porque `runTest` espera a que el planificador se vacíe.
- **Permisos en Robolectric**: se conceden con `shadowOf(application).grantPermissions(...)`; el rationale con `shadowOf(packageManager).setShouldShowRequestPermissionRationale(...)`; la solicitud al sistema se comprueba con `shadowOf(activity).lastRequestedPermission`, y el resultado se entrega con `activity.onRequestPermissionsResult(...)`. La denegación permanente se simula marcando el permiso como "ya pedido" (`Context.markRequested`).
- **Selectores**: se busca por texto leído de los recursos (`activity.getString(R.string...)`), no por literales, para que el cambio de un texto no rompa tests.
- **Cada test, una idea y un nombre que dice qué debe ocurrir.** Los tests de un fallo se escriben antes de arreglarlo y deben fallar con el código anterior.

## Fallos reales encontrados por los tests

| Fallo | Cómo se vio | Arreglo |
|---|---|---|
| `formatDistance(999.6)` → `"1000 m"` en lugar de `"1.00 km"`; negativos → `-5 m`; `NaN` → `NaN km/h`; duración negativa → `00:-5` | Tests de borde | La unidad se decide con el valor redondeado; valores no válidos → 0 |
| El cronómetro seguía contando tras un fallo del flujo de ubicación (p. ej. permiso revocado a mitad del rastreo) | `LocationViewModelTest` (y un test colgado por el cronómetro huérfano) | Se cancela al fallar, y se crea antes que la recogida |
| El atajo con la app ya abierta **no navegaba**: con las flags de un atajo, `handleDeepLink` intenta relanzar la tarea | `AppNavGraphTest` | Resolución propia de la ruta (`ShortcutPolicy.moduleRoute`), con Home debajo y sin duplicar |

## Comprobación de mutaciones

Para saber si los tests detectan errores de verdad, se introdujeron a mano 8 errores en el código de producción y se comprobó que la suite falla en cada uno (después se restauró el código):

| Error introducido | Tests que lo detectan |
|---|---|
| `USE_BIOMETRIC` deja de ser permiso "concedido al instalar" | 5 |
| Nunca se muestra el rationale | 5 |
| El atajo apila el módulo en vez de reemplazarlo | 1 |
| El botón de permisos se muestra siempre | 3 |
| El rastreo no filtra las lecturas imprecisas | 4 |
| Se pide el permiso aunque ya esté concedido | 8 |
| Un atajo acepta cualquier ruta (sin lista de módulos conocidos) | 3 |
| Un permiso bloqueado se vuelve a pedir | 5 |

Ninguna mutación sobrevivió. Repetirlo tras cambios grandes en permisos, navegación o ubicación.

## Huecos conocidos

- **El mapa real (`RouteMap`, osmdroid)** no se prueba: en los tests se sustituye. Se verifica a mano en el móvil.
- **`AppShortcuts`** (publicar los atajos y pedir el icono a Android) solo se prueba en su lógica (`ShortcutPolicy`); la llamada a `ShortcutManagerCompat` no.
- **`PermissionGroupState` con el lanzador real** se prueba a través de las pantallas con Robolectric, no con el sistema real de permisos del dispositivo.
- **Sin tests instrumentados, de capturas de pantalla ni E2E.** Las capturas con AGP 9.4.1 usarían el plugin antiguo (la skill propone el nuevo con AGP 9.5.0-alpha03 o superior, y no se actualiza AGP solo por eso).
- **Sin medición de cobertura** (Jacoco): pendiente del lote 3, junto con los umbrales.
- **Estado de la rotación y recreación de la actividad** no se verifica.

## Cómo añadir tests nuevos

1. Lógica sin Android → función pura + test JUnit. Es lo más barato y lo que más se mantiene.
2. Algo que depende de un servicio del sistema → interfaz en producción + fake en test, y fábrica manual.
3. Una pantalla → test de comportamiento con `createAndroidComposeRule<ComponentActivity>()`; sustituir lo pesado (mapas, cámara) por un parámetro.
4. Un fallo → primero el test que lo reproduce y falla, luego el arreglo.
5. Un módulo nuevo (cámara, biometría, segundo plano) → su fake propio, su ViewModel inyectable y su test de pantalla con el flujo de permisos de `ModuleScreenTest` como modelo.
