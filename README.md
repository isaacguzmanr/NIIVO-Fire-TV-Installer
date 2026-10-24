# NIVO Fire TV Installer

Instala o actualiza NIVO en un Amazon Fire TV desde un teléfono Android conectado a la misma red Wi‑Fi. Incluye el APK de NIVO 1.1.

## Descargas

- **Instalador para Android:** `Instalador-NIVO-Android-v1.0.apk`
- **Código fuente completo del proyecto Android Studio (incluye NIVO y el instalador):** `NIVO-Fire-TV-Installer-source-v1.0.zip`
- **Comprobaciones SHA-256:** `SHA256SUMS.txt`

## Instalar NIVO desde el teléfono

1. Descarga `Instalador-NIVO-Android-v1.0.apk` e instálalo en tu teléfono Android. Si Android lo solicita, permite instalar aplicaciones desde el navegador o administrador de archivos.
2. En Fire TV, abre **Configuración → Mi Fire TV → Acerca de** y pulsa varias veces el nombre del dispositivo para mostrar las opciones de desarrollador si aún no aparecen.
3. En **Opciones para desarrolladores**, activa **Depuración ADB**.
4. Conecta el teléfono y el Fire TV a la misma Wi‑Fi. Las redes de invitados pueden impedir que se detecten.
5. Abre **Instalador NIVO**, toca **Buscar Fire TV en mi Wi‑Fi** y elige el televisor. Si no aparece, escribe la IP de **Mi Fire TV → Acerca de → Red**.
6. Toca **Instalar NIVO**. La primera vez, acepta en la pantalla del televisor la autorización de depuración ADB.

El instalador confirma que el dispositivo sea Amazon Fire TV, instala NIVO, prepara el servicio de accesibilidad y abre NIVO. No puede aceptar por ti el aviso de autorización que aparece en el televisor.

## Compilar desde el código fuente

Requisitos: Android Studio o JDK 17, Android SDK Platform 35 y conexión a Internet para descargar las dependencias de Gradle. Descomprime el ZIP, abre la carpeta del proyecto en Android Studio o ejecuta en Windows PowerShell desde esa carpeta:

```powershell
.\gradlew.bat assembleDebug
```

El APK se genera en `app\build\outputs\apk\debug\app-debug.apk`. Las rutas locales, las cachés y las llaves de firma privadas no se incluyen en el código fuente.

## Versiones y prueba

- Instalador NIVO: **1.0** (versionCode 1).
- NIVO incluido: **1.1**.
- El APK se compiló y verificó. No hubo un teléfono Android disponible para probar la conexión inalámbrica de extremo a extremo; la primera autorización ADB debe aceptarse en Fire TV.

## Licencia de terceros

La biblioteca `com.tananaev:adblib 1.3` utiliza BSD 3-Clause. El aviso completo está en `app/src/main/assets/THIRD_PARTY_NOTICES.txt` dentro del ZIP del código fuente.
