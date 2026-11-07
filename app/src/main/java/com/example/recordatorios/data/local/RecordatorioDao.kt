package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Operaciones permitidas sobre la tabla. Las consultas que usa la interfaz devuelven Flow:
// cuando la tabla cambia, Room vuelve a ejecutarlas y emite el resultado nuevo.
// Las operaciones puntuales son suspend (Room no deja usar la base desde el hilo principal).
@Dao
interface RecordatorioDao {

    // Listado completo (los marcados para borrar ya no se muestran)
    @Query("SELECT * FROM recordatorios WHERE estado_sync != 'PENDIENTE_BORRAR'")
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    // Filtro por estado: solo los que no están completados
    @Query("SELECT * FROM recordatorios WHERE estado_sync != 'PENDIENTE_BORRAR' AND completado = 0")
    fun observarSinCompletar(): Flow<List<RecordatorioEntity>>

    // Un recordatorio por identificador (pantalla de detalle)
    @Query("SELECT * FROM recordatorios WHERE id = :id AND estado_sync != 'PENDIENTE_BORRAR'")
    fun observarPorId(id: String): Flow<RecordatorioEntity?>

    // Cuántos cambios faltan enviar al servidor (se muestra en el listado)
    @Query("SELECT COUNT(*) FROM recordatorios WHERE estado_sync IN ('PENDIENTE', 'PENDIENTE_BORRAR', 'ERROR')")
    fun observarCantidadSinSincronizar(): Flow<Int>

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun buscarPorId(id: String): RecordatorioEntity?

    // Cola de operaciones pendientes, de la más vieja a la más nueva
    @Query("SELECT * FROM recordatorios WHERE estado_sync IN ('PENDIENTE', 'PENDIENTE_BORRAR') ORDER BY actualizado_en ASC")
    suspend fun buscarPendientes(): List<RecordatorioEntity>

    @Query("SELECT id FROM recordatorios WHERE estado_sync = 'SINCRONIZADO'")
    suspend fun buscarIdsSincronizados(): List<String>

    // Inserta si el id no existe o actualiza si ya existe (sirve para crear y para editar)
    @Upsert
    suspend fun guardar(recordatorio: RecordatorioEntity)

    @Query("UPDATE recordatorios SET completado = :completado, fecha_completado = :fechaCompletado, actualizado_en = :ahora, estado_sync = 'PENDIENTE' WHERE id = :id")
    suspend fun cambiarCompletado(id: String, completado: Boolean, fechaCompletado: Long?, ahora: Long)

    // Borrado "suave": se marca y se oculta; se borra de verdad cuando el servidor lo confirma
    @Query("UPDATE recordatorios SET estado_sync = 'PENDIENTE_BORRAR', actualizado_en = :ahora WHERE id = :id")
    suspend fun marcarParaBorrar(id: String, ahora: Long)

    // Marca como sincronizado solo si nadie lo modificó mientras se enviaba
    @Query("UPDATE recordatorios SET estado_sync = 'SINCRONIZADO' WHERE id = :id AND actualizado_en = :actualizadoEn")
    suspend fun marcarSincronizado(id: String, actualizadoEn: Long)

    @Query("UPDATE recordatorios SET estado_sync = 'ERROR' WHERE id = :id")
    suspend fun marcarConError(id: String)

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminarDefinitivo(id: String)
}
