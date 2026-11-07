package com.example.recordatorios.data.remoto

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

// Todo lo que puede pasar al llamar al servicio. Con un sealed interface el repositorio
// está obligado (por el when) a decidir qué hacer en cada caso.
sealed interface ResultadoRemoto<out T> {
    data class Exito<T>(val datos: T) : ResultadoRemoto<T>
    data object SinConexion : ResultadoRemoto<Nothing>                 // no se pudo conectar
    data object Timeout : ResultadoRemoto<Nothing>                     // el servidor no respondió a tiempo
    data class ErrorHttp(val codigo: Int) : ResultadoRemoto<Nothing>   // 4xx o 5xx
    data class DatosInvalidos(val causa: Throwable) : ResultadoRemoto<Nothing>
}

// Única clase que usa Retrofit. Traduce excepciones y códigos HTTP a ResultadoRemoto.
// Las pantallas nunca ven códigos HTTP ni excepciones de red.
class FuenteRemota(private val api: RecordatoriosApi) {

    suspend fun listar(): ResultadoRemoto<List<RecordatorioDto>> = conCuerpo { api.listar() }

    suspend fun guardar(dto: RecordatorioDto): ResultadoRemoto<RecordatorioDto> = conCuerpo { api.guardar(dto.id, dto) }

    // El DELETE responde 204 (sin cuerpo). Un 404 significa que el servidor ya no lo tiene:
    // para un borrado eso también cuenta como éxito.
    suspend fun eliminar(id: String): ResultadoRemoto<Unit> = capturarErrores {
        val respuesta = api.eliminar(id)
        if (respuesta.isSuccessful || respuesta.code() == 404) ResultadoRemoto.Exito(Unit)
        else ResultadoRemoto.ErrorHttp(respuesta.code())
    }

    // Para las llamadas que tienen que devolver datos
    private suspend fun <T : Any> conCuerpo(llamada: suspend () -> Response<T>): ResultadoRemoto<T> =
        capturarErrores {
            val respuesta = llamada()
            val cuerpo = respuesta.body()
            when {
                !respuesta.isSuccessful -> ResultadoRemoto.ErrorHttp(respuesta.code())
                // Un 2xx sin el cuerpo esperado también es un problema
                cuerpo == null -> ResultadoRemoto.DatosInvalidos(IllegalStateException("La respuesta no tiene datos"))
                else -> ResultadoRemoto.Exito(cuerpo)
            }
        }

    // Convierte las excepciones de red en resultados controlados
    private suspend fun <T> capturarErrores(bloque: suspend () -> ResultadoRemoto<T>): ResultadoRemoto<T> =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e                       // si se cancela la coroutine no es un error de red
        } catch (e: SocketTimeoutException) {
            ResultadoRemoto.Timeout       // va antes que IOException porque es un caso particular de ella
        } catch (e: IOException) {
            ResultadoRemoto.SinConexion
        } catch (e: SerializationException) {
            ResultadoRemoto.DatosInvalidos(e)
        }
}
