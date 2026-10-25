# Instalador NIIVO para Fire TV

Instala, actualiza o quita NIIVO en un Amazon Fire TV desde un teléfono Android. Esta beta pública incluye NIIVO **2.0.0-beta.1** y el instalador Android actualizado.

## Descargas

- **Instalador Android 1.5.0-beta.1:** [NIIVO-Installer-v1.5.0-beta.1.apk]((https://github.com/isaacguzmanr/NIIVO-Fire-TV-Installer/releases/download/v2.0.0-beta.2/NIIVO-Installer-v1.5.0-beta.2.apk))

- **SHA-256 del instalador:** [NIIVO-Installer-v1.5.0-beta.1-SHA256SUMS.txt](https://github.com/isaacguzmanr/NIVO-Fire-TV-Installer/releases/download/v2.0.0-beta.1/NIIVO-Installer-v1.5.0-beta.1-SHA256SUMS.txt)
- **Código fuente:** [Descargar ZIP de main](https://github.com/isaacguzmanr/NIVO-Fire-TV-Installer/archive/refs/heads/main.zip)

## Instalar NIIVO desde el teléfono

1. Descarga e instala el APK del Instalador NIIVO en un teléfono Android.
2. En Fire TV, activa **Depuración ADB** en las opciones para desarrolladores.
3. Conecta el teléfono y el Fire TV a la misma Wi-Fi.
4. Abre **Instalador NIIVO**, elige **Instalar** o **Actualizar** y busca el dispositivo. Si no aparece, escribe la IP de **Mi Fire TV → Acerca de → Red**.
5. Acepta en el televisor el aviso de autorización ADB.

El instalador detecta Fire TV, instala NIIVO, configura el servicio de accesibilidad que redirige el botón Inicio y abre el launcher. No puede aceptar por ti el aviso ADB del televisor.

## Versiones

- Instalador NIIVO: **1.5.0-beta.1** (versionCode 9).
- NIIVO incluido: **2.0.0-beta.1** (versionCode 5).

## Licencia de terceros

La biblioteca `com.tananaev:adblib 1.3` utiliza BSD 3-Clause. El aviso completo está en `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.
