# Recordatorios (Programación Mobile · TECLAB)

App Android de recordatorios que se desarrolla a lo largo de la materia.
Versión actual: **4.0.0-rc1 (Actividad 4)**: Firebase Authentication, sincronización con Cloud Firestore
sobre la arquitectura local-first (Room), reglas de seguridad por cuenta y release firmada (APK + AAB).

| Actividad | Qué agrega |
|-----------|------------|
| 1 | Modelos con data classes y sealed classes, validaciones y listado con Compose |
| 2 | Navegación, ViewModel, UiState, StateFlow y repositorio en memoria |
| 3 | DataStore, Room, repositorio local-first, servicio remoto, sincronización y modo sin conexión |
| 4 | Firebase Authentication, Cloud Firestore (`users/{uid}/reminders/{id}`), reglas de seguridad, Room por cuenta, flavors `dev`/`prod`, versión y firma de la release |

## Requisitos

- Android Studio (probado con AGP 9.4 y el JDK que trae Android Studio).
- Un emulador o teléfono con Android 7.0 (API 24) o superior **con Google Play services**. Probado en un emulador Pixel 10a, API 37.
- Un proyecto de Firebase con Authentication (correo y contraseña) y Cloud Firestore.

## 1. Configurar Firebase

1. En la [consola de Firebase](https://console.firebase.google.com/) crear un proyecto y registrar dos apps Android:
   `com.example.recordatorios` (prod) y `com.example.recordatorios.dev` (dev).
2. Descargar `google-services.json` (desde *Configuración del proyecto*; trae las dos apps) y copiarlo en `app/`.
3. *Authentication → Método de acceso*: habilitar **Correo electrónico/contraseña**.
4. *Firestore*: crear la base en modo producción y publicar las reglas de [`firestore.rules`](firestore.rules).

> `app/google-services.json` no está en el repositorio (es público). El zip de la entrega lo incluye.

## 2. Firma de la release (opcional para compilar debug)

```bash
cp keystore.properties.example keystore.properties
```

Completar la ruta del keystore (fuera del repositorio), las contraseñas y el alias.
`keystore.properties` y los `.jks` están en `.gitignore`. Si el archivo no existe, la release se genera sin firmar.

## 3. Compilar y ejecutar

Variantes: flavor `dev` o `prod` × build type `debug` o `release`.

| Flavor | applicationId | Nombre | Uso |
|--------|---------------|--------|-----|
| `dev` | `com.example.recordatorios.dev` | Recordatorios Dev | Desarrollo; en debug incluye herramientas de prueba en Ajustes |
| `prod` | `com.example.recordatorios` | Recordatorios | Versión para testers |

```bash
./gradlew assembleDevDebug                         # para desarrollar
./gradlew assembleProdRelease bundleProdRelease    # APK firmado + AAB
./gradlew testProdDebugUnitTest                    # pruebas unitarias
```

Salidas: `app/build/outputs/apk/prod/release/app-prod-release.apk` y `app/build/outputs/bundle/prodRelease/app-prod-release.aab`.

## Organización del código

```
com.example.recordatorios
├── RecordatoriosApplication.kt   ← una sola instancia de Auth, Firestore, Room, DataStore y repositorio
├── MainActivity.kt               ← verifica la sesión en cada onStart
├── data/
│   ├── auth/           AuthRepository (interfaz), FirebaseAuthRepository, CerrarSesion
│   ├── local/          Room: RecordatorioEntity (con uid), RecordatorioDao, migración 1→2
│   ├── remoto/         FuenteRemotaRecordatorios (interfaz), FuenteRemotaFirestore, RecordatorioRemoto
│   ├── preferencias/   DataStore
│   ├── red/            ObservadorConexion
│   ├── Mapeos.kt       Firestore ↔ Room ↔ modelo y resolución de conflictos
│   └── RecordatorioRepositoryLocalFirst.kt  ← coordina sesión, Room y Firestore
├── domain/  validation/
└── ui/  acceso/ (login y registro), lista/, crear/, detalle/, ajustes/, navegacion/ (RaizApp)
```

Ningún `@Composable` usa Firebase: las pantallas solo conocen los ViewModels y los tipos propios
(`EstadoSesion`, `ErrorAuth`, `ResultadoSincronizacion`).

## Cómo funciona

1. **Sesión:** `RaizApp` muestra el acceso o los recordatorios según `EstadoSesion`. Firebase conserva la sesión; al volver a primer plano se verifica con `reload()` (cuenta inhabilitada o eliminada → vuelve al acceso con aviso).
2. **Local-first:** toda escritura va primero a Room (`PENDIENTE`) con el `uid` de la cuenta activa; la pantalla se actualiza por Flow con o sin red.
3. **Sincronización:** con red y sesión, los pendientes se envían a `users/{uid}/reminders/{id}` con `set()` y el **mismo UUID** que en Room (un reintento no duplica). Después se descarga la colección de la cuenta. Conflictos: gana el `actualizadoEn` más reciente.
4. **Seguridad:** las reglas de Firestore solo permiten leer y escribir debajo del propio uid y validan los datos. Al cerrar sesión se borran del teléfono los recordatorios de la cuenta; al iniciar sesión, los de cualquier otra.
