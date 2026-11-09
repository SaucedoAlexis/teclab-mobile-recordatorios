package com.example.recordatorios.data.remoto

import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.EstadoSesion

// Herramienta de prueba (solo se muestra en la versión de desarrollo).
// Simula lo que haría un cliente modificado: usar la MISMA fuente remota de la app, pero apuntando
// a la ruta de otra cuenta (users/{otroUid}/reminders) o mandando un documento con otro dueño.
// La app nunca arma esas rutas por sí sola: sirve para comprobar que la protección real está en el servidor
// (reglas de Firestore) y no en lo que la interfaz muestra u oculta.
class PruebaAccesoCruzado(
    private val auth: AuthRepository,
    private val remota: FuenteRemotaRecordatorios
) {
    data class Intento(val descripcion: String, val resultado: ResultadoRemoto<*>)

    suspend fun ejecutar(uidAjeno: String): List<Intento> {
        val propio = (auth.estado.value as? EstadoSesion.Activa)?.usuario?.uid ?: return emptyList()
        val ahora = System.currentTimeMillis()
        val documento = RecordatorioRemoto(
            id = "prueba-acceso-cruzado", uid = uidAjeno, titulo = "Escrito por otra cuenta",
            descripcion = null, categoria = "Trabajo", prioridad = "ALTA", completado = false,
            fechaCompletado = null, fechaCreacion = ahora, actualizadoEn = ahora
        )
        return listOf(
            // 1. Leer los recordatorios de otra cuenta
            Intento("Leer users/$uidAjeno/reminders", remota.listar(uidAjeno)),
            // 2. Crear un documento en la colección de otra cuenta
            Intento("Escribir en users/$uidAjeno/reminders", remota.guardar(uidAjeno, documento)),
            // 3. Borrar un documento de otra cuenta
            Intento("Borrar en users/$uidAjeno/reminders", remota.eliminar(uidAjeno, documento.id)),
            // 4. Escribir en la ruta PROPIA, pero declarando a otra cuenta como dueña (falla la validación de datos)
            Intento("Escribir en mi ruta con uid ajeno", remota.guardar(propio, documento))
        )
    }
}
