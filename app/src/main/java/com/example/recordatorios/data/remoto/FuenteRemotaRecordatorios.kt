package com.example.recordatorios.data.remoto

// Todo lo que puede pasar al hablar con la nube. Con un sealed interface el repositorio
// está obligado (por el when) a decidir qué hacer en cada caso. No menciona a Firebase:
// el repositorio no sabe qué proveedor hay detrás.
sealed interface ResultadoRemoto<out T> {
    data class Exito<T>(val datos: T) : ResultadoRemoto<T>
    data object SinConexion : ResultadoRemoto<Nothing>                      // no se pudo llegar al servicio
    data object Timeout : ResultadoRemoto<Nothing>                          // no respondió a tiempo
    data object NoAutenticado : ResultadoRemoto<Nothing>                    // la sesión dejó de ser válida
    data class Rechazado(val motivo: String) : ResultadoRemoto<Nothing>     // sin permiso o datos inválidos: reintentar no sirve
    data class ErrorTemporal(val motivo: String) : ResultadoRemoto<Nothing> // falla del servicio que puede resolverse sola
    data class DatosInvalidos(val causa: Throwable) : ResultadoRemoto<Nothing>
}

// Contrato de la fuente remota. Toda operación recibe el uid: los datos de la nube siempre
// están asociados a una identidad (users/{uid}/reminders/{id}).
interface FuenteRemotaRecordatorios {
    suspend fun listar(uid: String): ResultadoRemoto<List<RecordatorioRemoto>>

    // Crea o reemplaza el documento con ese id. Repetirlo no crea copias (operación idempotente).
    suspend fun guardar(uid: String, recordatorio: RecordatorioRemoto): ResultadoRemoto<Unit>

    // Borrar un documento que ya no existe también cuenta como éxito
    suspend fun eliminar(uid: String, id: String): ResultadoRemoto<Unit>
}
