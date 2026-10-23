# DinoDateAuto 2.0 · Android 15 / HyperOS 2 · sin root

Proyecto de Android (Java) compilable por GitHub Actions. **Código fuente, no APK precompilado.**

## Funciones

- Servicio de Accesibilidad que identifica la pantalla de `com.emgruppen.bitcoindino` si el árbol de accesibilidad contiene **DINO HUB**, **Bitcoin Valley** y **POWER-UPS**.
- Una sola activación por visita, con intervalo mínimo de 60 segundos entre intentos.
- Integración con Shizuku API 13.1.5: solicita autorización y prueba el comando `cmd alarm set-time <milisegundos>`.
- Comprueba por separado que el reloj haya avanzado; si falla, muestra una notificación que abre los ajustes de fecha y hora al tocarla.
- Botón para activar/desactivar el detector. Desactivado inicialmente.

## Generar APK

1. Crear repositorio vacío en GitHub y subir **el contenido** de este ZIP, incluida `.github/workflows/build-apk.yml`.
2. Ir a Actions > **Build Android APK** > **Run workflow** (o hacer push en `main`).
3. Descargar el artefacto `DinoDateAuto-debug-apk` y extraer `app-debug.apk`.
4. Instalarlo, activar Accesibilidad para Dino Date Auto y dar permiso de notificaciones.
5. Instalar y arrancar Shizuku con depuración inalámbrica, autorizar Dino Date Auto dentro de Shizuku y desactivar fecha automática en Ajustes.
6. Activar el detector desde la app. En HyperOS, permitir autoinicio o uso sin restricciones de batería si la capa del fabricante limita el servicio.

## Limitaciones importantes

- **No hay garantía de cambio automático de fecha en Android 15 sin root.** Shizuku iniciado vía ADB no otorga root; el comando puede no estar disponible o puede ser rechazado. El flujo manual es el resultado esperado cuando no existen permisos suficientes.
- Los juegos renderizados con OpenGL/Unity pueden **no exponer texto a Accesibilidad**. Este proyecto no realiza reconocimiento de imágenes ni capturas de pantalla continuas.
- Con el sistema bloqueado o el servicio desactivado no hay detección; no es un servicio residente incondicional.
- Shizuku puede necesitar iniciarse de nuevo después de reiniciar el teléfono.
- Cambiar la fecha del dispositivo puede afectar alarmas, sincronización, autenticación y otros programas. Las recompensas regidas por el servidor no cambian necesariamente.
- No se ha compilado ni probado físicamente en el teléfono del usuario.

## Referencias

- https://github.com/RikkaApps/Shizuku-API
- https://shizuku.rikka.app/
- https://developer.android.com/reference/android/app/AlarmManager#setTime(long)
