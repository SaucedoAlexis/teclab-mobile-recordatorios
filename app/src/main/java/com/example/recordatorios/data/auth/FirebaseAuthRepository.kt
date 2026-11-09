package com.example.recordatorios.data.auth

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

// Única clase que usa Firebase Authentication. Traduce sus objetos y excepciones a los tipos propios de la app.
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Comprobando)
    override val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    // Motivo del próximo cierre, para que el listener sepa por qué se quedó sin usuario
    @Volatile
    private var motivoCierre: MotivoSinSesion? = null

    init {
        // Firebase avisa cada vez que cambia el usuario: al registrar el listener (sesión guardada),
        // al iniciar sesión y al cerrarla. Firebase conserva la sesión entre aperturas de la app.
        auth.addAuthStateListener { firebaseAuth ->
            val usuario = firebaseAuth.currentUser
            _estado.value = if (usuario != null) {
                motivoCierre = null
                EstadoSesion.Activa(usuario.aUsuarioSesion())
            } else {
                EstadoSesion.SinSesion(motivoCierre)
            }
        }
    }

    override suspend fun iniciarSesion(email: String, contrasena: String): ResultadoAuth = ejecutar {
        auth.signInWithEmailAndPassword(email.trim(), contrasena).await()
    }

    override suspend fun registrar(email: String, contrasena: String): ResultadoAuth = ejecutar {
        // Si sale bien, Firebase deja la cuenta nueva con la sesión iniciada
        auth.createUserWithEmailAndPassword(email.trim(), contrasena).await()
    }

    override suspend fun cerrarSesion() {
        motivoCierre = MotivoSinSesion.CERRADA_POR_USUARIO
        auth.signOut()
    }

    override suspend fun verificarSesion() {
        val usuario = auth.currentUser ?: return
        try {
            // reload() pide al servidor los datos actuales de la cuenta. Si se deshabilitó o se eliminó,
            // falla con FirebaseAuthInvalidUserException aunque el teléfono todavía tenga la sesión guardada.
            usuario.reload().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w(TAG, "La sesión guardada ya no es válida: ${e.errorCode}")
            invalidarSesion()
        } catch (e: FirebaseNetworkException) {
            // Sin conexión no se puede comprobar: se mantiene la sesión (la app funciona offline)
            Log.i(TAG, "No se pudo verificar la sesión sin conexión")
        } catch (e: Exception) {
            Log.w(TAG, "Error al verificar la sesión", e)
        }
    }

    override fun invalidarSesion() {
        motivoCierre = MotivoSinSesion.SESION_INVALIDA
        auth.signOut()
    }

    private suspend fun ejecutar(operacion: suspend () -> Unit): ResultadoAuth = try {
        operacion()
        ResultadoAuth.Exito
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Error de autenticación: ${e::class.simpleName}")
        ResultadoAuth.Error(traducir(e))
    }

    companion object {
        private const val TAG = "AuthRepository"

        // El orden importa: FirebaseAuthWeakPasswordException es una subclase de InvalidCredentials
        fun traducir(e: Exception): ErrorAuth = when (e) {
            is FirebaseAuthWeakPasswordException -> ErrorAuth.CONTRASENA_DEBIL
            is FirebaseAuthUserCollisionException -> ErrorAuth.EMAIL_EN_USO
            is FirebaseAuthInvalidCredentialsException ->
                if (e.errorCode == "ERROR_INVALID_EMAIL") ErrorAuth.EMAIL_INVALIDO else ErrorAuth.CREDENCIALES_INVALIDAS
            is FirebaseAuthInvalidUserException ->
                if (e.errorCode == "ERROR_USER_DISABLED") ErrorAuth.CUENTA_DESHABILITADA else ErrorAuth.CREDENCIALES_INVALIDAS
            is FirebaseTooManyRequestsException -> ErrorAuth.DEMASIADOS_INTENTOS
            is FirebaseNetworkException -> ErrorAuth.SIN_CONEXION
            else -> ErrorAuth.DESCONOCIDO
        }
    }
}

private fun FirebaseUser.aUsuarioSesion() = UsuarioSesion(uid = uid, email = email)
