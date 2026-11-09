package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Operaciones permitidas sobre la tabla. Las consultas que usa la interfaz devuelven Flow:
// cuando la tabla cambia, Room vuelve a ejecutarlas y emite el resultado nuevo.
// Las operaciones puntuales son suspend (Room no deja usar la base desde el hilo principal).
//
// Actividad 4: toda consulta y modificación recibe el uid de la cuenta activa y lo usa en el WHERE.
// Aunque una pantalla pidiera el id de un recordatorio ajeno, Room no lo devuelve ni lo modifica.
@Dao
interface RecordatorioDao {

    // Listado completo (los marcados para borrar ya no se muestran)
    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estado_sync != 'PENDIENTE_BORRAR'")
    fun observarTodos(uid: String): Flow<List<RecordatorioEntity>>

    // Filtro por estado: solo los que no están completados
    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estado_sync != 'PENDIENTE_BORRAR' AND completado = 0")
    fun observarSinCompletar(uid: String): Flow<List<RecordatorioEntity>>

    // Un recordatorio por identificador (pantalla de detalle)
    @Query("SELECT * FROM recordatorios WHERE id = :id AND uid = :uid AND estado_sync != 'PENDIENTE_BORRAR'")
    fun observarPorId(id: String, uid: String): Flow<RecordatorioEntity?>

    // Cuántos cambios faltan enviar (se muestra en el listado y al cerrar sesión)
    @Query("SELECT COUNT(*) FROM recordatorios WHERE uid = :uid AND estado_sync IN ('PENDIENTE', 'PENDIENTE_BORRAR', 'ERROR')")
    fun observarCantidadSinSincronizar(uid: String): Flow<Int>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND uid = :uid")
    suspend fun buscarPorId(id: String, uid: String): RecordatorioEntity?

    // Cola de operaciones pendientes de la cuenta, de la más vieja a la más nueva
    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estado_sync IN ('PENDIENTE', 'PENDIENTE_BORRAR') ORDER BY actualizado_en ASC")
    suspend fun buscarPendientes(uid: String): List<RecordatorioEntity>

    @Query("SELECT id FROM recordatorios WHERE uid = :uid AND estado_sync = 'SINCRONIZADO'")
    suspend fun buscarIdsSincronizados(uid: String): List<String>

    // Inserta si el id no existe o actualiza si ya existe (sirve para crear y para editar).
    // El uid viaja dentro de la entidad: lo completa el repositorio con la cuenta activa.
    @Upsert
    suspend fun guardar(recordatorio: RecordatorioEntity)

    @Query("UPDATE recordatorios SET completado = :completado, fecha_completado = :fechaCompletado, actualizado_en = :ahora, estado_sync = 'PENDIENTE' WHERE id = :id AND uid = :uid")
    suspend fun cambiarCompletado(id: String, uid: String, completado: Boolean, fechaCompletado: Long?, ahora: Long): Int

    // Borrado "suave": se marca y se oculta; se borra de verdad cuando la nube lo confirma
    @Query("UPDATE recordatorios SET estado_sync = 'PENDIENTE_BORRAR', actualizado_en = :ahora WHERE id = :id AND uid = :uid")
    suspend fun marcarParaBorrar(id: String, uid: String, ahora: Long): Int

    // Marca como sincronizado solo si nadie lo modificó mientras se enviaba
    @Query("UPDATE recordatorios SET estado_sync = 'SINCRONIZADO' WHERE id = :id AND uid = :uid AND actualizado_en = :actualizadoEn")
    suspend fun marcarSincronizado(id: String, uid: String, actualizadoEn: Long)

    @Query("UPDATE recordatorios SET estado_sync = 'ERROR' WHERE id = :id AND uid = :uid")
    suspend fun marcarConError(id: String, uid: String)

    @Query("DELETE FROM recordatorios WHERE id = :id AND uid = :uid")
    suspend fun eliminarDefinitivo(id: String, uid: String)

    // Al cerrar sesión: se borran del teléfono los recordatorios de esa cuenta (en la nube siguen)
    @Query("DELETE FROM recordatorios WHERE uid = :uid")
    suspend fun eliminarDeCuenta(uid: String): Int

    // Al iniciar sesión: se borra lo que haya quedado de otras cuentas (o sin dueño, de la versión 1)
    @Query("DELETE FROM recordatorios WHERE uid != :uid")
    suspend fun eliminarDeOtrasCuentas(uid: String): Int

    // Para el registro de pruebas: cuántas filas hay en total y de cuántas cuentas
    @Query("SELECT COUNT(*) FROM recordatorios")
    suspend fun contarTodas(): Int
}
