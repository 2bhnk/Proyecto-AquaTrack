# AquaTrack

**Aplicación Android para la medición de cloro en línea — sector avícola y ganadero**

| | |
|---|---|
| **Asignatura** | DSY1105 — Desarrollo de Aplicaciones Móviles |
| **Evaluación** | Evaluación Parcial 2 (8%) |
| **Estudiante** | **William Zamorano** |

---

Todos los datos son **ficticios** y "API" es una simulación local
(`ChlorineApiSimulada`) que entrega una medición por punto cada 5 minutos.

### Rangos de cloro libre (MVP)
1 ppm equivale a 1 miligramo de cloro por cada litro de agua 

| Estado | Rango |
|---|---|
| 🟢 Normal | 3,00 – 5,00 ppm |
| 🟠 Advertencia | 2,00 a < 3,00 ppm o > 5,00 a 6,00 ppm |
| 🔴 Crítico | < 2,00 ppm o > 6,00 ppm |

Los valores se muestran siempre con **2 decimales** (ej: `3,50 ppm`).

---

Contraseña de todos: **`123456`**

| Rol | Correo | Permisos principales |

| Operario | `operario@duoc.cl` | Registrar mediciones, ver sus granjas |
| Supervisor | `supervisor@duoc.cl` | Registrar acciones y **cerrar alertas** |
| Jefatura | `jefatura@duoc.cl` | Ver **todas** las granjas, cerrar alertas |

---

## Pasos para ejecutar el proyecto

### Requisitos
- **Android Studio** con SDK 37 instalado.
- **JDK 17**
- Un **emulador** (API 24+) o un dispositivo Android real con depuración USB.

### Ejecución
1. Clonar o descargar el repositorio:
   ```bash
   git clone https://github.com/<usuario>/AquaTrackX.git
   ```
2. Abrir la carpeta del proyecto en **Android Studio** (*File → Open*).
3. Esperar la sincronización de Gradle (*Sync Now*).
4. Seleccionar un dispositivo/emulador y presionar **▶ Run** (`Shift + F10`).
5. Ingresar con una cuenta de la tabla anterior.

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Permisos que pedirá la app
- **Cámara** → al tocar "Tomar foto" en el registro.
- **Ubicación** → al tocar "Obtener ubicación actual".
- **Notificaciones** → al iniciar la app en Android 13+.

---


##  Requerimientos externos

- **GitHub**: repositorio público con commits técnicos.
