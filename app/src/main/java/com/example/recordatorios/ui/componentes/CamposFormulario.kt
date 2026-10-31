package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Campo controlado: no guarda el texto; lo recibe en "valor" y avisa cada cambio con onValorChange.
// El error se comunica con isError (color), un texto visible y la propiedad semántica error (lectores de pantalla).
@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    unaLinea: Boolean = true,
    contador: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = error != null,
        enabled = habilitado,
        singleLine = unaLinea,
        minLines = if (unaLinea) 1 else 3,
        supportingText = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(error ?: "", modifier = Modifier.weight(1f))
                if (contador != null) Text(contador)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) }
    )
}

// Selector controlado de una opción entre varias (se usa para categoría y prioridad).
// La opción elegida se marca con un ícono de check, no solo con color.
@Composable
fun <T> SelectorOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T?,
    textoOpcion: (T) -> String,
    onSeleccionar: (T) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Column(
        modifier = modifier.semantics { if (error != null) error(error) },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                FilterChip(
                    selected = elegida,
                    onClick = { onSeleccionar(opcion) },
                    label = { Text(textoOpcion(opcion)) },
                    enabled = habilitado,
                    leadingIcon = if (elegida) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null
                )
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
