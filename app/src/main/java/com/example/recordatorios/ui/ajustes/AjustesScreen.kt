@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.BuildConfig
import com.example.recordatorios.R
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.Tema
import com.example.recordatorios.ui.componentes.MensajeEstado
import com.example.recordatorios.ui.componentes.TipoMensaje
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun AjustesScreen(viewModel: AjustesViewModel, onVolver: () -> Unit) {
    // Se observa con conciencia del ciclo de vida, igual que el resto de las pantallas
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    AjustesContenido(
        preferencias = preferencias,
        mostrarPruebas = BuildConfig.DEBUG,
        onTemaChange = viewModel::onTemaChange,
        onMostrarCompletadosChange = viewModel::onMostrarCompletadosChange,
        onOrdenChange = viewModel::onOrdenChange,
        onGuardarOrdenInvalido = viewModel::onGuardarOrdenInvalido,
        onVolver = onVolver
    )
}

@Composable
fun AjustesContenido(
    preferencias: PreferenciasUsuario?,
    mostrarPruebas: Boolean,
    onTemaChange: (Tema) -> Unit,
    onMostrarCompletadosChange: (Boolean) -> Unit,
    onOrdenChange: (OrdenListado) -> Unit,
    onGuardarOrdenInvalido: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_ajustes)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.accion_volver))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (preferencias == null) {
            MensajeEstado(TipoMensaje.Carga, stringResource(R.string.cargando), Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.ajustes_info), style = MaterialTheme.typography.bodyMedium)

            Seccion(stringResource(R.string.ajustes_tema))
            Tema.entries.forEach { tema ->
                OpcionRadio(tema.etiqueta, seleccionada = preferencias.tema == tema, onClick = { onTemaChange(tema) })
            }
            if (preferencias.temaGuardadoInvalido) Aviso(stringResource(R.string.ajustes_tema_invalido))

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferencias.mostrarCompletados,
                        role = Role.Switch,
                        onValueChange = onMostrarCompletadosChange
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ajustes_mostrar_completados), Modifier.weight(1f))
                Switch(checked = preferencias.mostrarCompletados, onCheckedChange = null)
            }

            HorizontalDivider()
            Seccion(stringResource(R.string.ajustes_orden))
            OrdenListado.entries.forEach { orden ->
                OpcionRadio(orden.etiqueta, seleccionada = preferencias.orden == orden, onClick = { onOrdenChange(orden) })
            }
            if (preferencias.ordenGuardadoInvalido) Aviso(stringResource(R.string.ajustes_orden_invalido))

            if (mostrarPruebas) {
                HorizontalDivider()
                Seccion(stringResource(R.string.ajustes_pruebas))
                OutlinedButton(onClick = onGuardarOrdenInvalido) {
                    Text(stringResource(R.string.ajustes_guardar_invalido))
                }
            }
        }
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Aviso(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

// Toda la fila es seleccionable (más fácil de tocar y la lee bien el lector de pantalla)
@Composable
private fun OpcionRadio(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = seleccionada, onClick = null)
        Text(texto, Modifier.padding(start = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun AjustesPreview() {
    RecordatoriosTheme {
        AjustesContenido(
            preferencias = PreferenciasUsuario(ordenGuardadoInvalido = true),
            mostrarPruebas = true,
            onTemaChange = {}, onMostrarCompletadosChange = {}, onOrdenChange = {},
            onGuardarOrdenInvalido = {}, onVolver = {}
        )
    }
}
