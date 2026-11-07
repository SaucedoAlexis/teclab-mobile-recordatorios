package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.ui.theme.RecordatoriosTheme

private enum class TipoAviso { Advertencia, Progreso, Info }

// Avisos de conexión y sincronización arriba del listado.
// Los datos de Room se siguen mostrando abajo: la falta de red no reemplaza la pantalla por un error.
@Composable
fun BarraSincronizacion(
    hayConexion: Boolean,
    sinSincronizar: Int,
    sincronizando: Boolean,
    mensaje: String?,
    mensajeEsError: Boolean,
    modifier: Modifier = Modifier
) {
    val avisos = buildList {
        if (!hayConexion) add(stringResource(R.string.sin_conexion) to TipoAviso.Advertencia)
        if (sincronizando) {
            add(stringResource(R.string.sincronizando) to TipoAviso.Progreso)
        } else if (mensaje != null) {
            add(mensaje to if (mensajeEsError) TipoAviso.Advertencia else TipoAviso.Info)
        }
        if (sinSincronizar > 0) add(stringResource(R.string.cambios_sin_sincronizar, sinSincronizar) to TipoAviso.Info)
    }
    if (avisos.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        avisos.forEach { (texto, tipo) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when (tipo) {
                    TipoAviso.Progreso -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    TipoAviso.Advertencia -> Icon(
                        Icons.Filled.Warning, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)
                    )
                    TipoAviso.Info -> Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    texto,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tipo == TipoAviso.Advertencia) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BarraSincronizacionPreview() {
    RecordatoriosTheme {
        BarraSincronizacion(hayConexion = false, sinSincronizar = 2, sincronizando = false, mensaje = null, mensajeEsError = false)
    }
}
