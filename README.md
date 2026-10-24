# Instalador NIVO para Fire TV

Aplicación Android para instalar o actualizar NIVO en un Fire TV desde un teléfono conectado a la misma red Wi‑Fi. El instalador incluye el APK de NIVO 1.1.

## Descargar e instalar en el teléfono

1. Descarga `releases/Instalador-NIVO-Android-v1.0.apk` desde este repositorio.
2. Abre el archivo en tu teléfono Android. Si Android lo solicita, permite instalar aplicaciones desde el navegador o administrador de archivos.
3. Abre **Instalador NIVO**.

## Preparar el Fire TV

1. En Fire TV, abre **Configuración → Mi Fire TV → Acerca de** y pulsa varias veces el nombre del dispositivo hasta activar las opciones de desarrollador (si aún no aparecen).
2. En **Opciones para desarrolladores**, activa **Depuración ADB**.
3. Conecta el teléfono y Fire TV a la misma red Wi‑Fi. Evita redes de invitados que aíslen los dispositivos.
4. En el teléfono, toca **Buscar Fire TV en mi Wi‑Fi**. Si no aparece, escribe la IP que indica Fire TV en **Mi Fire TV → Acerca de → Red**.
5. Selecciona el dispositivo encontrado y toca **Instalar NIVO**.
6. La primera vez, acepta en la pantalla del televisor la autorización de depuración ADB. Confirma en el televisor; el instalador no puede aceptar ese diálogo por ti.

La aplicación verifica que el dispositivo sea un Amazon Fire TV antes de instalar. La instalación conserva los datos si ya existe una versión con la misma firma. El instalador configura el servicio de accesibilidad que NIVO necesita y abre NIVO al terminar.

## Crear el APK desde el código

Requisitos: Android Studio o JDK 17, Android SDK Platform 35 y conexión a Internet para descargar dependencias de Gradle.

En Windows PowerShell, desde la carpeta del proyecto:

```powershell
.\gradlew.bat assembleDebug
```

El APK generado queda en:

```text
app\build\outputs\apk\debug\app-debug.apk
```

El proyecto usa la firma de depuración estándar de Android. `local.properties`, las claves de firma y los archivos de compilación locales no forman parte del repositorio.

## Versiones

- App instaladora: **1.0** (versionCode 1).
- NIVO incluido: **1.1**.
- SHA-256 del APK publicado: ver `releases/SHA256SUMS.txt`.

## Aviso de prueba

La compilación del instalador se completó correctamente y se comprobó el APK. Esta sesión no tuvo un teléfono Android disponible para una prueba real de conexión inalámbrica; el primer emparejamiento ADB requiere confirmar el aviso en el Fire TV.

## Licencias

La biblioteca `com.tananaev:adblib 1.3` está distribuida bajo BSD 3-Clause. El texto completo está en `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.
