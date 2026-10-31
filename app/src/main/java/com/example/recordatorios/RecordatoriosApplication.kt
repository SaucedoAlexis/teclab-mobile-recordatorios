package com.example.recordatorios

import android.app.Application
import com.example.recordatorios.data.ControlSimulacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.RecordatorioRepositoryEnMemoria

// Crea las dependencias una sola vez para toda la app.
// Vive más que la Activity, así que el repositorio sobrevive a una rotación.
class RecordatoriosApplication : Application() {
    val simulacion = ControlSimulacion()
    val repositorio: RecordatorioRepository by lazy { RecordatorioRepositoryEnMemoria(simulacion) }
}
