package com.example.recordatorios.ui.navegacion

import kotlinx.serialization.Serializable

// Rutas tipadas: los destinos sin argumentos son objetos; el detalle lleva solo el id
@Serializable
object RutaLista

@Serializable
object RutaCrear

@Serializable
data class RutaDetalle(val id: Long)
