package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.validation.validarFormulario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class CrearViewModel(private val repositorio: RecordatorioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(validar(CrearUiState(categorias = repositorio.obtenerCategorias())))
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    // Eventos que envía la interfaz: cada uno produce una nueva instancia con copy()
    fun onTituloChange(titulo: String) = actualizar { it.copy(titulo = titulo) }
    fun onDescripcionChange(descripcion: String) = actualizar { it.copy(descripcion = descripcion) }
    fun onCategoriaChange(categoria: Categoria) = actualizar { it.copy(categoria = categoria) }
    fun onPrioridadChange(prioridad: Prioridad) = actualizar { it.copy(prioridad = prioridad) }

    fun onGuardar() {
        val actual = _uiState.value
        if (!actual.puedeGuardar) return
        // Con el formulario válido, categoría y prioridad no son null (sin usar !!)
        val categoria = actual.categoria ?: return
        val prioridad = actual.prioridad ?: return

        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            try {
                repositorio.guardarRecordatorio(
                    Recordatorio(
                        id = 0,
                        titulo = actual.titulo.trim(),
                        categoria = categoria,
                        prioridad = prioridad,
                        estado = EstadoRecordatorio.Pendiente,
                        descripcion = actual.descripcion.trim().ifEmpty { null }
                    )
                )
                _uiState.update { it.copy(envio = EstadoEnvio.Exito) }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(envio = EstadoEnvio.Error("No se pudo guardar el recordatorio. Intentá nuevamente."))
                }
            }
        }
    }

    // Aplica el cambio, vuelve a validar y marca que el usuario ya interactuó con el formulario
    private fun actualizar(cambio: (CrearUiState) -> CrearUiState) {
        _uiState.update { actual ->
            validar(cambio(actual)).copy(mostrarErrores = true, envio = EstadoEnvio.Editando)
        }
    }

    private fun validar(estado: CrearUiState): CrearUiState = estado.copy(
        errores = validarFormulario(
            estado.titulo,
            estado.descripcion,
            estado.categoria,
            estado.prioridad,
            estado.categorias
        )
    )
}
