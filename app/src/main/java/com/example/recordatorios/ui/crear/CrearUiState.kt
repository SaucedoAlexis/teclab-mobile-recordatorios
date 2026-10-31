package com.example.recordatorios.ui.crear

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.validation.ErroresFormulario

// Estado del envío: una sola alternativa por vez (nunca "enviando" y "éxito" juntos)
sealed interface EstadoEnvio {
    data object Editando : EstadoEnvio
    data object Enviando : EstadoEnvio
    data object Exito : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

// Estado inmutable del formulario: valores de los campos, mensajes de validación y envío
data class CrearUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val categorias: List<Categoria> = emptyList(),
    val errores: ErroresFormulario = ErroresFormulario(),
    val mostrarErrores: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Editando
) {
    // Datos derivados: se calculan, no se guardan aparte
    val enviando: Boolean get() = envio == EstadoEnvio.Enviando
    val puedeGuardar: Boolean get() = errores.esValido && !enviando

    // Los errores aparecen recién cuando el usuario empezó a completar el formulario
    val erroresVisibles: ErroresFormulario get() = if (mostrarErrores) errores else ErroresFormulario()
}
