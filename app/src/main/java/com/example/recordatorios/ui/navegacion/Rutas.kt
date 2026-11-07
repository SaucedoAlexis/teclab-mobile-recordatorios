package com.example.recordatorios.ui.navegacion

import kotlinx.serialization.Serializable

// Rutas tipadas: los destinos sin argumentos son objetos; los demás llevan solo el id
@Serializable
object RutaLista

// El mismo formulario sirve para crear (id = null) y para editar (id del recordatorio)
@Serializable
data class RutaFormulario(val id: String? = null)

@Serializable
data class RutaDetalle(val id: String)

@Serializable
object RutaAjustes
