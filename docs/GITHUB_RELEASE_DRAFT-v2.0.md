# NIIVO 2.0 | Stable Release

Notas de publicación de NIIVO 2.0. Ambos APK release están compilados, firmados y verificados. El Installer usa el certificado local existente que coincide con el APK público anterior para preservar la posibilidad de actualizar; no se generó ni se subió una keystore.

## Texto preparado para GitHub

### NIIVO 2.0 | Stable Release

NIIVO 2.0 reúne el launcher de Fire TV y su aplicación instaladora para Android en una versión estable.

#### Novedades

- Launcher NIIVO con cinco espacios para aplicaciones favoritas y navegación mediante control remoto.
- Acceso a búsqueda, Wi-Fi y ajustes desde la barra superior.
- Instalación, actualización y desinstalación del Launcher desde Android mediante ADB en la red local.
- Comprobación de releases del Launcher en GitHub, comparación de versión instalada y validación del APK descargado.
- Canales estables y beta diferenciados según la versión instalada.

#### Requisitos

- **Fire TV:** Android API 30 o posterior, Depuración ADB activada y autorización confirmada en el televisor.
- **Teléfono:** Android API 26 o posterior; teléfono y Fire TV en la misma red local para ADB por Wi-Fi.
- La compatibilidad depende de Fire OS y del fabricante; no se afirma compatibilidad universal.

#### Instalación

1. Instala `NIIVO-Installer-v2.0.apk` en el teléfono.
2. Activa Depuración ADB en Fire TV y conecta ambos dispositivos a la misma Wi-Fi.
3. En NIIVO Installer, busca el Fire TV o introduce la IP que aparece en **Mi Fire TV → Acerca de → Red**.
4. Acepta la autorización ADB en el televisor y sigue la pantalla de instalación.

#### Archivos del release

- `NIIVO-Launcher-v2.0.apk` — APK release firmado y verificado.
- `NIIVO-Installer-v2.0.apk` — APK release firmado y verificado.
- `NIIVO-2.0-SHA256SUMS.txt` — hashes SHA-256 de ambos APKs.

#### Actualizar desde una beta

Los `applicationId` se mantienen. El Launcher 2.0 conserva el certificado usado por el beta 2.0.0-beta.5. El Installer 2.0 conserva el certificado del Installer público anterior. Android permitirá actualizar cada app en el sitio si la instalación existente usa el mismo certificado.

## Archivos disponibles en esta preparación

- Launcher release: `../releases/NIIVO-Launcher-v2.0.apk`.
- Installer release firmado: `../releases/NIIVO-Installer-v2.0.apk`.
- Build de depuración Installer 2.0 (solo pruebas, no distribuir): `../app/build/outputs/apk/debug/app-debug.apk`.

Los archivos APK y el checksum adjunto al release son los artefactos publicados para esta versión estable.
