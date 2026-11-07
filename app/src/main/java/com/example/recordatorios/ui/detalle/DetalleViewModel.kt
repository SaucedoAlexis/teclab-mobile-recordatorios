package com.example.recordatorios.ui.detalle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.ResultadoOperacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Estado de la última acción del usuario en esta pantalla
private sealed interface Accion {
    data object Ninguna : Accion
    data object Procesando : Accion
    data class Fallo(val mensaje: String) : Accion
    data object Eliminado : Accion
}

// Recibe solo el id que llega por la ruta y observa ese registro en Room.
// Si se edita o se sincroniza, la pantalla se actualiza sola.
class DetalleViewModel(
    private val idRecordatorio: String,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val accion = MutableStateFlow<Accion>(Accion.Ninguna)

    // stateIn convierte el Flow en StateFlow. WhileSubscribed(5000): deja de observar Room
    // 5 segundos después de que la pantalla deja de estar visible.
    val uiState: StateFlow<DetalleUiState> =
        combine(repositorio.observarRecordatorio(idRecordatorio), accion) { recordatorio, accionActual ->
            when {
                accionActual is Accion.Eliminado -> DetalleUiState.Eliminado
                recordatorio == null -> DetalleUiState.Error("No se encontró el recordatorio solicitado.")
                else -> DetalleUiState.ConDatos(
                    recordatorio = recordatorio,
                    procesando = accionActual is Accion.Procesando,
                    mensajeError = (accionActual as? Accion.Fallo)?.mensaje
                )
            }
        }
            .catch { error ->
                Log.e("DetalleViewModel", "No se pudo leer el recordatorio", error)
                emit(DetalleUiState.Error("No se pudo leer el recordatorio guardado."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    // Actualización: cambia entre pendiente y completado
    fun onCambiarCompletado() {
        val actual = (uiState.value as? DetalleUiState.ConDatos)?.recordatorio ?: return
        viewModelScope.launch {
            accion.value = Accion.Procesando
            val resultado = repositorio.cambiarCompletado(actual.id, !actual.completado)
            accion.value = when (resultado) {
                ResultadoOperacion.Exito -> Accion.Ninguna
                is ResultadoOperacion.Error -> Accion.Fallo(resultado.mensaje)
            }
        }
    }

    fun onEliminar() {
        viewModelScope.launch {
            accion.value = Accion.Procesando
            accion.value = when (val resultado = repositorio.eliminarRecordatorio(idRecordatorio)) {
                ResultadoOperacion.Exito -> Accion.Eliminado
                is ResultadoOperacion.Error -> Accion.Fallo(resultado.mensaje)
            }
        }
    }
}
