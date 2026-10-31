@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui.crear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.R
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.prioridadesDisponibles
import com.example.recordatorios.ui.componentes.CampoTexto
import com.example.recordatorios.ui.componentes.SelectorOpciones
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import com.example.recordatorios.validation.MAX_CARACTERES_DESCRIPCION
import com.example.recordatorios.validation.validarFormulario

@Composable
fun CrearScreen(
    viewModel: CrearViewModel,
    onGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // El éxito se procesa como efecto (una vez por cambio de estado), no durante la composición
    LaunchedEffect(uiState.envio) {
        if (uiState.envio == EstadoEnvio.Exito) onGuardado()
    }

    CrearContenido(
        uiState = uiState,
        onTituloChange = viewModel::onTituloChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onPrioridadChange = viewModel::onPrioridadChange,
        onGuardar = viewModel::onGuardar,
        onCancelar = onCancelar
    )
}

@Composable
fun CrearContenido(
    uiState: CrearUiState,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (Categoria) -> Unit,
    onPrioridadChange: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errores = uiState.erroresVisibles
    val editable = !uiState.enviando

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_crear)) },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.accion_cancelar))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampoTexto(
                valor = uiState.titulo,
                onValorChange = onTituloChange,
                etiqueta = stringResource(R.string.campo_titulo),
                error = errores.titulo,
                habilitado = editable
            )
            CampoTexto(
                valor = uiState.descripcion,
                onValorChange = onDescripcionChange,
                etiqueta = stringResource(R.string.campo_descripcion),
                error = errores.descripcion,
                habilitado = editable,
                unaLinea = false,
                contador = stringResource(R.string.contador_caracteres, uiState.descripcion.length, MAX_CARACTERES_DESCRIPCION)
            )
            SelectorOpciones(
                titulo = stringResource(R.string.campo_categoria),
                opciones = uiState.categorias,
                seleccionada = uiState.categoria,
                textoOpcion = { it.nombre },
                onSeleccionar = onCategoriaChange,
                error = errores.categoria,
                habilitado = editable
            )
            SelectorOpciones(
                titulo = stringResource(R.string.campo_prioridad),
                opciones = prioridadesDisponibles,
                seleccionada = uiState.prioridad,
                textoOpcion = { it.etiqueta },
                onSeleccionar = onPrioridadChange,
                error = errores.prioridad,
                habilitado = editable
            )

            // Error general de guardado: ícono + texto, y se anuncia a los lectores de pantalla
            val envio = uiState.envio
            if (envio is EstadoEnvio.Error) {
                Row(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.descripcion_error),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(envio.mensaje, color = MaterialTheme.colorScheme.error)
                }
            }

            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onCancelar, enabled = editable) {
                    Text(stringResource(R.string.accion_cancelar))
                }
                // Deshabilitado si el formulario es inválido o si hay un envío en curso
                Button(onClick = onGuardar, enabled = uiState.puedeGuardar) {
                    if (uiState.enviando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.accion_guardando), Modifier.padding(start = 8.dp))
                    } else {
                        Text(stringResource(R.string.accion_guardar))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CrearContenidoPreview() {
    RecordatoriosTheme {
        CrearContenido(
            uiState = CrearUiState(
                titulo = "Pag",
                categorias = DatosSimulados.categorias,
                errores = validarFormulario("Pag", "", null, null, DatosSimulados.categorias),
                mostrarErrores = true
            ),
            onTituloChange = {}, onDescripcionChange = {}, onCategoriaChange = {}, onPrioridadChange = {},
            onGuardar = {}, onCancelar = {}
        )
    }
}
