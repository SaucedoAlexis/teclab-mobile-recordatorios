package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.ResultadoOperacion
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

// idEditar = null: se crea un recordatorio nuevo. Con un id: se edita ese recordatorio.
class CrearViewModel(
    private val repositorio: RecordatorioRepository,
    private val idEditar: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        validar(
            CrearUiState(
                categorias = repositorio.obtenerCategorias(),
                esEdicion = idEditar != null,
                cargandoRegistro = idEditar != null
            )
        )
    )
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    // Registro original al editar: se conservan el id y la fecha de creación
    private var original: Recordatorio? = null

    init {
        if (idEditar != null) cargarParaEditar(idEditar)
    }

    private fun cargarParaEditar(id: String) {
        viewModelScope.launch {
            val recordatorio = repositorio.buscarRecordatorio(id)
            original = recordatorio
            _uiState.update { actual ->
                if (recordatorio == null) {
                    actual.copy(cargandoRegistro = false, envio = EstadoEnvio.Error("No se encontró el recordatorio a editar."))
                } else {
                    validar(
                        actual.copy(
                            titulo = recordatorio.titulo,
                            descripcion = recordatorio.descripcion.orEmpty(),
                            categoria = recordatorio.categoria,
                            prioridad = recordatorio.prioridad,
                            cargandoRegistro = false
                        )
                    )
                }
            }
        }
    }

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

        val titulo = actual.titulo.trim()
        val descripcion = actual.descripcion.trim().ifEmpty { null }
        // Al editar se parte del original; al crear, el id vacío le indica al repositorio que genere uno
        val aGuardar = original?.copy(
            titulo = titulo, descripcion = descripcion, categoria = categoria, prioridad = prioridad
        ) ?: Recordatorio(
            id = "",
            titulo = titulo,
            categoria = categoria,
            prioridad = prioridad,
            estado = EstadoRecordatorio.Pendiente,
            descripcion = descripcion
        )

        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            // Se guarda en Room (funciona sin conexión); la sincronización la decide el repositorio
            val resultado = repositorio.guardarRecordatorio(aGuardar)
            _uiState.update {
                when (resultado) {
                    ResultadoOperacion.Exito -> it.copy(envio = EstadoEnvio.Exito)
                    // Si falla, el formulario conserva lo escrito y se puede volver a intentar
                    is ResultadoOperacion.Error -> it.copy(envio = EstadoEnvio.Error(resultado.mensaje))
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
