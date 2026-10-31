package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.ui.theme.RecordatoriosTheme

enum class TipoMensaje { Carga, Vacio, Error }

// Componente único para comunicar carga, vacío o error, con una acción opcional (crear o reintentar)
@Composable
fun MensajeEstado(
    tipo: TipoMensaje,
    mensaje: String,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    onAccion: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (tipo) {
            TipoMensaje.Carga -> CircularProgressIndicator()
            TipoMensaje.Error -> Icon(
                Icons.Filled.Warning,
                contentDescription = stringResource(R.string.descripcion_error),
                tint = MaterialTheme.colorScheme.error
            )
            TipoMensaje.Vacio -> Unit
        }
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (tipo == TipoMensaje.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (textoAccion != null) {
            Button(onClick = onAccion) { Text(textoAccion) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MensajeEstadoErrorPreview() {
    RecordatoriosTheme {
        MensajeEstado(TipoMensaje.Error, "No se pudieron cargar los recordatorios.", textoAccion = "Reintentar")
    }
}
