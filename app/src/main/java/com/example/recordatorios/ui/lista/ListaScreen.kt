@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.R
import com.example.recordatorios.ui.EjemplosPreview
import com.example.recordatorios.ui.componentes.BarraSincronizacion
import com.example.recordatorios.ui.componentes.MensajeEstado
import com.example.recordatorios.ui.componentes.RecordatorioCard
import com.example.recordatorios.ui.componentes.TipoMensaje
import com.example.recordatorios.ui.theme.RecordatoriosTheme

// Conecta el ViewModel con la pantalla. collectAsStateWithLifecycle deja de recolectar
// cuando la pantalla no está visible y vuelve a hacerlo al regresar.
@Composable
fun ListaScreen(
    viewModel: ListaViewModel,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    onAbrirAjustes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ListaContenido(
        uiState = uiState,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::onReintentar,
        onSincronizar = viewModel::onSincronizar,
        onAbrirAjustes = onAbrirAjustes
    )
}

// Pantalla sin estado propio: dibuja el UiState recibido y avisa los eventos con lambdas
@Composable
fun ListaContenido(
    uiState: ListaUiState,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    onReintentar: () -> Unit,
    onSincronizar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_listado)) },
                actions = {
                    // Sin conexión o con una sincronización en curso, el botón queda deshabilitado
                    IconButton(onClick = onSincronizar, enabled = uiState.hayConexion && !uiState.sincronizando) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.accion_sincronizar))
                    }
                    IconButton(onClick = onAbrirAjustes) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.accion_ajustes))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accion_crear))
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            BarraSincronizacion(
                hayConexion = uiState.hayConexion,
                sinSincronizar = uiState.sinSincronizar,
                sincronizando = uiState.sincronizando,
                mensaje = uiState.mensajeSincronizacion,
                mensajeEsError = uiState.mensajeEsError
            )
            val contenidoModifier = Modifier.weight(1f)
            // when exhaustivo: el compilador obliga a contemplar los cuatro casos
            when (val contenido = uiState.contenido) {
                ContenidoLista.Cargando -> MensajeEstado(
                    TipoMensaje.Carga,
                    stringResource(R.string.cargando),
                    contenidoModifier
                )
                ContenidoLista.Vacio -> MensajeEstado(
                    TipoMensaje.Vacio,
                    stringResource(R.string.listado_vacio),
                    contenidoModifier,
                    textoAccion = stringResource(R.string.accion_crear),
                    onAccion = onCrearRecordatorio
                )
                is ContenidoLista.Error -> MensajeEstado(
                    TipoMensaje.Error,
                    contenido.mensaje,
                    contenidoModifier,
                    textoAccion = stringResource(R.string.accion_reintentar),
                    onAccion = onReintentar
                )
                is ContenidoLista.ConDatos -> LazyColumn(
                    modifier = contenidoModifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // key estable: el id del recordatorio, no la posición
                    items(contenido.recordatorios, key = { it.id }) { recordatorio ->
                        RecordatorioCard(recordatorio, onClick = { onRecordatorioClick(recordatorio.id) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListaConDatosPreview() {
    RecordatoriosTheme {
        ListaContenido(
            uiState = ListaUiState(
                contenido = ContenidoLista.ConDatos(EjemplosPreview.lista),
                hayConexion = false,
                sinSincronizar = 1
            ),
            onCrearRecordatorio = {}, onRecordatorioClick = {}, onReintentar = {}, onSincronizar = {}, onAbrirAjustes = {}
        )
    }
}
