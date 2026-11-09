package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Contrato de la capa de datos. Los ViewModel dependen de esta interfaz y no saben
// si los datos vienen de Room, del servidor o de los dos.
interface RecordatorioRepository {
    // Listado desde Room, ya filtrado y ordenado según las preferencias guardadas en DataStore
    fun observarRecordatorios(): Flow<List<Recordatorio>>
    fun observarRecordatorio(id: String): Flow<Recordatorio?>
    suspend fun buscarRecordatorio(id: String): Recordatorio?

    // Escrituras: se aplican primero en Room y quedan pendientes de sincronizar
    suspend fun guardarRecordatorio(recordatorio: Recordatorio): ResultadoOperacion
    suspend fun cambiarCompletado(id: String, completado: Boolean): ResultadoOperacion
    suspend fun eliminarRecordatorio(id: String): ResultadoOperacion

    fun obtenerCategorias(): List<Categoria>

    // Información para mostrar el estado de la sincronización
    val hayConexion: StateFlow<Boolean>
    val estadoSincronizacion: StateFlow<EstadoSincronizacion>
    fun observarCantidadSinSincronizar(): Flow<Int>

    suspend fun sincronizar(): ResultadoSincronizacion

    // Actividad 4: borra del teléfono los recordatorios de una cuenta (se usa al cerrar sesión)
    suspend fun borrarDatosLocales(uid: String)
}
