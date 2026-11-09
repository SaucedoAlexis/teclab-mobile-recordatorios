package com.example.recordatorios.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.R
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.EstadoSesion
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.remoto.PruebaAccesoCruzado
import com.example.recordatorios.ui.acceso.AccesoScreen
import com.example.recordatorios.ui.acceso.AccesoViewModel
import com.example.recordatorios.ui.componentes.MensajeEstado
import com.example.recordatorios.ui.componentes.TipoMensaje

// Raíz de la interfaz: decide qué mostrar según el estado de la sesión.
// Las pantallas de recordatorios solo existen mientras hay una sesión activa.
@Composable
fun RaizApp(
    auth: AuthRepository,
    repositorio: RecordatorioRepository,
    preferencias: PreferenciasRepository,
    pruebaAcceso: PruebaAccesoCruzado
) {
    val sesion by auth.estado.collectAsStateWithLifecycle()

    when (val estado = sesion) {
        EstadoSesion.Comprobando ->
            MensajeEstado(TipoMensaje.Carga, stringResource(R.string.acceso_comprobando))

        is EstadoSesion.SinSesion -> {
            val viewModel = viewModel { AccesoViewModel(auth) }
            AccesoScreen(viewModel = viewModel, motivo = estado.motivo)
        }

        is EstadoSesion.Activa ->
            // key(uid): si cambia la cuenta, se descarta toda la navegación anterior junto con sus ViewModels
            // y sus estados. Así una cuenta nueva nunca ve, ni por un instante, pantallas de la sesión anterior.
            key(estado.usuario.uid) {
                NavegacionRecordatorios(
                    repositorio = repositorio,
                    preferencias = preferencias,
                    auth = auth,
                    pruebaAcceso = pruebaAcceso
                )
            }
    }
}
