package com.example.recordatorios.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.Tema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AjustesViewModel(private val repositorio: PreferenciasRepository) : ViewModel() {

    // null mientras DataStore todavía no respondió (se muestra "Cargando…")
    val preferencias: StateFlow<PreferenciasUsuario?> = repositorio.preferencias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    // Cada cambio se guarda con una función suspendida dentro de viewModelScope (no bloquea la interfaz)
    fun onTemaChange(tema: Tema) {
        viewModelScope.launch { repositorio.cambiarTema(tema) }
    }

    fun onMostrarCompletadosChange(mostrar: Boolean) {
        viewModelScope.launch { repositorio.cambiarMostrarCompletados(mostrar) }
    }

    fun onOrdenChange(orden: OrdenListado) {
        viewModelScope.launch { repositorio.cambiarOrden(orden) }
    }

    // Solo para la prueba del dato inválido (el botón se ve únicamente en la versión debug)
    fun onGuardarOrdenInvalido() {
        viewModelScope.launch { repositorio.guardarOrdenInvalidoDePrueba() }
    }
}
