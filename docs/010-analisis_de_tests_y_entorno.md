# Análisis de tests, entorno de desarrollo y automatización

Análisis del proyecto hecho con las skills del plugin `android-skills` (`testing-setup`, `android-cli`, `android-intent-security` y `android-permissions-security`). Estado en el momento del análisis: `dev` en el commit `c7517c3`, versión `0.2.1-dev`, release `v0.2.0` publicada. Relacionado: [flujo de ramas y PR (009)](009-flujo_de_ramas_y_pr.md) y [versionado (007)](007-versionado_semantico.md).

## 1. Estado de testing

Siguiendo el inventario del paso 1 de la skill `testing-setup`:

| Aspecto | Hoy |
|---|---|
| App | 100 % Compose |
| Tests locales | JUnit 4.13.2: **41 tests en 6 archivos**, todos sobre lógica pura |
| Tests de scripts | 50 comprobaciones en bash (versionado 33, guardia de PR 8, hook 9) |
| DI, mocks, Robolectric | Ninguno |
| Tests de UI (Compose) | Ninguno |
| Tests de navegación / deep links | Ninguno |
| Capturas de pantalla | Ninguna |
| `androidTest` / E2E | No existe el directorio |
| Cobertura (Jacoco) | No se mide |
| Wrapper de Gradle | Falta; `./gradlew` no existe |

Tests por archivo: `RouteMathTest` 10, `PermissionStateTest` 9, `AppPermissionTest` 8, `ShortcutPolicyTest` 7, `DemoModuleTest` 4, `TileUsagePolicyTest` 3.

## 2. Calidad de los tests existentes

**Lo que está bien:** nombres descriptivos, una idea por test, `Locale` fijo para que sean deterministas, y tests de los scripts con un `gh` falso y repos git temporales. La CI los ejecuta siempre.

**Problemas:**

1. **Hay un fallo que los tests no detectan.** `formatDistance(999.6)` devuelve `"1000 m"` en lugar de `"1.00 km"`, porque redondea a 1000 antes de cambiar de unidad (comprobado calculando los bordes: 999,4 → `999 m`; 999,5 a 999,99 → `1000 m`; 1000 → `1.00 km`). El test solo prueba 42,4 y 1250. Faltan casos de borde (999,5; 1000), de `NaN` y de segundos negativos en `formatDuration`, que con `%02d` daría `00:-5`.
2. **`thereAreAtMostFourModuleShortcuts` es frágil.** Afirma `DemoModule.entries.size <= 4`, una convención de los lanzadores, no una regla del código. Fallará al añadir el quinto módulo, aunque `AppShortcuts` ya recorta con `take(max)`. Debería probar ese recorte.
3. **`moduleDescriptionTextsAreDistinct` tiene poco valor.** Compara ids de recursos, que son distintos por construcción; solo detecta un copia-pega del mismo id. Lo útil sería comprobar que los textos no están vacíos.
4. **Los tests acoplan constantes.** `AppPermissionTest` repite el `minSdk = 26` a mano.

## 3. Cobertura de casos de uso

| Caso de uso | Lógica pura | Orquestación / UI |
|---|---|---|
| Permisos: estados y por versión de Android | Probada | No probada |
| Permisos: rationale, denegación permanente → Ajustes | Sin extraer, no probable | `PermissionGroupState.request()` sin tests |
| Ubicación: cálculo de ruta y formatos | Probada | — |
| Ubicación: acumular ruta, filtrar lecturas imprecisas, errores | — | **`LocationViewModel` sin tests y no se puede probar tal cual** |
| Accesos directos | Política probada | Registro, oferta única y deep links sin probar |
| Pantallas (Home, módulo, pie de versión, tarjeta de atajos) | — | Sin probar |
| Versionado, guardia de PR, hook | Muy bien cubiertos | CI lo verifica con `aapt2` |

El `LocationViewModel` crea su `LocationRepository` (Play Services) por dentro y usa `SystemClock`. La skill pide tests para todo ViewModel, pero primero hay que **extraer una interfaz y un reloj** para poder inyectar fakes. Como referencia, los archivos con algún test son una cuarta parte de los de producción (6 de 22); la lógica con ramas (ViewModel y `PermissionGroupState`) no tiene ninguno. No hay medición real de cobertura.

## 4. Entorno de desarrollo y automatización

- **Sin entorno local.** No hay Java, SDK ni `adb`, así que cada comprobación cuesta 4-6 minutos de CI. La máquina tiene 8 núcleos, 15 GB de RAM (unos 3 libres en el momento del análisis), 288 GB de disco libres y `/dev/kvm`.
- **Falta el wrapper** y no hay formato ni análisis estático (`.editorconfig`, ktlint o detekt).
- **Faltan automatizaciones comunes:** Dependabot para Gradle y Actions, plantilla de PR, `AGENTS.md` y `docs/testing.md`, y una validación local del formato de los commits.
- **CI mejorable:**
  - No hay `concurrency` (las ejecuciones se acumulan) ni `timeout-minutes`.
  - No se suben los informes de tests y lint como artefactos, así que cuando algo falla hay que leer el log.
  - **`ubuntu-latest` migra a Ubuntu 26 el 19 de octubre de 2026.** Se fija `ubuntu-24.04` para que no se rompa por sorpresa (hecho junto a este documento).
- **Gradle:** podría activarse `org.gradle.parallel` y la configuration cache.

## 5. Seguridad (skills de intents y permisos)

- **Permisos: bien alineados.** Se consulta el estado siempre con `checkSelfPermission` sin cachearlo, se sigue el flujo de tres estados con salida a Ajustes, no se pide ubicación en segundo plano y `allowBackup` está desactivado.
- **Corrección de documentación.** El doc [008](008-accesos_directos.md) decía que "otras apps no pueden abrir un módulo por URL". Es falso: `MainActivity` está exportada y Navigation resuelve el deep link de cualquier intent explícito que reciba. No se ha probado en un dispositivo. El riesgo es bajo, porque solo abre pantallas de la demo. Regla a respetar: los módulos sensibles, como el de la nota biométrica, no deben depender de llegar por navegación. El 008 ya está corregido.
- **Para los módulos que vienen:** los `PendingIntent` de notificaciones deben ser `FLAG_IMMUTABLE`. El de Activity Recognition necesita ser mutable, pero con componente explícito.

## 6. Decisiones tomadas

| Pregunta | Decisión | Por qué |
|---|---|---|
| Entorno local | **JDK 17 + Android CLI + SDK, sin Android Studio, sin sudo** (opción "build y tests") | Da casi todo el beneficio: build, tests y lint en 1-2 min. El emulador añade 3-4 GB y RAM y se comporta distinto del móvil real en permisos, atajos, huella y GPS. Se puede añadir después, o conectar el móvil por USB, que es más barato |
| Inyección de dependencias | **Fábricas manuales** (interfaz + constructor + factory), sin Hilt | La app es pequeña y solo un ViewModel necesita una dependencia sustituible; Hilt añade KSP, plugin y configuración de tests. La skill propone Hilt por defecto para apps sin DI: es un criterio propio, y migrar después es fácil porque las interfaces serían las mismas |
| Primer lote | **Testabilidad y tests** primero | Es lo que más protege la app y ya hay un fallo real sin cubrir. Jacoco y los umbrales solo aportan con más tests. Se hace ya la parte urgente de CI: fijar el runner |

## 7. Hoja de ruta

Cada lote va por su rama `fase-*` y su PR a `dev`, con commits convencionales.

| Lote | Contenido | Estado |
|---|---|---|
| **0** | Este documento, corrección del 008 y runner fijado a `ubuntu-24.04` | **Hecho** (PR #4) |
| **1** | Entorno local: JDK 17 en `~/.local`, `android` CLI y SDK, wrapper de Gradle; la CI pasa a usar `./gradlew` | **Hecho**, ver [011](011-entorno_de_desarrollo_local.md) |
| **2** | Testabilidad y tests, ver abajo | Pendiente |
| **3** | Automatización: Jacoco e informes como artefactos, `concurrency` y `timeout`, Dependabot, ktlint o detekt con `.editorconfig`, hook `commit-msg`, plantilla de PR, `docs/testing.md` y `AGENTS.md` | Pendiente |
| **4** | A valorar: capturas de pantalla, E2E mínimo con emulador, R8 | Pendiente |

### Lote 2 en detalle

1. **Arreglar `formatDistance`** y añadir los casos de borde (999,5; 1000; `NaN`; segundos negativos).
2. **Testabilidad:**
   - `LocationSource`: interfaz para el acceso a ubicación, con la implementación de Play Services y un fake en tests.
   - Un reloj inyectable en lugar de `SystemClock`.
   - Una fábrica manual para el `LocationViewModel`.
3. **Tests del ViewModel** con `kotlinx-coroutines-test`: acumular ruta, filtrar lecturas imprecisas, iniciar y detener el rastreo, ubicación desactivada, errores.
4. **Flujo de permisos:** extraer la decisión de `PermissionGroupState.request()` (lanzar, mostrar rationale o ir a Ajustes) a una función pura y probarla.
5. **Sustituir los tests frágiles:** el recorte de atajos en `AppShortcuts` y los textos vacíos en los módulos.
6. **Tests de UI con Robolectric y Compose** (Home, módulo, estado de permisos, pie de versión, `LocationScreen` por estados) y **de navegación** (deep links y "atrás" con `TestNavHostController`).
7. Documentar la estrategia en `docs/testing.md` y enlazarla desde un `AGENTS.md` (se hace cuando haya comandos y tests reales que describir).

**Riesgos conocidos:**
- No se sabe si la versión de Robolectric disponible soporta la API 37 del proyecto; si no, los tests de UI usarían el SDK que sí soporte.
- Las capturas de pantalla con AGP 9.4.1 usarían el plugin antiguo, que la skill marca como obsoleto; no se actualiza AGP solo por eso.
- Con el lote 1 hecho, los tests y el lint se pasan en local antes de subir; la CI sigue siendo la verificación final y la que valida el APK con `aapt2`.
