package com.example.recordatorios.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.ControlSimulacion
import com.example.recordatorios.data.ModoSimulacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.domain.ordenarPorPrioridad
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Depende de la interfaz RecordatorioRepository, no de la implementación en memoria
class ListaViewModel(
    private val repositorio: RecordatorioRepository,
    private val simulacion: ControlSimulacion
) : ViewModel() {

    // Privado y modificable adentro; hacia la interfaz solo se expone la versión de lectura
    private val _uiState = MutableStateFlow<ListaUiState>(ListaUiState.Cargando)
    val uiState: StateFlow<ListaUiState> = _uiState.asStateFlow()

    val modoSimulacion: StateFlow<ModoSimulacion> = simulacion.modo

    private var carga: Job? = null

    init {
        cargar()
    }

    // viewModelScope: la coroutine se cancela sola cuando el ViewModel se destruye
    fun cargar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            _uiState.update { ListaUiState.Cargando }
            repositorio.observarRecordatorios()
                .map { lista ->
                    if (lista.isEmpty()) ListaUiState.Vacio
                    else ListaUiState.ConDatos(lista.ordenarPorPrioridad())
                }
                .catch {
                    emit(ListaUiState.Error("No se pudieron cargar los recordatorios. Revisá la conexión e intentá de nuevo."))
                }
                .collect { nuevoEstado -> _uiState.update { nuevoEstado } }
        }
    }

    fun onReintentar() = cargar()

    fun onCambiarSimulacion(modo: ModoSimulacion) {
        simulacion.cambiar(modo)
        cargar()
    }
}
