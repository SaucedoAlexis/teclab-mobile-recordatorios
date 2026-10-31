@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.R
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.ModoSimulacion
import com.example.recordatorios.ui.componentes.MensajeEstado
import com.example.recordatorios.ui.componentes.RecordatorioCard
import com.example.recordatorios.ui.componentes.TipoMensaje
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import com.example.recordatorios.validation.validos

// Conecta el ViewModel con la pantalla. collectAsStateWithLifecycle deja de recolectar
// cuando la pantalla no está visible y vuelve a hacerlo al regresar.
@Composable
fun ListaScreen(
    viewModel: ListaViewModel,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modoSimulacion by viewModel.modoSimulacion.collectAsStateWithLifecycle()
    ListaContenido(
        uiState = uiState,
        modoSimulacion = modoSimulacion,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::onReintentar,
        onCambiarSimulacion = viewModel::onCambiarSimulacion
    )
}

// Pantalla sin estado propio: dibuja el UiState recibido y avisa los eventos con lambdas
@Composable
fun ListaContenido(
    uiState: ListaUiState,
    modoSimulacion: ModoSimulacion,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onReintentar: () -> Unit,
    onCambiarSimulacion: (ModoSimulacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_listado)) },
                actions = { MenuSimulacion(modoSimulacion, onCambiarSimulacion) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accion_crear))
            }
        }
    ) { innerPadding ->
        val contenidoModifier = Modifier.padding(innerPadding)
        // when exhaustivo: el compilador obliga a contemplar los cuatro estados
        when (uiState) {
            ListaUiState.Cargando -> MensajeEstado(
                TipoMensaje.Carga,
                stringResource(R.string.cargando),
                contenidoModifier
            )
            ListaUiState.Vacio -> MensajeEstado(
                TipoMensaje.Vacio,
                stringResource(R.string.listado_vacio),
                contenidoModifier,
                textoAccion = stringResource(R.string.accion_crear),
                onAccion = onCrearRecordatorio
            )
            is ListaUiState.Error -> MensajeEstado(
                TipoMensaje.Error,
                uiState.mensaje,
                contenidoModifier,
                textoAccion = stringResource(R.string.accion_reintentar),
                onAccion = onReintentar
            )
            is ListaUiState.ConDatos -> LazyColumn(
                modifier = contenidoModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // key estable: el id del recordatorio, no la posición
                items(uiState.recordatorios, key = { it.id }) { recordatorio ->
                    RecordatorioCard(recordatorio, onClick = { onRecordatorioClick(recordatorio.id) })
                }
            }
        }
    }
}

// Menú para forzar cada estado sin base de datos ni servicio remoto
@Composable
private fun MenuSimulacion(modoActual: ModoSimulacion, onCambiar: (ModoSimulacion) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { abierto = true }) { Text(stringResource(R.string.accion_simular)) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ModoSimulacion.entries.forEach { modo ->
                DropdownMenuItem(
                    text = { Text(modo.etiqueta) },
                    leadingIcon = { RadioButton(selected = modo == modoActual, onClick = null) },
                    onClick = {
                        abierto = false
                        onCambiar(modo)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListaConDatosPreview() {
    RecordatoriosTheme {
        ListaContenido(
            uiState = ListaUiState.ConDatos(DatosSimulados.recordatorios.validos()),
            modoSimulacion = ModoSimulacion.Normal,
            onCrearRecordatorio = {}, onRecordatorioClick = {}, onReintentar = {}, onCambiarSimulacion = {}
        )
    }
}
