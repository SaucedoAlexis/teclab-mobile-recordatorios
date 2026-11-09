package com.example.recordatorios.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.CerrarSesion
import com.example.recordatorios.data.auth.EstadoSesion
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.remoto.PruebaAccesoCruzado
import com.example.recordatorios.data.remoto.ResultadoRemoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.Tema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AjustesViewModel(
    private val repositorio: PreferenciasRepository,
    private val auth: AuthRepository,
    recordatorios: RecordatorioRepository,
    private val pruebaAcceso: PruebaAccesoCruzado
) : ViewModel() {

    // uid de la cuenta activa: se muestra en la sección de pruebas para poder usarlo desde otra cuenta
    val uidSesion: StateFlow<String?> = auth.estado
        .map { (it as? EstadoSesion.Activa)?.usuario?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    // Resultado de la prueba de acceso cruzado (null = todavía no se ejecutó)
    private val _resultadoPrueba = MutableStateFlow<List<String>?>(null)
    val resultadoPrueba: StateFlow<List<String>?> = _resultadoPrueba.asStateFlow()

    private val cerrarSesion = CerrarSesion(auth, recordatorios)

    // Cambios que todavía no llegaron a la nube: se avisan antes de cerrar sesión porque se pierden
    val cambiosSinSincronizar: StateFlow<Int> = recordatorios.observarCantidadSinSincronizar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    // Correo de la cuenta activa, para mostrarlo en la sección "Cuenta"
    val emailSesion: StateFlow<String?> = auth.estado
        .map { (it as? EstadoSesion.Activa)?.usuario?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

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

    // Borra los datos locales de la cuenta y cierra la sesión; la raíz de la app vuelve sola al acceso
    fun onCerrarSesion() {
        viewModelScope.launch { cerrarSesion() }
    }

    // Paso 06: intenta leer, escribir y borrar en la colección de otra cuenta
    fun onProbarAccesoCruzado(uidAjeno: String) {
        val uid = uidAjeno.trim()
        if (uid.isEmpty()) return
        _resultadoPrueba.value = listOf("Probando…")
        viewModelScope.launch {
            _resultadoPrueba.value = pruebaAcceso.ejecutar(uid).map { intento ->
                val resultado = when (val r = intento.resultado) {
                    is ResultadoRemoto.Exito -> "PERMITIDO ⚠"
                    is ResultadoRemoto.Rechazado -> "RECHAZADO ✓ (${r.motivo})"
                    ResultadoRemoto.NoAutenticado -> "RECHAZADO ✓ (sin sesión)"
                    ResultadoRemoto.SinConexion -> "sin conexión"
                    ResultadoRemoto.Timeout -> "sin respuesta"
                    is ResultadoRemoto.ErrorTemporal -> r.motivo
                    is ResultadoRemoto.DatosInvalidos -> "error inesperado"
                }
                "${intento.descripcion}: $resultado"
            }
        }
    }

    // Solo para la prueba del dato inválido (el botón se ve únicamente en la versión debug)
    fun onGuardarOrdenInvalido() {
        viewModelScope.launch { repositorio.guardarOrdenInvalidoDePrueba() }
    }
}
