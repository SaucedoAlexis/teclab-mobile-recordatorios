package com.example.recordatorios.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.recordatorios.data.ControlSimulacion
import com.example.recordatorios.data.RecordatorioRepository
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
    simulacion: ControlSimulacion
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RutaLista) {
        composable<RutaLista> {
            // viewModel { } crea el ViewModel una vez por destino y le pasa la interfaz del repositorio
            val viewModel = viewModel { ListaViewModel(repositorio, simulacion) }
            ListaScreen(
                viewModel = viewModel,
                onCrearRecordatorio = { navController.navigate(RutaCrear) { launchSingleTop = true } },
                onRecordatorioClick = { id -> navController.navigate(RutaDetalle(id)) { launchSingleTop = true } }
            )
        }
        composable<RutaCrear> {
            val viewModel = viewModel { CrearViewModel(repositorio) }
            CrearScreen(
                viewModel = viewModel,
                // popBackStack() vuelve al listado que ya estaba en la pila; no agrega otra copia
                onGuardado = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }
        composable<RutaDetalle> { entrada ->
            val ruta = entrada.toRoute<RutaDetalle>()
            val viewModel = viewModel { DetalleViewModel(ruta.id, repositorio) }
            DetalleScreen(viewModel = viewModel, onVolver = { navController.popBackStack() })
        }
    }
}
