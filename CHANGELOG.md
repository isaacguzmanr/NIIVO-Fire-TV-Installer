# Changelog

Cambios relevantes por versión de NIIVO. Las notas describen cambios presentes en el código y artefactos disponibles; no implican que cada combinación de dispositivo se haya probado físicamente.

## 2.1.0-beta.1 — versión de pruebas, 26 de octubre de 2026

Esta pre-release está destinada a pruebas. La validación en Fire TV y teléfono físicos queda pendiente.

### NIIVO Launcher

- Añade un menú contextual compacto para las aplicaciones favoritas, con acciones para reemplazar, quitar de favoritos y abrir la información de la aplicación.
- Conserva los espacios del dock al quitar una favorita para que otra aplicación pueda ocupar esa misma posición.
- Se fija `versionName 2.1.0-beta.1` y `versionCode 11`; se conserva `applicationId com.ivantrj.glasslauncher`.

### NIIVO Installer

- Adapta el menú principal y las pantallas de conexión a pantallas estrechas y tamaños de letra ampliados, con contenido desplazable para evitar recortes.
- Simplifica las descripciones de las cuatro acciones principales y mantiene sus funciones.
- Muestra el estado y los resultados de la búsqueda de Fire TV dentro del flujo de conexión.
- Incluye el APK NIIVO Launcher 2.1.0-beta.1 para instalarlo desde el teléfono.
- Se fija `versionName 2.1.0-beta.1` y `versionCode 18`; se conserva `applicationId com.nivo.installer`.

### Validación de esta beta

- `clean assembleRelease` finalizó correctamente en ambos proyectos.
- Se verificaron los identificadores, las versiones, la firma de ambos APK y que el Launcher integrado en el Installer coincide con el APK generado.
- Las firmas coinciden con las versiones 2.0 correspondientes. La instalación y el funcionamiento en Fire TV y teléfono físicos siguen pendientes de prueba.

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
