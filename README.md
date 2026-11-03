# AquaTrackX 💧

**Aplicación Android para la medición de cloro en línea — sector avícola y ganadero**

| | |
|---|---|
| **Asignatura** | DSY1105 — Desarrollo de Aplicaciones Móviles (Duoc UC) |
| **Evaluación** | Evaluación Parcial 2 (8%) — trabajo en parejas, semanas 6-11 |
| **Estudiantes** | **[Nombre estudiante 1]** — **[Nombre estudiante 2]** |
| **Fecha de entrega** | 31-08-2026 |
| **Tecnología** | Kotlin · Jetpack Compose · Material 3 · MVVM |

> ⚠️ Reemplaza los campos **[Nombre estudiante 1]** y **[Nombre estudiante 2]** de la tabla
> por los nombres reales de la pareja antes de subir el repositorio a GitHub.

---

## 1. Descripción del caso

La empresa avícola/ganadera necesita controlar el **cloro libre** del agua de las naves.
La app permite a **Operarios**, **Supervisores** y **Jefatura** registrar y consultar
mediciones de cloro, recibir alertas automáticas cuando el valor sale de rango,
registrar acciones correctivas y cerrar alertas con evidencia, funcionando también
**sin conexión a internet**.

Todos los datos son **ficticios** y la "API" es una simulación local
(`ChlorineApiSimulada`) que entrega una medición por punto cada 5 minutos.

### Rangos de cloro libre (MVP)

| Estado | Rango |
|---|---|
| 🟢 Normal | 3,00 – 5,00 ppm |
| 🟠 Advertencia | 2,00 a < 3,00 ppm o > 5,00 a 6,00 ppm |
| 🔴 Crítico | < 2,00 ppm o > 6,00 ppm |

Los valores se muestran siempre con **2 decimales** (ej: `3,50 ppm`).

---

## 2. Funcionalidades

### Navegación e interfaz (IE 2.1.1 — 15%)
- Pantalla de **login** con sesión persistente (no pide login en cada apertura).
- **Menú inferior** con 5 destinos: Panel, Puntos, Alertas, Histórico y Perfil.
- Navegación jerárquica: Panel/Puntos → Detalle del punto → Registro de medición.
- Centro de alertas con vista de listado y detalle (acciones, condiciones de cierre).
- Estados de carga y mensajes de resultado en cada pantalla.

### Formularios validados (IE 2.1.2 — 15%)
- Login (correo + contraseña) con **ícono por campo**, mensaje de error bajo cada
  campo y validación al salir del campo.
- Registro de medición: punto obligatorio, valor numérico 0,00–20,00 ppm con
  máximo 2 decimales.
- Acción correctiva: descripción (mín. 10 caracteres) y responsable (mín. 3).

### Lógica desacoplada (IE 2.2.1 — 10%)
- Validadores puros en `core/validacion/` (`LoginValidator`, `MedicionValidator`,
  `AccionCorrectivaValidator`): sin dependencia de Compose ni Android.
- Reglas de negocio en `core/ChlorineRules` (clasificación, rangos, formato, gauge).
- Los ViewModels solo orquestan; la UI solo renderiza mensajes.

### Animaciones (IE 2.2.2 — 10%)
- **Medidor circular animado** de cloro con pulso en estado crítico.
- **Gráfico de líneas** con animación de progreso y relleno degradado.
- Tarjetas con entrada **escalonada** (fundido + desplazamiento).
- Chips de estado con color animado, barras de progreso y transiciones del menú.

### Arquitectura y persistencia (IE 2.3.1 — 15%)
- Estructura **modular** por capas: `domain` → `data` → `ui` → `service`.
- **Persistencia local con Room**: usuarios, granjas, puntos, mediciones,
  alertas y acciones correctivas (conversores de enums, índices y relaciones).
- **MVVM** con `StateFlow`, inyección manual de dependencias (`AppContainer`)
  y factory de ViewModels (sin frameworks externos).
- Consumo de **API simulada** reemplazable por Retrofit (mismo contrato `ChlorineApi`).

### Recursos nativos del dispositivo (IE 2.4.1 — 15%) — 3 recursos
1. **Cámara** 📷: evidencia fotográfica de la medición (FileProvider + permiso CAMERA).
2. **Ubicación** 📍: georreferencia GPS de la medición de terreno (LocationManager).
3. **Notificaciones** 🔔: alertas críticas de cloro en el dispositivo (canal propio,
   permiso POST_NOTIFICATIONS en Android 13+).

### Funcionalidades del MVP del caso
- **Login con perfiles**: 3 usuarios demo con rol principal (Operario, Supervisor,
  Jefatura) y visibilidad de granjas/puntos según rol.
- **Consulta de granjas y puntos**: 3 granjas × 4 puntos = **12 puntos**.
- **Visualización de niveles y estados**: panel con promedio, máximo y mínimo de
  24 h, gráfico histórico y estado por punto.
- **Alertas automáticas** al salir de rango, diferenciando *cloro insuficiente* y
  *cloro excesivo*.
- **Cierre de alertas** solo por Supervisor/Jefatura, exigiendo acción correctiva
  + medición posterior dentro del rango aceptable.
- **Históricos** filtrables por 24 h / 7 días / 30 días, granja y punto, con
  contadores por estado.
- **Acciones correctivas** registradas con responsable y fecha/hora.
- **Modo sin conexión**: las mediciones quedan marcadas como *pendientes de
  sincronización* y se envían al reconectar (interruptor en Perfil).

---

## 3. Usuarios de demostración

Contraseña de todos: **`123456`**

| Rol | Correo | Permisos principales |
|---|---|---|
| Operario | `operario@duoc.cl` | Registrar mediciones, ver sus granjas |
| Supervisor | `supervisor@duoc.cl` | Registrar acciones y **cerrar alertas** |
| Jefatura | `jefatura@duoc.cl` | Ver **todas** las granjas, cerrar alertas |

---

## 4. Pasos para ejecutar el proyecto

### Requisitos
- **Android Studio** (Ladybug o superior) con SDK 37 instalado.
- **JDK 17** (lo incluye Android Studio).
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

### Compilación por terminal (opcional)
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Permisos que pedirá la app
- **Cámara** → al tocar "Tomar foto" en el registro.
- **Ubicación** → al tocar "Obtener ubicación actual".
- **Notificaciones** → al iniciar la app en Android 13+.

---

## 5. Estructura del proyecto

```
app/src/main/java/com/example/aquatrackx/
├── domain/model/        # Enums de dominio (roles, estados, períodos)
├── core/                # ChlorineRules, validadores, hash, preferencias
├── data/
│   ├── local/           # Room: entities, DAOs, base de datos
│   ├── remote/          # ChlorineApi (contrato) + ChlorineApiSimulada
│   ├── repository/      # Auth, Catálogo, Mediciones, Alertas
│   ├── seed/            # Datos ficticios (3 granjas, 12 puntos, histórico)
│   └── AppContainer/    # Inyección de dependencias manual
├── service/             # Recursos nativos: cámara, ubicación, notificaciones
└── ui/
    ├── login/  dashboard/  puntos/  detalle/
    ├── registro/  alertas/  historial/  perfil/
    ├── componentes/     # Campos validados, medidor, gráfico, tarjetas
    ├── navegacion/      # Rutas
    └── AppViewModelFactory.kt
```

---

## 6. Requerimientos externos de la evaluación

- **GitHub**: repositorio público con commits técnicos (ver historial de este repo).
- **Trello**: tablero de planificación con actividad real (user stories, columnas
  *Por hacer / En curso / Hecho*, fechas y miembros).
- ⚠️ Según la rúbrica, **no debe haber cambios en el repositorio después de la
  fecha de entrega** (nota 1.0 automática). La entrega en AVA la realiza un solo
  miembro de la pareja.
