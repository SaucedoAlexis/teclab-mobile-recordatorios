package com.example.recordatorios.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.ErrorAuth
import com.example.recordatorios.data.auth.ResultadoAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModoAcceso { INICIAR_SESION, REGISTRARSE }

// Errores de validación del formulario (antes de llamar al servicio)
enum class ErrorCampo { VACIO, EMAIL_FORMATO, CONTRASENA_CORTA, NO_COINCIDEN }

// Estado del envío: una sola alternativa por vez
sealed interface EstadoAcceso {
    data object Editando : EstadoAcceso
    data object EnCurso : EstadoAcceso                       // "autenticación en curso"
    data class Error(val motivo: ErrorAuth) : EstadoAcceso   // error devuelto por el servicio
}

data class AccesoUiState(
    val modo: ModoAcceso = ModoAcceso.INICIAR_SESION,
    val email: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val mostrarErrores: Boolean = false,
    val estado: EstadoAcceso = EstadoAcceso.Editando
) {
    val errorEmail: ErrorCampo?
        get() = when {
            email.isBlank() -> ErrorCampo.VACIO
            !FORMATO_EMAIL.matches(email.trim()) -> ErrorCampo.EMAIL_FORMATO
            else -> null
        }

    val errorContrasena: ErrorCampo?
        get() = when {
            contrasena.isEmpty() -> ErrorCampo.VACIO
            // Al iniciar sesión no se valida el largo: se deja que el servicio responda
            modo == ModoAcceso.REGISTRARSE && contrasena.length < LARGO_MINIMO -> ErrorCampo.CONTRASENA_CORTA
            else -> null
        }

    val errorConfirmacion: ErrorCampo?
        get() = if (modo == ModoAcceso.REGISTRARSE && confirmacion != contrasena) ErrorCampo.NO_COINCIDEN else null

    val enCurso: Boolean get() = estado == EstadoAcceso.EnCurso
    val esValido: Boolean get() = errorEmail == null && errorContrasena == null && errorConfirmacion == null
    val puedeEnviar: Boolean get() = esValido && !enCurso

    companion object {
        // Firebase exige al menos 6 caracteres
        const val LARGO_MINIMO = 6
        private val FORMATO_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

class AccesoViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AccesoUiState())
    val uiState: StateFlow<AccesoUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) = editar { it.copy(email = valor) }
    fun onContrasenaChange(valor: String) = editar { it.copy(contrasena = valor) }
    fun onConfirmacionChange(valor: String) = editar { it.copy(confirmacion = valor) }

    fun onCambiarModo() = _uiState.update {
        val nuevo = if (it.modo == ModoAcceso.INICIAR_SESION) ModoAcceso.REGISTRARSE else ModoAcceso.INICIAR_SESION
        it.copy(modo = nuevo, confirmacion = "", mostrarErrores = false, estado = EstadoAcceso.Editando)
    }

    fun onEnviar() {
        val actual = _uiState.value
        if (!actual.esValido) {
            _uiState.update { it.copy(mostrarErrores = true) }
            return
        }
        if (actual.enCurso) return
        _uiState.update { it.copy(estado = EstadoAcceso.EnCurso) }
        viewModelScope.launch {
            val resultado = when (actual.modo) {
                ModoAcceso.INICIAR_SESION -> auth.iniciarSesion(actual.email, actual.contrasena)
                ModoAcceso.REGISTRARSE -> auth.registrar(actual.email, actual.contrasena)
            }
            _uiState.update {
                when (resultado) {
                    // Con éxito no hace falta navegar desde acá: la raíz de la app observa la sesión y cambia de pantalla.
                    // El formulario vuelve a su estado inicial (sin correo ni contraseña en memoria), así al cerrar
                    // sesión la próxima persona encuentra la pantalla de inicio vacía.
                    ResultadoAuth.Exito -> AccesoUiState()
                    is ResultadoAuth.Error -> it.copy(estado = EstadoAcceso.Error(resultado.motivo))
                }
            }
        }
    }

    private fun editar(cambio: (AccesoUiState) -> AccesoUiState) = _uiState.update {
        // Al escribir de nuevo desaparece el error del servicio
        val nuevo = cambio(it)
        if (nuevo.estado is EstadoAcceso.Error) nuevo.copy(estado = EstadoAcceso.Editando) else nuevo
    }
}
