package com.example.recordatorios.ui.lista

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.EstadoSincronizacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.ResultadoSincronizacion
import com.example.recordatorios.ui.formatearHora
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Depende de la interfaz RecordatorioRepository. No sabe nada de Room, Retrofit ni DataStore.
class ListaViewModel(private val repositorio: RecordatorioRepository) : ViewModel() {

    // Privado y modificable adentro; hacia la interfaz solo se expone la versión de lectura
    private val _uiState = MutableStateFlow(ListaUiState())
    val uiState: StateFlow<ListaUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    init {
        cargar()
        observarSincronizacion()
    }

    // Observa Room: cada cambio en la tabla llega solo, sin volver a pedirlo
    fun cargar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            _uiState.update { it.copy(contenido = ContenidoLista.Cargando) }
            repositorio.observarRecordatorios()
                .map { lista ->
                    if (lista.isEmpty()) ContenidoLista.Vacio else ContenidoLista.ConDatos(lista)
                }
                .catch { error ->
                    // Error de lectura: es distinto de "no hay recordatorios", por eso tiene su propio estado
                    Log.e("ListaViewModel", "No se pudo leer Room", error)
                    emit(ContenidoLista.Error("No se pudieron leer los recordatorios guardados en el teléfono."))
                }
                .collect { contenido -> _uiState.update { it.copy(contenido = contenido) } }
        }
    }

    // Combina tres flujos del repositorio en la parte del estado que muestra la barra de avisos
    private fun observarSincronizacion() {
        viewModelScope.launch {
            combine(
                repositorio.hayConexion,
                repositorio.observarCantidadSinSincronizar(),
                repositorio.estadoSincronizacion
            ) { hayConexion, sinSincronizar, estado ->
                Triple(hayConexion, sinSincronizar, estado)
            }.collect { (hayConexion, sinSincronizar, estado) ->
                val (mensaje, esError) = mensajeDe(estado)
                // copy() solo cambia estos campos; el contenido de la lista queda igual
                _uiState.update {
                    it.copy(
                        hayConexion = hayConexion,
                        sinSincronizar = sinSincronizar,
                        sincronizando = estado is EstadoSincronizacion.EnCurso,
                        mensajeSincronizacion = mensaje,
                        mensajeEsError = esError
                    )
                }
            }
        }
    }

    // Sincronización manual (botón de la barra superior)
    fun onSincronizar() {
        if (_uiState.value.sincronizando || !_uiState.value.hayConexion) return
        viewModelScope.launch { repositorio.sincronizar() }
    }

    fun onReintentar() = cargar()

    // Texto para la persona usuaria. Los detalles técnicos quedan en Logcat.
    private fun mensajeDe(estado: EstadoSincronizacion): Pair<String?, Boolean> = when (estado) {
        EstadoSincronizacion.Inactiva, EstadoSincronizacion.EnCurso -> null to false
        is EstadoSincronizacion.Terminada -> when (val resultado = estado.resultado) {
            is ResultadoSincronizacion.Exito ->
                if (resultado.rechazados > 0) {
                    "Sincronizado a las ${formatearHora(estado.hora)}. El servidor rechazó ${resultado.rechazados} recordatorio(s)." to true
                } else {
                    "Sincronizado a las ${formatearHora(estado.hora)}" to false
                }
            ResultadoSincronizacion.SinConexion ->
                "No se pudo conectar con el servidor. Los cambios quedan guardados en el teléfono." to true
            is ResultadoSincronizacion.ErrorReintentable ->
                "${resultado.motivo}. Los datos guardados se siguen mostrando y se va a reintentar." to true
            is ResultadoSincronizacion.ErrorPermanente ->
                "${resultado.motivo}. No se reintenta automáticamente." to true
            ResultadoSincronizacion.EnCurso -> null to false
        }
    }
}
