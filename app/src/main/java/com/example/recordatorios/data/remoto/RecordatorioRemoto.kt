package com.example.recordatorios.data.remoto

// Representa un documento de Cloud Firestore: users/{uid}/reminders/{id}.
// Es el "contrato" con la nube: no es la entidad de Room ni el modelo de la app, y nunca llega a las pantallas.
//
// La conversión a y desde Map es manual (no se usa toObject() con reflexión): así el nombre de cada campo
// está escrito una sola vez acá y la minificación (R8) de la versión release no puede renombrarlos.
data class RecordatorioRemoto(
    val id: String,              // id del documento = mismo UUID que en Room
    val uid: String,             // dueño; las reglas de seguridad exigen que coincida con la ruta
    val titulo: String,
    val descripcion: String?,
    val categoria: String,
    val prioridad: String,       // "ALTA", "MEDIA" o "BAJA"
    val completado: Boolean,
    val fechaCompletado: Long?,
    val fechaCreacion: Long,
    val actualizadoEn: Long      // milisegundos; se usa para resolver conflictos (gana el más reciente)
) {
    fun aMapa(): Map<String, Any?> = mapOf(
        CAMPO_UID to uid,
        CAMPO_TITULO to titulo,
        CAMPO_DESCRIPCION to descripcion,
        CAMPO_CATEGORIA to categoria,
        CAMPO_PRIORIDAD to prioridad,
        CAMPO_COMPLETADO to completado,
        CAMPO_FECHA_COMPLETADO to fechaCompletado,
        CAMPO_FECHA_CREACION to fechaCreacion,
        CAMPO_ACTUALIZADO_EN to actualizadoEn
    )

    companion object {
        const val CAMPO_UID = "uid"
        const val CAMPO_TITULO = "titulo"
        const val CAMPO_DESCRIPCION = "descripcion"
        const val CAMPO_CATEGORIA = "categoria"
        const val CAMPO_PRIORIDAD = "prioridad"
        const val CAMPO_COMPLETADO = "completado"
        const val CAMPO_FECHA_COMPLETADO = "fechaCompletado"
        const val CAMPO_FECHA_CREACION = "fechaCreacion"
        const val CAMPO_ACTUALIZADO_EN = "actualizadoEn"
        // Lo completa el servidor de Firestore (hora del servidor); solo informativo
        const val CAMPO_SINCRONIZADO_EN = "sincronizadoEn"

        // Lee un documento. Si falta un campo obligatorio o tiene otro tipo, devuelve null
        // (el documento se ignora y se registra en Logcat) en lugar de cerrar la app.
        fun desdeMapa(id: String, datos: Map<String, Any?>?): RecordatorioRemoto? {
            if (datos == null) return null
            return RecordatorioRemoto(
                id = id,
                uid = datos[CAMPO_UID] as? String ?: return null,
                titulo = datos[CAMPO_TITULO] as? String ?: return null,
                descripcion = datos[CAMPO_DESCRIPCION] as? String,
                categoria = datos[CAMPO_CATEGORIA] as? String ?: return null,
                prioridad = datos[CAMPO_PRIORIDAD] as? String ?: "MEDIA",
                completado = datos[CAMPO_COMPLETADO] as? Boolean ?: false,
                // Firestore devuelve los enteros como Long; Number cubre también Double
                fechaCompletado = (datos[CAMPO_FECHA_COMPLETADO] as? Number)?.toLong(),
                fechaCreacion = (datos[CAMPO_FECHA_CREACION] as? Number)?.toLong() ?: return null,
                actualizadoEn = (datos[CAMPO_ACTUALIZADO_EN] as? Number)?.toLong() ?: return null
            )
        }
    }
}
