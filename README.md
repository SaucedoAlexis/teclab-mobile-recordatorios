# Recordatorios (Programación Mobile · TECLAB)

App Android de recordatorios que se desarrolla a lo largo de la materia.
Versión actual: **3.0 (Actividad 3)**: persistencia con Room y DataStore, repositorio local-first,
servicio remoto con Retrofit + OkHttp y sincronización controlada.

| Actividad | Qué agrega |
|-----------|------------|
| 1 | Modelos con data classes y sealed classes, validaciones y listado con Compose |
| 2 | Navegación, ViewModel, UiState, StateFlow y repositorio en memoria |
| 3 | DataStore, Room, repositorio local-first, Retrofit/OkHttp, sincronización y modo sin conexión |

## Requisitos

- Android Studio (probado con AGP 9.4 y JDK 17).
- Un emulador o un teléfono con Android 7.0 (API 24) o superior. Probado en un emulador Pixel 10a con API 37.
- Python 3.8 o superior, solo para el servidor simulado (no necesita librerías extra).

## 1. Configurar

El proyecto lee la URL del servicio y la clave desde `api.properties` (en la raíz).
Ese archivo **no se sube al repositorio**. Hay un ejemplo sin secretos:

```bash
cp api.properties.example api.properties
```

| Propiedad | Para qué sirve | Valor de ejemplo |
|-----------|----------------|------------------|
| `api.baseUrl` | URL base del servicio (tiene que terminar con `/`) | `http://localhost:3000/` |
| `api.key` | Clave que se manda en el encabezado `X-Api-Key` | `clave-de-prueba-local` |

> Si no existe `api.properties`, Gradle usa los valores de `api.properties.example`, así que el proyecto compila igual.

## 2. Levantar el servidor simulado

```bash
cd servidor-simulado
python servidor.py
```

Queda escuchando en `http://localhost:3000/` y guarda los datos en `servidor-simulado/datos.json`.
La primera vez arranca con 4 recordatorios de ejemplo.

Con el emulador ya abierto, conectar su puerto 3000 con el de la PC (hay que repetirlo si se reinicia el emulador):

```bash
adb reverse tcp:3000 tcp:3000
```

Para probar errores, abrir estas direcciones en el navegador de la PC:

| Dirección | Qué simula |
|-----------|------------|
| `http://localhost:3000/_modo/normal` | Todo funciona |
| `http://localhost:3000/_modo/error500` | Error del servidor (500) |
| `http://localhost:3000/_modo/lento` | Tarda 15 s: la app corta a los 10 s (timeout) |
| `http://localhost:3000/_modo/rechazar400` | Rechaza los recordatorios enviados (400) |

## 3. Compilar y ejecutar

Desde Android Studio: abrir la carpeta del proyecto, esperar la sincronización de Gradle y ejecutar ▶ `app`.

O desde la terminal:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Pruebas unitarias (mapeos, resolución de conflictos y preferencias inválidas):

```bash
./gradlew testDebugUnitTest
```

> La app funciona aunque el servidor no esté levantado: muestra lo guardado en Room y deja los cambios pendientes de sincronizar.

## Organización del código

```
com.example.recordatorios
├── RecordatoriosApplication.kt   ← crea una sola instancia de cada dependencia
├── data/
│   ├── preferencias/   DataStore: PreferenciasUsuario, PreferenciasRepository
│   ├── local/          Room: RecordatorioEntity, RecordatorioDao, BaseDatosRecordatorios, Conversores
│   ├── remoto/         Retrofit/OkHttp: RecordatorioDto, RecordatoriosApi, ClienteApi, FuenteRemota
│   ├── red/            ObservadorConexion (ConnectivityManager → Flow)
│   ├── Mapeos.kt       DTO ↔ entidad ↔ modelo y resolución de conflictos
│   ├── RecordatorioRepository.kt            ← interfaz
│   └── RecordatorioRepositoryLocalFirst.kt  ← coordina DataStore, Room y el servicio
├── domain/       modelos de la app (Recordatorio, Prioridad, EstadoSync…)
├── validation/   reglas del formulario
└── ui/           pantallas Compose: lista, crear (crear/editar), detalle, ajustes, navegación
```

## Cómo funciona la sincronización (resumen)

1. Toda escritura se guarda **primero en Room** con estado `PENDIENTE` (o `PENDIENTE_BORRAR`). La pantalla se actualiza enseguida por Flow.
2. Cuando hay red (al abrir la app, al volver la conexión, después de un cambio o con el botón ⟳), el repositorio envía los pendientes con `PUT /recordatorios/{id}` o `DELETE` y después descarga la lista del servidor.
3. El **id es un UUID creado en el teléfono** y el servidor usa el mismo: reenviar un recordatorio lo actualiza, no lo duplica.
4. Un `Mutex` impide dos sincronizaciones al mismo tiempo. Los timeouts y errores 5xx se reintentan hasta 3 veces (esperas de 2 s y 4 s). Los 4xx no se reintentan solos.
5. Si un recordatorio cambió en los dos lados, gana la versión con `updatedAt` más reciente (last-write-wins).
