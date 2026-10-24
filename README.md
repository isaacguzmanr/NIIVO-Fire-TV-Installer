# Instalador NIIVO para Fire TV

Instala o actualiza NIIVO en un Amazon Fire TV desde un teléfono Android conectado a la misma red Wi‑Fi. La versión 1.3 del instalador incluye NIIVO 1.2, con los iconos superiores del launcher renovados.

## Descargas

- **Instalador para Android:** [NIIVO.apk](https://github.com/isaacguzmanr/NIIVO-Fire-TV-Installer/releases/latest/download/NIIVO.apk)
- **Código fuente completo:** [NIIVO-Fire-TV-Installer-source-v1.3.zip](https://github.com/isaacguzmanr/NIIVO-Fire-TV-Installer/releases/latest/download/NIIVO-Fire-TV-Installer-source-v1.3.zip)
- **Comprobaciones SHA-256:** [SHA256SUMS.txt](https://github.com/isaacguzmanr/NIVO-Fire-TV-Installer/releases/download/v1.3/SHA256SUMS.txt)

## Instalar NIIVO desde el teléfono

1. Descarga el APK e instálalo en tu teléfono Android. Si Android lo solicita, permite instalar aplicaciones desde el navegador o administrador de archivos.
2. En Fire TV, abre **Configuración → Mi Fire TV → Acerca de** y pulsa varias veces el nombre del dispositivo para mostrar las opciones de desarrollador si aún no aparecen.
3. En **Opciones para desarrolladores**, activa **Depuración ADB**.
4. Conecta el teléfono y el Fire TV a la misma Wi‑Fi. Las redes de invitados pueden impedir que se detecten.
5. Abre **Instalador NIIVO**, toca **Buscar en la red Wi‑Fi** y elige el televisor. Si no aparece, escribe la IP de **Mi Fire TV → Acerca de → Red**.
6. Toca **Instalar NIIVO**. La primera vez, acepta en la pantalla del televisor la autorización de depuración ADB.

El instalador confirma que el dispositivo sea Amazon Fire TV, instala NIIVO, prepara el servicio de accesibilidad que redirige el botón Inicio y abre NIIVO. No puede aceptar por ti el aviso de autorización que aparece en el televisor.

## Compilar desde el código fuente

Requisitos: Android Studio o JDK 17, Android SDK Platform 35 y conexión a Internet para descargar las dependencias de Gradle. Descomprime el ZIP, abre la carpeta del proyecto en Android Studio o ejecuta en Windows PowerShell desde esa carpeta:

```powershell
.\gradlew.bat assembleDebug
```

El APK de depuración se genera en `app\build\outputs\apk\debug\app-debug.apk`. Las rutas locales, las cachés y las llaves privadas de firma no se incluyen en el código fuente.

## Versiones

- Instalador NIIVO: **1.3** (versionCode 4).
- NIIVO incluido: **1.2** (versionCode 3).

## Licencia de terceros

La biblioteca `com.tananaev:adblib 1.3` utiliza BSD 3-Clause. El aviso completo está en `app/src/main/assets/THIRD_PARTY_NOTICES.txt` dentro del ZIP del código fuente.
