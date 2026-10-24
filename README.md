# Instalador NIIVO para Fire TV

Instala, actualiza o quita NIIVO en un Amazon Fire TV desde un teléfono Android. El instalador 1.4.0 estrena una interfaz azul y violeta más limpia, con accesos separados para instalar, actualizar, desinstalar y configuración.

## Descargas

- **Instalador Android 1.4.2:** [NIIVO-Installer-v1.4.0.apk](https://github.com/isaacguzmanr/NIVO-Fire-TV-Installer/releases/download/1.4.2/NIIVO-Installer-v1.4.2.apk)
- **Código fuente:** [Descargar ZIP de main](https://github.com/isaacguzmanr/NIVO-Fire-TV-Installer/archive/refs/heads/main.zip)
- **SHA-256:** [NIIVO-Installer-v1.4.0-SHA256SUMS.txt](https://github.com/isaacguzmanr/NIVO-Fire-TV-Installer/raw/refs/heads/main/releases/NIIVO-Installer-v1.4.0-SHA256SUMS.txt)

## Instalar NIIVO desde el teléfono

1. Descarga e instala el APK en un teléfono Android.
2. En Fire TV, abre **Configuración → Mi Fire TV → Acerca de** y pulsa varias veces el nombre del dispositivo para mostrar las opciones de desarrollador si aún no aparecen.
3. En **Opciones para desarrolladores**, activa **Depuración ADB**.
4. Conecta el teléfono y el Fire TV a la misma Wi-Fi.
5. Abre **Instalador NIIVO**, elige **Instalar** o **Actualizar** y busca el dispositivo. Si no aparece, escribe la IP de **Mi Fire TV → Acerca de → Red**.
6. Acepta en el televisor el aviso de autorización ADB.

El instalador confirma que el dispositivo sea Amazon Fire TV, instala NIIVO, prepara el servicio de accesibilidad que redirige el botón Inicio y abre NIIVO. No puede aceptar por ti el aviso que aparece en el televisor.

## Compilar desde el código fuente

Requisitos: Android Studio o JDK 17, Android SDK Platform 35 y conexión a Internet para descargar las dependencias de Gradle. Desde la carpeta del proyecto, ejecuta en Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

El APK de depuración se genera en `app\build\outputs\apk\debug\app-debug.apk`.

## Versiones

- Instalador NIIVO: **1.4.0** (versionCode 5).
- NIIVO incluido: **1.2** (versionCode 3).

## Licencia de terceros

La biblioteca `com.tananaev:adblib 1.3` utiliza BSD 3-Clause. El aviso completo está en `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.
