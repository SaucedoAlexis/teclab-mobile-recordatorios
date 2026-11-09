package com.example.recordatorios.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.remoto.PruebaAccesoCruzado
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.ui.ajustes.AjustesScreen
import com.example.recordatorios.ui.ajustes.AjustesViewModel
import com.example.recordatorios.ui.crear.CrearScreen
import com.example.recordatorios.ui.crear.CrearViewModel
import com.example.recordatorios.ui.detalle.DetalleScreen
import com.example.recordatorios.ui.detalle.DetalleViewModel
import com.example.recordatorios.ui.lista.ListaScreen
import com.example.recordatorios.ui.lista.ListaViewModel

// Único lugar que conoce el NavController: las pantallas solo reciben lambdas
@Composable
fun NavegacionRecordatorios(
    repositorio: RecordatorioRepository,
    preferencias: PreferenciasRepository,
    auth: AuthRepository,
    pruebaAcceso: PruebaAccesoCruzado
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RutaLista) {
        composable<RutaLista> {
            // viewModel { } crea el ViewModel una vez por destino y le pasa la interfaz del repositorio
            val viewModel = viewModel { ListaViewModel(repositorio) }
            ListaScreen(
                viewModel = viewModel,
                onCrearRecordatorio = { navController.navigate(RutaFormulario()) { launchSingleTop = true } },
                onRecordatorioClick = { id -> navController.navigate(RutaDetalle(id)) { launchSingleTop = true } },
                onAbrirAjustes = { navController.navigate(RutaAjustes) { launchSingleTop = true } }
            )
        }
        composable<RutaFormulario> { entrada ->
            val ruta = entrada.toRoute<RutaFormulario>()
            val viewModel = viewModel { CrearViewModel(repositorio, ruta.id) }
            CrearScreen(
                viewModel = viewModel,
                // popBackStack() vuelve a la pantalla anterior; no agrega otra copia
                onGuardado = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }
        composable<RutaDetalle> { entrada ->
            val ruta = entrada.toRoute<RutaDetalle>()
            val viewModel = viewModel { DetalleViewModel(ruta.id, repositorio) }
            DetalleScreen(
                viewModel = viewModel,
                onEditar = { navController.navigate(RutaFormulario(ruta.id)) { launchSingleTop = true } },
                onVolver = { navController.popBackStack() }
            )
        }
        composable<RutaAjustes> {
            val viewModel = viewModel { AjustesViewModel(preferencias, auth, repositorio, pruebaAcceso) }
            AjustesScreen(viewModel = viewModel, onVolver = { navController.popBackStack() })
        }
    }
}
