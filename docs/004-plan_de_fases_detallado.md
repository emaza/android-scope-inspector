# Plan de fases (detallado): cada módulo es una demo práctica y comprobable

## Context
La Fase 0 (esqueleto + CI) y la Fase 1 (tema, navegación, Home, gestor de permisos) están hechas en `fase-1`, con CI en verde. Hoy cada módulo es una pantalla genérica que solo pide permisos. El objetivo cambia: cada módulo debe **mostrar de forma práctica qué se puede hacer en el móvil con esos permisos** y dar al usuario **pruebas visibles de que funciona**, no solo el diálogo del sistema. Este plan redefine las Fases 1.1 a 6 con ese criterio. Se sustituye el plan anterior de "tarjeta con funcionalidad y ejemplo" (queda integrado en la Fase 1.1).

## Principios comunes a todos los módulos
1. **Resultado visible, no solo permiso:** cada demo produce algo que el usuario ve u oye (ruta en un mapa, vídeo reproducido, nota descifrada, notificación recibida).
2. **Evidencia en pantalla:** lecturas en vivo, estados y un registro de eventos corto ("14:02:11 permiso concedido", "tarea completada") para saber qué pasó.
3. **Degradación a la vista:** si se deniega un permiso, la demo sigue con una versión reducida y lo dice claramente (nunca pantalla bloqueada).
4. **Permiso en el momento de uso:** se pide al pulsar la acción, no al abrir el módulo.
5. **Ficha del módulo:** arriba, "Qué aporta" + "Ejemplo"; abajo, "Cómo comprobarlo" con 3-4 pasos para probar la demo en el móvil.
6. **Ejecución en local imposible:** se verifica con CI (tests unitarios + lint + APK) y el usuario prueba el APK en su teléfono.

## Fase 1.1 – Fichas de módulo (cierre de Fase 1, en `fase-1`)
- Commit previo de los cambios pendientes: permisos normales "Concedido al instalar" (`AppPermission.grantedAtInstall`).
- `DemoModule` gana tres textos: `featureRes` (qué aporta), `exampleRes` (ejemplo concreto) y `howToTestRes` (pasos de comprobación). Composable nuevo `ui/components/ModuleDescription.kt`, usado en la tarjeta de Home (qué aporta + ejemplo) y en `ModuleScreen` (los tres).
- Textos de "qué aporta / ejemplo" definidos en las secciones de cada fase.
- Test: los textos de cada módulo son distintos entre sí. Archivos: `strings.xml`, `DemoModule.kt`, `HomeScreen.kt`, `ModuleScreen.kt`, `DemoModuleTest.kt`.

## Fase 2 – Módulo 4: Segundo plano y notificaciones (WorkManager + NotificationManager)
**Qué aporta:** trabajo pesado que sigue aunque cierres la app y te avisa al terminar. **Ejemplo:** lanzas una descarga simulada de 30 s, bloqueas el móvil y recibes una notificación cuando acaba.

**Qué ve el usuario**
- Controles: duración (10 / 30 / 60 s), interruptor "Solo si hay red", interruptor "Solo cargando" y botón "Programar tarea".
- Lista de tareas con estado en vivo (En cola → Ejecutando con barra de progreso → Completada / Cancelada / Fallida), hora de inicio y fin, y botón Cancelar.
- Notificación al terminar; al tocarla se abre la app en este módulo.

**Cómo comprueba que funciona**
1. Programar 30 s, ver la barra avanzar, pulsar Inicio y esperar: llega la notificación.
2. Programar otra, forzar el cierre de la app y volver a abrirla: la tarea sigue o se reanuda (demuestra que WorkManager es persistente).
3. Activar "Solo cargando" sin cargador: queda "En cola" hasta enchufar el cable (demuestra las restricciones).

**Permiso y degradación:** `POST_NOTIFICATIONS` (Android 13+) se pide al pulsar "Programar tarea". Si se deniega, la tarea se ejecuta igual y la lista muestra "Completada (sin notificación: permiso denegado)". En Android ≤ 12 no se pide.

**Técnica:** `DemoWorker : CoroutineWorker` con `setProgress`; `OneTimeWorkRequest` con `Constraints`; `getWorkInfosForUniqueWorkFlow`; `NotificationHelper` con canal; `PendingIntent` al módulo. Dependencias: `androidx.work:work-runtime-ktx`. Archivos: `feature/background/` (`BackgroundScreen`, `BackgroundViewModel`, `DemoWorker`, `NotificationHelper`).
**Riesgos:** Doze puede retrasar el trabajo con la pantalla apagada; el worker ejecuta un bucle de espera con progreso en lugar de depender de retardos largos.

## Fase 3 – Módulo 3: Biometría (BiometricPrompt + Android Keystore)
**Qué aporta:** proteger una acción o un dato con huella o cara. **Ejemplo:** una nota secreta que solo se puede leer tras autenticarte.

**Qué ve el usuario**
- Panel de capacidad: "Disponible" / "Sin sensor" / "Sin huella registrada" (con botón a Ajustes para registrar una) / "No disponible ahora". Es el resultado real de `BiometricManager.canAuthenticate()`.
- Nota secreta: campo de texto + "Guardar protegida". La app muestra el texto cifrado (Base64) para evidenciar que lo guardado no es legible; "Ver nota" abre el `BiometricPrompt` y, si va bien, muestra el texto descifrado unos 30 s y se vuelve a bloquear.
- "Probar autenticación": resultado explícito (éxito, no reconocido, cancelado, bloqueo temporal) en el registro de eventos.

**Cómo comprueba que funciona:** guardar una nota; intentar verla con un dedo no registrado o cancelando (sigue oculta); verla con el dedo correcto (aparece); añadir una huella nueva en Ajustes y comprobar que la clave queda invalidada y la nota deja de poder leerse (mensaje claro).

**Permiso:** `USE_BIOMETRIC` es normal ("Concedido al instalar", ya explicado en la app). La degradación real es de hardware: sin sensor o sin huella, la demo muestra el motivo y oculta el botón de acceso seguro.

**Técnica:** `androidx.biometric`; clave AES-GCM en Keystore con autenticación requerida por uso y `BiometricPrompt.CryptoObject`; almacenamiento en `SharedPreferences`/DataStore propio (no `EncryptedSharedPreferences`, deprecada). **`MainActivity` pasa de `ComponentActivity` a `FragmentActivity`** (requisito de `BiometricPrompt`). Archivos: `feature/biometric/` (`BiometricScreen`, `BiometricAuthenticator`, `SecretNoteStore`).
**Riesgos:** comportamiento de claves con autenticación por uso varía entre API 26-29 y 30+; probar en el teléfono real e indicar su versión de Android.

## Fase 4 – Módulo 1: Ubicación y actividad física (Fused Location + Activity Recognition)
**Qué aporta:** saber dónde estás y qué haces. **Ejemplo:** sales a andar, la app dibuja tu ruta y marca el tramo como "caminando".

**Qué ve el usuario**
- Botón "Iniciar rastreo de ruta": solo entonces se piden FINE + COARSE.
- Mapa (osmdroid) con tu posición y la ruta como línea que crece; panel con latitud/longitud, precisión (m), velocidad, distancia recorrida y tiempo. Indica si el permiso concedido es **preciso o aproximado**.
- Interruptor opcional "Detectar actividad" (pide `ACTIVITY_RECOGNITION`): etiqueta en vivo (Quieto / Caminando / Corriendo / Bici / Vehículo) con confianza y hora de la última lectura.
- "Posición actual" de una sola vez para probar sin moverse.

**Cómo comprueba que funciona:** pulsar "Posición actual" y ver el marcador; iniciar rastreo y caminar unos 50-100 m (la línea y la distancia crecen); activar la detección de actividad y caminar (aparece "Caminando"; puede tardar unos minutos).

**Permisos y degradación:** solo aproximada → círculo de precisión y aviso; denegada → mapa sin posición y explicación; sin `ACTIVITY_RECOGNITION` → la ruta funciona y la etiqueta muestra "no disponible".

**Técnica:** `play-services-location` (`FusedLocationProviderClient`, `ActivityRecognitionClient` con transiciones o actualizaciones), osmdroid (requiere `INTERNET` y user-agent). Rastreo solo con la app abierta (foreground service fuera de alcance). Archivos: `feature/location/`.
**Riesgos:** la detección de actividad depende de Google Play Services y tiene latencia; se avisa en pantalla y se confirma en el móvil. Si no hay Play Services, la ruta no funciona (se detecta y se explica).

## Fase 5 – Módulo 2: Cámara y vídeo (CameraX + Media3)
**Qué aporta:** capturar y revisar fotos y vídeos dentro de la app. **Ejemplo:** grabas un clip de 10 s de tu escritorio y lo reproduces al instante.

**Qué ve el usuario**
- Vista previa en vivo, botón de foto, botón grabar/parar con cronómetro (máx. 15 s) y cambio de cámara frontal/trasera.
- Tira de capturas hechas en la app (miniaturas). Al tocar: foto a pantalla completa o vídeo en un reproductor Media3 con controles; ficha con resolución, tamaño y duración, y botón Eliminar.

**Cómo comprueba que funciona:** hacer una foto (aparece la miniatura); grabar un vídeo hablando y reproducirlo con sonido (prueba de `RECORD_AUDIO`); denegar el micrófono y grabar otro: se reproduce mudo y muestra la etiqueta "Sin audio".

**Permisos y degradación:** `CAMERA` se pide al abrir la cámara; tras la primera denegación sale el rationale. Sin cámara se siguen pudiendo ver las capturas guardadas. `RECORD_AUDIO` es opcional (vídeo mudo).

**Técnica:** `camera-core/camera2/lifecycle/video/view`, `PreviewView` vía `AndroidView`; `media3-exoplayer` + `media3-ui`; archivos en almacenamiento propio de la app (sin permisos de almacenamiento). Archivos: `feature/camera/` (`CameraScreen`, `CapturesRepository`, `PlaybackScreen`).
**Riesgos:** requiere dispositivo físico; el APK crece. Las capturas se guardan solo en la app.

## Fase 6 – Pulido y documentación
Comprobar textos, accesibilidad y tema oscuro; README con requisitos y capturas; añadir el wrapper de Gradle (`gradle wrapper` o Android Studio); actualizar `docs/002-plan_de_analisis_y_desarrollo.md` con este plan detallado.
## Fase 7 (opcional) – Play Integrity API
Solo si se quiere; requiere proyecto de Google Cloud / Play Console.

## Flujo de trabajo y verificación
- Una rama por fase (`fase-N` desde `dev`), CI en cada push (el trigger ya incluye `fase-*`), merge a `dev` cuando el usuario confirma la prueba en su móvil.
- Cada fase termina con: CI en verde (tests unitarios de la lógica pura de cada módulo, lint, APK como artefacto) y la lista "Cómo comprobarlo" ejecutada en el teléfono real por el usuario.
- Tests unitarios por módulo: lógica pura (mapeo de estados de `canAuthenticate()`, cálculo de distancia de ruta, formato de duración, reglas de degradación de permisos) separada de las clases Android para poder probarla en CI.
