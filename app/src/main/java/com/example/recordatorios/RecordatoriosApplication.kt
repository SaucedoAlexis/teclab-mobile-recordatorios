package com.example.recordatorios

import android.app.Application
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.RecordatorioRepositoryLocalFirst
import com.example.recordatorios.data.local.BaseDatosRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.dataStorePreferencias
import com.example.recordatorios.data.red.ObservadorConexion
import com.example.recordatorios.data.remoto.ClienteApi
import com.example.recordatorios.data.remoto.FuenteRemota
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

    val preferencias by lazy { PreferenciasRepository(dataStorePreferencias) }

    private val conexion by lazy { ObservadorConexion(this, alcanceApp) }

    val repositorio: RecordatorioRepository by lazy {
        RecordatorioRepositoryLocalFirst(
            dao = baseDatos.recordatorioDao(),
            remota = FuenteRemota(ClienteApi.crear()),
            preferencias = preferencias,
            conexion = conexion,
            alcance = alcanceApp
        )
    }
}
