@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.recordatorios.ui.EjemplosPreview
import com.example.recordatorios.ui.componentes.Etiqueta
import com.example.recordatorios.ui.componentes.MensajeEstado
import com.example.recordatorios.ui.componentes.TipoMensaje
import com.example.recordatorios.ui.componentes.colorCategoria
import com.example.recordatorios.ui.componentes.colorPrioridad
import com.example.recordatorios.ui.componentes.colorSync
import com.example.recordatorios.ui.componentes.textoEstado
import com.example.recordatorios.ui.componentes.textoSync
import com.example.recordatorios.ui.formatearFecha
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun DetalleScreen(viewModel: DetalleViewModel, onEditar: () -> Unit, onVolver: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Después de eliminar se vuelve al listado (efecto, no durante la composición)
    LaunchedEffect(uiState) {
        if (uiState == DetalleUiState.Eliminado) onVolver()
    }

    DetalleContenido(
        uiState = uiState,
        onEditar = onEditar,
        onCambiarCompletado = viewModel::onCambiarCompletado,
        onEliminar = viewModel::onEliminar,
        onVolver = onVolver
    )
}

@Composable
fun DetalleContenido(
    uiState: DetalleUiState,
    onEditar: () -> Unit,
    onCambiarCompletado: () -> Unit,
    onEliminar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            DetalleUiState.Cargando, DetalleUiState.Eliminado ->
                MensajeEstado(TipoMensaje.Carga, stringResource(R.string.cargando), contenidoModifier)
            is DetalleUiState.Error -> MensajeEstado(
                TipoMensaje.Error,
                uiState.mensaje,
                contenidoModifier,
                textoAccion = stringResource(R.string.accion_volver),
                onAccion = onVolver
            )
            is DetalleUiState.ConDatos -> FichaRecordatorio(
                uiState = uiState,
                onEditar = onEditar,
                onCambiarCompletado = onCambiarCompletado,
                onEliminar = onEliminar,
                modifier = contenidoModifier
            )
        }
    }
}

// Información completa del registro y acciones de modificación
@Composable
private fun FichaRecordatorio(
    uiState: DetalleUiState.ConDatos,
    onEditar: () -> Unit,
    onCambiarCompletado: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recordatorio = uiState.recordatorio
    var confirmarEliminar by remember { mutableStateOf(false) }

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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Dato(stringResource(R.string.detalle_creado), formatearFecha(recordatorio.fechaCreacion))
                Dato(stringResource(R.string.detalle_modificado), formatearFecha(recordatorio.actualizadoEn))
                Column {
                    Text(
                        stringResource(R.string.detalle_sincronizacion),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Etiqueta(textoSync(recordatorio.estadoSync), colorSync(recordatorio.estadoSync))
                }
                Dato(stringResource(R.string.detalle_identificador), recordatorio.id)
            }
        }

        if (uiState.mensajeError != null) {
            Text(uiState.mensajeError, color = MaterialTheme.colorScheme.error)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onCambiarCompletado, enabled = !uiState.procesando) {
                Text(
                    stringResource(
                        if (recordatorio.completado) R.string.accion_marcar_pendiente else R.string.accion_marcar_completado
                    )
                )
            }
            OutlinedButton(onClick = onEditar, enabled = !uiState.procesando) {
                Text(stringResource(R.string.accion_editar))
            }
            OutlinedButton(
                onClick = { confirmarEliminar = true },
                enabled = !uiState.procesando,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.accion_eliminar))
            }
        }
    }

    // Se pide confirmación antes de eliminar
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(stringResource(R.string.confirmar_eliminar_titulo)) },
            text = { Text(stringResource(R.string.confirmar_eliminar_texto, recordatorio.titulo)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    onEliminar()
                }) { Text(stringResource(R.string.accion_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text(stringResource(R.string.accion_cancelar)) }
            }
        )
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
        DetalleContenido(
            DetalleUiState.ConDatos(EjemplosPreview.recordatorio),
            onEditar = {}, onCambiarCompletado = {}, onEliminar = {}, onVolver = {}
        )
    }
}
