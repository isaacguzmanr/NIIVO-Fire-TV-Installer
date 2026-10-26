# Changelog

Cambios relevantes por versión de NIIVO. Las notas describen cambios presentes en el código y artefactos disponibles; no implican que cada combinación de dispositivo se haya probado físicamente.

## 2.0 — estable, 26 de octubre de 2026

### NIIVO Launcher

- Consolida la línea visual y funcional 2.0 del launcher de Fire TV como versión estable.
- Mantiene cinco espacios de aplicaciones favoritas, navegación con control remoto y acciones contextuales del dock.
- Incluye la barra superior con búsqueda, Wi-Fi, configuración y reloj del dispositivo.
- Conserva el redireccionamiento del botón Inicio mediante el servicio de accesibilidad.
- Se fija `versionName 2.0`, `versionCode 10` y el mismo `applicationId` de las instalaciones beta.

### NIIVO Installer

- Se fija `versionName 2.0`, `versionCode 17` y se conserva `applicationId com.nivo.installer`.
- Se incluye el APK estable NIIVO Launcher 2.0 firmado con el mismo certificado local que el Launcher beta 2.0.0-beta.5.
- El comprobador de releases acepta tags y nombres de APK `x.y` y `x.y.z`, incluidos los assets `NIIVO-Launcher-vX.Y.apk`.
- El canal consultado se determina a partir de la versión instalada: estable para instalaciones estables y estable/beta para instalaciones beta.
- Se conserva la validación de tag, nombre de asset, versión empaquetada, tamaño descargado y checksum cuando el release lo proporciona.
- Se genera `NIIVO-Installer-v2.0.apk` firmado con el certificado local existente, verificado como coincidente con el certificado del Installer publicado; se conserva así la compatibilidad de firma para actualizaciones.

### Documentación y preparación

- Actualiza los README del Installer y Launcher, añade esta bitácora y prepara un borrador de notas para GitHub.
- Añade capturas reales disponibles; identifica las capturas y validaciones de hardware que faltan.
- La compilación del Installer conserva la firma existente de Android usada por la versión pública anterior; no se creó una keystore nueva.

### Validación ejecutada

- `clean assembleRelease` del Launcher: correcto; APK verificado y firmado.
- `clean assembleRelease` del Installer: correcto; APK firmado verificado como `com.nivo.installer`, versión 2.0 (código 17), con el certificado coincidente con la versión pública anterior.
- `assembleDebug` del Installer, instalación y navegación de inicio/actualizaciones/configuración en emulador Android API 37: correctas.
- Fire TV físico, instalación ADB completa y Samsung Galaxy S21 FE 5G: no verificados en esta sesión.
