package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.validation.validos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.io.IOException

private const val DEMORA_CARGA_MS = 1500L
private const val DEMORA_DETALLE_MS = 600L
private const val DEMORA_GUARDADO_MS = 1500L

// Implementación en memoria: los datos se pierden al cerrar la app (la persistencia llega en el Módulo 3)
class RecordatorioRepositoryEnMemoria(
    private val simulacion: ControlSimulacion,
    datosIniciales: List<Recordatorio> = DatosSimulados.recordatorios.validos()
) : RecordatorioRepository {

    private val recordatorios = MutableStateFlow(datosIniciales)

    // flow { }: primero simula la demora; después emite la lista y cada cambio posterior
    override fun observarRecordatorios(): Flow<List<Recordatorio>> = flow {
        delay(DEMORA_CARGA_MS)
        when (simulacion.modo.value) {
            ModoSimulacion.ErrorAlCargar -> throw IOException("Fallo simulado al cargar")
            ModoSimulacion.SinDatos -> emit(emptyList())
            ModoSimulacion.Normal, ModoSimulacion.ErrorAlGuardar -> emitAll(recordatorios)
        }
    }

    override suspend fun obtenerRecordatorio(id: Long): Recordatorio? {
        delay(DEMORA_DETALLE_MS)
        return recordatorios.value.find { it.id == id }
    }

    override suspend fun guardarRecordatorio(recordatorio: Recordatorio): Recordatorio {
        delay(DEMORA_GUARDADO_MS)
        if (simulacion.modo.value == ModoSimulacion.ErrorAlGuardar) {
            throw IOException("Fallo simulado al guardar")
        }
        val nuevo = recordatorio.copy(id = (recordatorios.value.maxOfOrNull { it.id } ?: 0L) + 1)
        recordatorios.update { actuales -> actuales + nuevo }
        return nuevo
    }

    override fun obtenerCategorias(): List<Categoria> = DatosSimulados.categorias
}
