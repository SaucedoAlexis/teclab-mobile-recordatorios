package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.Flow

// Contrato de la fuente de datos: los ViewModel dependen de esta interfaz,
// no de una lista concreta. Más adelante se puede reemplazar por Room o una API.
interface RecordatorioRepository {
    fun observarRecordatorios(): Flow<List<Recordatorio>>
    suspend fun obtenerRecordatorio(id: Long): Recordatorio?
    suspend fun guardarRecordatorio(recordatorio: Recordatorio): Recordatorio
    fun obtenerCategorias(): List<Categoria>
}
