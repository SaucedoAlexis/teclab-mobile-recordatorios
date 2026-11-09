package com.example.recordatorios.data.auth

import kotlinx.coroutines.flow.StateFlow

// Identidad de la persona que inició sesión. Solo lo que la app necesita: el uid (para asociar los datos)
// y el correo (para mostrarlo). La contraseña y los tokens los administra Firebase; la app no los guarda.
data class UsuarioSesion(val uid: String, val email: String?)

// Estado de la sesión: una sola alternativa por vez
sealed interface EstadoSesion {
    // Al abrir la app, mientras se lee la sesión guardada por Firebase
    data object Comprobando : EstadoSesion

    // No hay sesión. motivo indica por qué, para avisarle a la persona (null = nunca inició sesión)
    data class SinSesion(val motivo: MotivoSinSesion? = null) : EstadoSesion

    data class Activa(val usuario: UsuarioSesion) : EstadoSesion
}

enum class MotivoSinSesion {
    CERRADA_POR_USUARIO,
    SESION_INVALIDA      // la cuenta se deshabilitó o eliminó, o la credencial dejó de ser válida
}

// Errores propios de la app: la interfaz no conoce las excepciones de Firebase
enum class ErrorAuth {
    CREDENCIALES_INVALIDAS,
    EMAIL_INVALIDO,
    EMAIL_EN_USO,
    CONTRASENA_DEBIL,
    CUENTA_DESHABILITADA,
    DEMASIADOS_INTENTOS,
    SIN_CONEXION,
    DESCONOCIDO
}

sealed interface ResultadoAuth {
    data object Exito : ResultadoAuth
    data class Error(val motivo: ErrorAuth) : ResultadoAuth
}

// Contrato de autenticación. Los ViewModels dependen de esta interfaz, no de Firebase,
// así en las pruebas se puede usar una implementación simulada.
interface AuthRepository {
    val estado: StateFlow<EstadoSesion>

    suspend fun iniciarSesion(email: String, contrasena: String): ResultadoAuth
    suspend fun registrar(email: String, contrasena: String): ResultadoAuth
    suspend fun cerrarSesion()

    // Consulta al servicio si la sesión guardada en el teléfono sigue siendo válida
    suspend fun verificarSesion()

    // La usa el repositorio de datos cuando la nube responde que la sesión ya no es válida
    fun invalidarSesion()
}
