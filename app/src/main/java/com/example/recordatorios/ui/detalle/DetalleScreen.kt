@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.R
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.ui.componentes.Etiqueta
import com.example.recordatorios.ui.componentes.MensajeEstado
import com.example.recordatorios.ui.componentes.TipoMensaje
import com.example.recordatorios.ui.componentes.colorCategoria
import com.example.recordatorios.ui.componentes.colorPrioridad
import com.example.recordatorios.ui.componentes.textoEstado
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun DetalleScreen(viewModel: DetalleViewModel, onVolver: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetalleContenido(uiState = uiState, onVolver = onVolver)
}

@Composable
fun DetalleContenido(uiState: DetalleUiState, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_detalle)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.accion_volver))
                    }
                }
            )
        }
    ) { innerPadding ->
        val contenidoModifier = Modifier.padding(innerPadding)
        when (uiState) {
            DetalleUiState.Cargando -> MensajeEstado(TipoMensaje.Carga, stringResource(R.string.cargando), contenidoModifier)
            is DetalleUiState.Error -> MensajeEstado(
                TipoMensaje.Error,
                uiState.mensaje,
                contenidoModifier,
                textoAccion = stringResource(R.string.accion_volver),
                onAccion = onVolver
            )
            is DetalleUiState.ConDatos -> FichaRecordatorio(uiState.recordatorio, onVolver, contenidoModifier)
        }
    }
}

// Información completa del registro, incluidos los datos opcionales cuando existen
@Composable
private fun FichaRecordatorio(recordatorio: Recordatorio, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(recordatorio.titulo, style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta(recordatorio.categoria.nombre, colorCategoria(recordatorio))
                    Etiqueta(
                        stringResource(R.string.etiqueta_prioridad, recordatorio.prioridad.etiqueta),
                        colorPrioridad(recordatorio.prioridad)
                    )
                }
                HorizontalDivider()
                Dato(
                    stringResource(R.string.detalle_descripcion),
                    recordatorio.descripcion ?: stringResource(R.string.detalle_sin_descripcion)
                )
                Dato(stringResource(R.string.detalle_estado), textoEstado(recordatorio.estado))
                recordatorio.ubicacion?.let { Dato(stringResource(R.string.detalle_ubicacion), it) }
                recordatorio.fechaAviso?.let { Dato(stringResource(R.string.detalle_aviso), it) }
                Dato(stringResource(R.string.detalle_identificador), recordatorio.id.toString())
            }
        }
        Button(onClick = onVolver, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.accion_volver))
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun DetallePreview() {
    RecordatoriosTheme {
        DetalleContenido(DetalleUiState.ConDatos(DatosSimulados.recordatorios.first()), onVolver = {})
    }
}
