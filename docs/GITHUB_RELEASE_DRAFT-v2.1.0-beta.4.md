# NIIVO Installer 2.1.0-beta.4

Pre-release de prueba de **NIIVO Installer para Android**. Esta versión actualiza solo la instaladora; el proyecto y APK del Launcher no se modifican.

## Cambios

- Nueva opción **Reparar accesibilidad** en Configuración. Reutiliza la IP de Fire TV guardada o permite introducir otra.
- Antes de cambiar el servicio, comprueba por ADB que el dispositivo se identifique como Amazon Fire TV y que NIIVO esté instalado.
- Después de instalar o actualizar el Launcher, vuelve a intentar habilitar y verificar el servicio de accesibilidad. Si Fire OS no confirma la activación, la app separa el resultado de instalación de la reparación y explica cómo repararlo manualmente.
- Conserva los servicios de accesibilidad que ya estaban activos.
- `applicationId`: `com.nivo.installer`; código 19. La firma coincide con la beta anterior para permitir actualización sobre ella.

## Requisitos de prueba

- Teléfono Android 8.0 o superior.
- Fire TV encendido y conectado a la misma red local.
- Depuración ADB activada y autorización aceptada en el televisor.

Instala el APK adjunto sobre la versión anterior sin desinstalarla. La reparación por ADB en un Fire TV físico aún requiere confirmación de prueba.
