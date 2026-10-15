# Guía de Planificación: Arquitectura y Permisos para una App Demo en Android

Esta guía está diseñada para estructurar el desarrollo de una aplicación demostrativa (*Demo App*) en Android. El objetivo es integrar de forma limpia y pedagógica las APIs más representativas del sistema operativo, justificando debidamente los permisos requeridos para cumplir con las directrices de Google Play.

---

## 1. Alcance y Objetivo de la App Demo
La aplicación demo mostrará el uso práctico de diferentes subsistemas del hardware y del sistema operativo Android, dividida en módulos interactivos. Cada módulo requerirá una gestión adecuada de permisos en tiempo de ejecución.

---

## 2. Matriz de Módulos, APIs y Permisos

### Módulo 1: Geolocalización y Contexto Físico
Permite visualizar la posición actual del usuario en un mapa y detectar su actividad física (caminando, en vehículo).
* **APIs / Servicios utilizados:** 
  * `FusedLocationProviderClient` (Ubicación optimizada).
  * `ActivityRecognitionClient` (Detección de movimiento).
* **Permisos requeridos (Peligrosos):**
  * `android.permission.ACCESS_FINE_LOCATION` (Ubicación precisa).
  * `android.permission.ACCESS_COARSE_LOCATION` (Ubicación aproximada).
  * `android.permission.ACTIVITY_RECOGNITION` (Actividad física - Android 10+).
* **Estrategia en la App Demo:** Solicitar el permiso de ubicación únicamente cuando el usuario pulse el botón "Iniciar Rastreo de Ruta", evitando pedirlo en el arranque inicial.

### Módulo 2: Captura Multimedia Avanzada
Permite tomar una fotografía o grabar un breve clip de vídeo utilizando las herramientas modernas de la cámara.
* **APIs / Servicios utilizados:**
  * `CameraX` (Control simplificado del ciclo de vida de la cámara).
  * `Media3 / ExoPlayer` (Reproducción del contenido capturado).
* **Permisos requeridos (Peligrosos):**
  * `android.permission.CAMERA` (Acceso al sensor óptico).
  * `android.permission.RECORD_AUDIO` (Captura de voz para vídeo).
* **Estrategia en la App Demo:** Mostrar un diálogo explicativo (*Rationale*) si el usuario deniega el acceso a la cámara por primera vez.

### Módulo 3: Autenticación Biométrica
Demuestra el uso de la seguridad de hardware para proteger pantallas o acciones sensibles dentro de la aplicación.
* **APIs / Servicios utilizados:**
  * `BiometricPrompt` (Interfaz unificada de huella dactilar y reconocimiento facial).
  * `Android Keystore` / `EncryptedSharedPreferences`.
* **Permisos requeridos:**
  * `android.permission.USE_BIOMETRIC` (Nota: en su mayoría gestionado automáticamente por el prompt del sistema).
* **Estrategia en la App Demo:** Validar la disponibilidad del hardware biométrico (`BiometricManager.canAuthenticate()`) antes de mostrar el botón de acceso seguro.

### Módulo 4: Tareas en Segundo Plano y Notificaciones
Simula la descarga de datos pesados o la sincronización con un servidor remoto mientras la app está minimizada.
* **APIs / Servicios utilizados:**
  * `WorkManager` (Tareas persistentes garantizadas).
  * `NotificationManager` (Alertas en la barra de estado).
* **Permisos requeridos (Android 13+):**
  * `android.permission.POST_NOTIFICATIONS` (Permiso peligroso de ejecución).
* **Estrategia en la App Demo:** Programar una tarea diferida con `WorkManager` que al finalizar emita una notificación local informando al usuario.

---

## 3. Flujo de Implementación Recomendado (Buenas Prácticas)

1. **Declaración en el Manifiesto:** Declarar estrictamente los permisos necesarios en el `AndroidManifest.xml`.
2. **Encapsulamiento de Permisos:** Utilizar contratos modernos con `registerForActivityResult(ActivityResultContracts.RequestPermission())` o `RequestMultiplePermissions()`.
3. **Manejo de Negativa:** Asegurar que si el usuario rechaza un permiso opcional, la app demo continúe funcionando degradando elegantemente la funcionalidad asociada, en lugar de bloquearse o cerrarse de manera forzosa.
4. **Validación de Integridad:** Opcionalmente, integrar pruebas con la `Play Integrity API` para verificar el estado seguro del entorno de ejecución de la demo.