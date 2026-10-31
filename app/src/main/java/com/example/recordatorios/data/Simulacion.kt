package com.example.recordatorios.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Situaciones que se pueden forzar para comprobar cada estado de la interfaz
enum class ModoSimulacion(val etiqueta: String) {
    Normal("Normal"),
    SinDatos("Sin datos"),
    ErrorAlCargar("Error al cargar"),
    ErrorAlGuardar("Error al guardar")
}

// Guarda el modo elegido; lo leen el repositorio en memoria y el menú del listado
class ControlSimulacion {
    private val _modo = MutableStateFlow(ModoSimulacion.Normal)
    val modo: StateFlow<ModoSimulacion> = _modo.asStateFlow()

    fun cambiar(modo: ModoSimulacion) {
        _modo.value = modo
    }
}
