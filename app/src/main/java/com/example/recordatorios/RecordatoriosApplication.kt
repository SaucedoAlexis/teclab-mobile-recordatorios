package com.example.recordatorios

import android.app.Application
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.RecordatorioRepositoryLocalFirst
import com.example.recordatorios.data.auth.FirebaseAuthRepository
import com.example.recordatorios.data.local.BaseDatosRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.dataStorePreferencias
import com.example.recordatorios.data.red.ObservadorConexion
import com.example.recordatorios.data.remoto.FuenteRemotaFirestore
import com.example.recordatorios.data.remoto.PruebaAccesoCruzado
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Contenedor de dependencias: crea UNA sola instancia de cada pieza para toda la app.
// Vive más que la Activity, así que nada se vuelve a crear al rotar la pantalla.
class RecordatoriosApplication : Application() {

    // Alcance para el trabajo que no depende de una pantalla (por ejemplo, sincronizar al volver la red).
    // SupervisorJob: si una tarea falla, las demás siguen funcionando.
    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val baseDatos by lazy { BaseDatosRecordatorios.crear(this) }

    // Sesión de Firebase Authentication (una sola instancia para toda la app)
    val auth by lazy { FirebaseAuthRepository() }

    val preferencias by lazy { PreferenciasRepository(dataStorePreferencias) }

    private val conexion by lazy { ObservadorConexion(this, alcanceApp) }

    // Cloud Firestore con caché solo en memoria: el almacenamiento offline de la app es Room.
    // Así los recordatorios no quedan guardados dos veces en el teléfono (Room + caché de Firestore)
    // y al cerrar sesión basta con limpiar Room.
    private val firestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
    }

    private val fuenteRemota by lazy { FuenteRemotaFirestore(firestore) }

    // Solo para la sección de pruebas de la versión de desarrollo (paso 06: acceso cruzado)
    val pruebaAccesoCruzado by lazy { PruebaAccesoCruzado(auth, fuenteRemota) }

    val repositorio: RecordatorioRepository by lazy {
        RecordatorioRepositoryLocalFirst(
            dao = baseDatos.recordatorioDao(),
            remota = fuenteRemota,
            preferencias = preferencias,
            conexion = conexion,
            auth = auth,
            alcance = alcanceApp
        )
    }
}
