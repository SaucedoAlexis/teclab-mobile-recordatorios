package com.example.recordatorios.ui.lista

import com.example.recordatorios.domain.Recordatorio

// Lo que se ve en el centro de la pantalla: un solo caso por vez (nunca carga y error juntos)
sealed interface ContenidoLista {
    data object Cargando : ContenidoLista
    data object Vacio : ContenidoLista
    data class ConDatos(val recordatorios: List<Recordatorio>) : ContenidoLista
    data class Error(val mensaje: String) : ContenidoLista
}

// Estado completo del listado: el contenido (de Room) más la información de conexión y sincronización.
// Se separan porque se puede estar sin conexión y aun así tener datos para mostrar.
data class ListaUiState(
    val contenido: ContenidoLista = ContenidoLista.Cargando,
    val hayConexion: Boolean = true,
    val sinSincronizar: Int = 0,
    val sincronizando: Boolean = false,
    val mensajeSincronizacion: String? = null,
    val mensajeEsError: Boolean = false
)
