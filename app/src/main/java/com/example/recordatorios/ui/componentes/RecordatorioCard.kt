package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.ui.EjemplosPreview
import com.example.recordatorios.ui.coloresPorCategoria
import com.example.recordatorios.ui.formatearFecha
import com.example.recordatorios.ui.theme.RecordatoriosTheme

// Tarjeta reutilizable: recibe los datos y avisa el clic; no sabe a qué pantalla se navega
@Composable
fun RecordatorioCard(
    recordatorio: Recordatorio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                recordatorio.titulo,
                style = MaterialTheme.typography.titleMedium,
                // Los completados se tachan (además del texto "Completado el ...")
                textDecoration = if (recordatorio.completado) TextDecoration.LineThrough else null
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Etiqueta(recordatorio.categoria.nombre, colorCategoria(recordatorio))
                Etiqueta(
                    stringResource(R.string.etiqueta_prioridad, recordatorio.prioridad.etiqueta),
                    colorPrioridad(recordatorio.prioridad)
                )
            }
            Text(textoEstado(recordatorio.estado), style = MaterialTheme.typography.labelLarge)
            // Estado de sincronización: con símbolo y palabra, no solo con color
            Etiqueta(textoSync(recordatorio.estadoSync), colorSync(recordatorio.estadoSync))
        }
    }
}

@Composable
fun Etiqueta(texto: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        texto,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun colorCategoria(recordatorio: Recordatorio): Color =
    coloresPorCategoria[recordatorio.categoria] ?: MaterialTheme.colorScheme.primary

@Composable
fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> MaterialTheme.colorScheme.error
    Prioridad.Media -> MaterialTheme.colorScheme.tertiary
    Prioridad.Baja -> MaterialTheme.colorScheme.secondary
}

@Composable
fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> stringResource(R.string.estado_pendiente)
    is EstadoRecordatorio.Completado -> stringResource(R.string.estado_completado, formatearFecha(estado.fechaCompletado))
}

@Composable
fun textoSync(estado: EstadoSync): String = when (estado) {
    EstadoSync.SINCRONIZADO -> stringResource(R.string.sync_sincronizado)
    EstadoSync.PENDIENTE -> stringResource(R.string.sync_pendiente)
    EstadoSync.PENDIENTE_BORRAR -> stringResource(R.string.sync_pendiente_borrar)
    EstadoSync.ERROR -> stringResource(R.string.sync_error)
}

@Composable
fun colorSync(estado: EstadoSync): Color = when (estado) {
    EstadoSync.SINCRONIZADO -> Color(0xFF2E7D32)
    EstadoSync.PENDIENTE, EstadoSync.PENDIENTE_BORRAR -> Color(0xFFEF6C00)
    EstadoSync.ERROR -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true)
@Composable
private fun RecordatorioCardPreview() {
    RecordatoriosTheme {
        RecordatorioCard(EjemplosPreview.recordatorio, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
