package com.example.recordatorios

import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.ErrorAuth
import com.example.recordatorios.data.auth.EstadoSesion
import com.example.recordatorios.data.auth.ResultadoAuth
import com.example.recordatorios.ui.acceso.AccesoViewModel
import com.example.recordatorios.ui.acceso.EstadoAcceso
import com.example.recordatorios.ui.acceso.ModoAcceso
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Repositorio de autenticación simulado: controla la respuesta y cuenta las llamadas.
// Así la prueba es reproducible (no depende de Firebase ni de la red).
private class AuthSimulado : AuthRepository {
    override val estado: StateFlow<EstadoSesion> = MutableStateFlow(EstadoSesion.SinSesion())
    var respuesta: ResultadoAuth = ResultadoAuth.Exito
    var pendiente: CompletableDeferred<ResultadoAuth>? = null   // para observar el estado "en curso"
    var llamadasInicio = 0
    var llamadasRegistro = 0

    override suspend fun iniciarSesion(email: String, contrasena: String): ResultadoAuth {
        llamadasInicio++
        return pendiente?.await() ?: respuesta
    }

    override suspend fun registrar(email: String, contrasena: String): ResultadoAuth {
        llamadasRegistro++
        return pendiente?.await() ?: respuesta
    }

    override suspend fun cerrarSesion() = Unit
    override suspend fun verificarSesion() = Unit
    override fun invalidarSesion() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccesoViewModelTest {

    private val auth = AuthSimulado()

    // viewModelScope usa Dispatchers.Main, que no existe en la PC: se reemplaza por uno de prueba
    @Before
    fun prepararMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurarMain() = Dispatchers.resetMain()

    @Test
    fun formulario_invalido_no_llama_al_servicio_y_muestra_errores() = runTest {
        val vm = AccesoViewModel(auth)
        vm.onEmailChange("no-es-un-correo")
        vm.onEnviar()
        assertEquals(0, auth.llamadasInicio)
        assertTrue(vm.uiState.value.mostrarErrores)
        assertFalse(vm.uiState.value.esValido)
    }

    @Test
    fun error_del_servicio_se_muestra_sin_detalles_del_proveedor() = runTest {
        auth.respuesta = ResultadoAuth.Error(ErrorAuth.CREDENCIALES_INVALIDAS)
        val vm = AccesoViewModel(auth)
        vm.onEmailChange("ana@ejemplo.com")
        vm.onContrasenaChange("incorrecta")
        vm.onEnviar()
        assertEquals(1, auth.llamadasInicio)
        assertEquals(EstadoAcceso.Error(ErrorAuth.CREDENCIALES_INVALIDAS), vm.uiState.value.estado)
        // Al volver a escribir, el error desaparece
        vm.onContrasenaChange("otra")
        assertEquals(EstadoAcceso.Editando, vm.uiState.value.estado)
    }

    @Test
    fun mientras_se_autentica_el_estado_es_en_curso_y_no_se_puede_reenviar() = runTest {
        val respuesta = CompletableDeferred<ResultadoAuth>()
        auth.pendiente = respuesta
        val vm = AccesoViewModel(auth)
        vm.onEmailChange("ana@ejemplo.com")
        vm.onContrasenaChange("secreta123")
        vm.onEnviar()
        assertEquals(EstadoAcceso.EnCurso, vm.uiState.value.estado)
        assertFalse(vm.uiState.value.puedeEnviar)
        vm.onEnviar()                       // segundo toque mientras espera
        assertEquals(1, auth.llamadasInicio)
        respuesta.complete(ResultadoAuth.Exito)
    }

    @Test
    fun exito_deja_el_formulario_vacio_sin_la_contrasena_en_memoria() = runTest {
        val vm = AccesoViewModel(auth)
        vm.onCambiarModo()
        vm.onEmailChange("ana@ejemplo.com")
        vm.onContrasenaChange("secreta123")
        vm.onConfirmacionChange("secreta123")
        vm.onEnviar()
        assertEquals(1, auth.llamadasRegistro)
        val estado = vm.uiState.value
        assertEquals("", estado.email)
        assertEquals("", estado.contrasena)
        assertEquals(ModoAcceso.INICIAR_SESION, estado.modo)
    }

    @Test
    fun registro_exige_contrasena_de_seis_caracteres_y_confirmacion_igual() = runTest {
        val vm = AccesoViewModel(auth)
        vm.onCambiarModo()
        vm.onEmailChange("ana@ejemplo.com")
        vm.onContrasenaChange("123")
        vm.onConfirmacionChange("124")
        vm.onEnviar()
        assertEquals(0, auth.llamadasRegistro)
        assertTrue(vm.uiState.value.errorContrasena != null)
        assertTrue(vm.uiState.value.errorConfirmacion != null)
    }
}
