@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.recordatorios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.EstadoPantalla
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.FiltroEstado
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.categoriasUnicas
import com.example.recordatorios.domain.contarPorPrioridad
import com.example.recordatorios.domain.filtrarPorEstado
import com.example.recordatorios.domain.ordenarPorPrioridad
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import com.example.recordatorios.validation.validos

@Composable
fun PantallaRecordatorios(recordatorios: List<Recordatorio>, modifier: Modifier = Modifier) {
    var filtro by remember { mutableStateOf<FiltroEstado>(FiltroEstado.Todos) }

    val validos = recordatorios.validos()
    val descartados = recordatorios.size - validos.size
    val categorias = validos.categoriasUnicas()
    val conteo = validos.contarPorPrioridad()

    val visibles = validos.filtrarPorEstado(filtro).ordenarPorPrioridad()
    val estado: EstadoPantalla =
        if (visibles.isEmpty()) EstadoPantalla.SinResultados else EstadoPantalla.ConDatos(visibles)

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.titulo_pantalla)) }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                stringResource(R.string.resumen, validos.size, descartados, categorias.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                conteo.entries.joinToString(" · ") { "${it.key.etiqueta}: ${it.value}" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            FiltrosEstado(seleccionado = filtro, onSeleccionar = { filtro = it })

            when (estado) {
                EstadoPantalla.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is EstadoPantalla.Error -> Text(
                    estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                EstadoPantalla.SinResultados -> Text(
                    stringResource(R.string.sin_resultados),
                    modifier = Modifier.padding(16.dp)
                )
                is EstadoPantalla.ConDatos -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.recordatorios, key = { it.id }) { TarjetaRecordatorio(it) }
                }
            }
        }
    }
}

@Composable
private fun FiltrosEstado(seleccionado: FiltroEstado, onSeleccionar: (FiltroEstado) -> Unit) {
    val opciones = listOf(FiltroEstado.Todos, FiltroEstado.Pendientes, FiltroEstado.Completados, FiltroEstado.Vencidos)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionado,
                onClick = { onSeleccionar(opcion) },
                label = { Text(opcion.etiqueta) }
            )
        }
    }
}

@Composable
fun TarjetaRecordatorio(recordatorio: Recordatorio, modifier: Modifier = Modifier) {
    val colorCategoria = coloresPorCategoria[recordatorio.categoria] ?: MaterialTheme.colorScheme.primary
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(recordatorio.titulo, style = MaterialTheme.typography.titleMedium)
            recordatorio.descripcion?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta(recordatorio.categoria.nombre, colorCategoria)
                Etiqueta("Prioridad ${recordatorio.prioridad.etiqueta}", colorPrioridad(recordatorio.prioridad))
            }
            Text(textoEstado(recordatorio.estado), style = MaterialTheme.typography.labelLarge)
            recordatorio.ubicacion?.let { Text("📍 $it", style = MaterialTheme.typography.bodySmall) }
            recordatorio.fechaAviso?.let { Text("⏰ $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Text(
        texto,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> MaterialTheme.colorScheme.error
    Prioridad.Media -> MaterialTheme.colorScheme.tertiary
    Prioridad.Baja -> MaterialTheme.colorScheme.secondary
}

private fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    is EstadoRecordatorio.Completado -> "Completado el ${estado.fechaCompletado}"
    is EstadoRecordatorio.Vencido -> "Vencido hace ${estado.diasDeRetraso} día(s)"
}

@Preview(showBackground = true)
@Composable
fun PantallaRecordatoriosPreview() {
    RecordatoriosTheme {
        PantallaRecordatorios(DatosSimulados.recordatorios)
    }
}