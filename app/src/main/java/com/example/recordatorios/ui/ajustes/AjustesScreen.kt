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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
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
    val emailSesion by viewModel.emailSesion.collectAsStateWithLifecycle()
    val sinSincronizar by viewModel.cambiosSinSincronizar.collectAsStateWithLifecycle()
    val uidSesion by viewModel.uidSesion.collectAsStateWithLifecycle()
    val resultadoPrueba by viewModel.resultadoPrueba.collectAsStateWithLifecycle()
    AjustesContenido(
        preferencias = preferencias,
        emailSesion = emailSesion,
        cambiosSinSincronizar = sinSincronizar,
        onCerrarSesion = viewModel::onCerrarSesion,
        mostrarPruebas = BuildConfig.DEBUG,
        onTemaChange = viewModel::onTemaChange,
        onMostrarCompletadosChange = viewModel::onMostrarCompletadosChange,
        onOrdenChange = viewModel::onOrdenChange,
        onGuardarOrdenInvalido = viewModel::onGuardarOrdenInvalido,
        onVolver = onVolver,
        pruebaAcceso = {
            PruebaAccesoCruzado(uidSesion, resultadoPrueba, viewModel::onProbarAccesoCruzado)
        }
    )
}

@Composable
fun AjustesContenido(
    preferencias: PreferenciasUsuario?,
    emailSesion: String?,
    cambiosSinSincronizar: Int,
    onCerrarSesion: () -> Unit,
    mostrarPruebas: Boolean,
    onTemaChange: (Tema) -> Unit,
    onMostrarCompletadosChange: (Boolean) -> Unit,
    onOrdenChange: (OrdenListado) -> Unit,
    onGuardarOrdenInvalido: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    pruebaAcceso: @Composable () -> Unit = {}
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

            HorizontalDivider()
            SeccionCuenta(emailSesion, cambiosSinSincronizar, onCerrarSesion)

            if (mostrarPruebas) {
                HorizontalDivider()
                Seccion(stringResource(R.string.ajustes_pruebas))
                OutlinedButton(onClick = onGuardarOrdenInvalido) {
                    Text(stringResource(R.string.ajustes_guardar_invalido))
                }
                pruebaAcceso()
            }
        }
    }
}

@Composable
private fun SeccionCuenta(emailSesion: String?, cambiosSinSincronizar: Int, onCerrarSesion: () -> Unit) {
    // rememberSaveable: el diálogo sigue abierto si se rota la pantalla
    var confirmar by rememberSaveable { mutableStateOf(false) }
    Seccion(stringResource(R.string.ajustes_cuenta))
    if (emailSesion != null) {
        Text(stringResource(R.string.ajustes_sesion_de, emailSesion), style = MaterialTheme.typography.bodyMedium)
    }
    OutlinedButton(onClick = { confirmar = true }, modifier = Modifier.testTag("boton_cerrar_sesion")) {
        Text(stringResource(R.string.accion_cerrar_sesion))
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text(stringResource(R.string.confirmar_cerrar_sesion_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.confirmar_cerrar_sesion_texto))
                    // Lo que todavía no se subió a la nube se pierde: se avisa con la cantidad exacta
                    if (cambiosSinSincronizar > 0) {
                        Text(
                            stringResource(R.string.confirmar_cerrar_sesion_pendientes, cambiosSinSincronizar),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onCerrarSesion() }) {
                    Text(stringResource(R.string.accion_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.accion_cancelar)) }
            }
        )
    }
}

// Paso 06 (solo versión de desarrollo): intenta acceder a la colección de otra cuenta
@Composable
private fun PruebaAccesoCruzado(uidSesion: String?, resultado: List<String>?, onProbar: (String) -> Unit) {
    var uidAjeno by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.prueba_acceso_titulo), style = MaterialTheme.typography.titleSmall)
    if (uidSesion != null) {
        Text(stringResource(R.string.prueba_acceso_mi_uid, uidSesion), style = MaterialTheme.typography.bodySmall)
    }
    OutlinedTextField(
        value = uidAjeno,
        onValueChange = { uidAjeno = it },
        label = { Text(stringResource(R.string.prueba_acceso_uid_ajeno)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("campo_uid_ajeno")
    )
    OutlinedButton(onClick = { onProbar(uidAjeno) }, enabled = uidAjeno.isNotBlank()) {
        Text(stringResource(R.string.prueba_acceso_ejecutar))
    }
    resultado?.forEach { linea -> Text(linea, style = MaterialTheme.typography.bodySmall) }
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
            emailSesion = "ana@ejemplo.com",
            cambiosSinSincronizar = 0,
            onCerrarSesion = {},
            mostrarPruebas = true,
            onTemaChange = {}, onMostrarCompletadosChange = {}, onOrdenChange = {},
            onGuardarOrdenInvalido = {}, onVolver = {}
        )
    }
}
