package com.example.recordatorios.data.remoto

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TAG = "FuenteFirestore"
private const val TIEMPO_MAXIMO_MS = 15_000L

// Única clase que usa Cloud Firestore. Traduce documentos y excepciones a tipos propios de la app.
class FuenteRemotaFirestore(private val db: FirebaseFirestore) : FuenteRemotaRecordatorios {

    // users/{uid}/reminders: cada cuenta tiene su propia subcolección
    private fun coleccion(uid: String): CollectionReference =
        db.collection(COLECCION_USUARIOS).document(uid).collection(COLECCION_RECORDATORIOS)

    override suspend fun listar(uid: String): ResultadoRemoto<List<RecordatorioRemoto>> = llamar("listar") {
        // Source.SERVER: se pide al servidor, no a la caché. Sin red falla (UNAVAILABLE) y el repositorio
        // lo informa como "sin conexión"; la lista que ve la persona sigue saliendo de Room.
        val documentos = coleccion(uid).get(Source.SERVER).await().documents
        documentos.mapNotNull { doc ->
            RecordatorioRemoto.desdeMapa(doc.id, doc.data)
                ?: null.also { Log.w(TAG, "Documento ${doc.id} con formato inválido: se ignora") }
        }
    }

    override suspend fun guardar(uid: String, recordatorio: RecordatorioRemoto): ResultadoRemoto<Unit> =
        llamar("guardar") {
            // set() sobre un id fijo: crea el documento o lo reemplaza. Un reintento escribe el mismo documento.
            val datos = recordatorio.aMapa() + (RecordatorioRemoto.CAMPO_SINCRONIZADO_EN to FieldValue.serverTimestamp())
            coleccion(uid).document(recordatorio.id).set(datos).await()
        }

    override suspend fun eliminar(uid: String, id: String): ResultadoRemoto<Unit> = llamar("eliminar") {
        // delete() de un documento inexistente no falla: el borrado es idempotente
        coleccion(uid).document(id).delete().await()
        Unit
    }

    private suspend fun <T> llamar(operacion: String, bloque: suspend () -> T): ResultadoRemoto<T> =
        try {
            ResultadoRemoto.Exito(withTimeout(TIEMPO_MAXIMO_MS) { bloque() })
        } catch (e: TimeoutCancellationException) {
            // Va antes que CancellationException porque es una subclase de ella
            Log.w(TAG, "$operacion: sin respuesta en ${TIEMPO_MAXIMO_MS / 1000} s")
            ResultadoRemoto.Timeout
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "$operacion: ${e.code}")
            traducir(e.code)
        } catch (e: Exception) {
            Log.e(TAG, "$operacion: error inesperado", e)
            ResultadoRemoto.DatosInvalidos(e)
        }

    companion object {
        const val COLECCION_USUARIOS = "users"
        const val COLECCION_RECORDATORIOS = "reminders"

        fun traducir(codigo: Code): ResultadoRemoto<Nothing> = when (codigo) {
            Code.UNAVAILABLE -> ResultadoRemoto.SinConexion
            Code.DEADLINE_EXCEEDED -> ResultadoRemoto.Timeout
            Code.UNAUTHENTICATED -> ResultadoRemoto.NoAutenticado
            Code.PERMISSION_DENIED -> ResultadoRemoto.Rechazado("Las reglas de seguridad rechazaron la operación")
            Code.INVALID_ARGUMENT, Code.FAILED_PRECONDITION, Code.OUT_OF_RANGE ->
                ResultadoRemoto.Rechazado("La nube rechazó los datos ($codigo)")
            else -> ResultadoRemoto.ErrorTemporal("La nube tuvo un error ($codigo)")
        }
    }
}
