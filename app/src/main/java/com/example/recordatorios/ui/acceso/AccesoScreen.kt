package com.example.recordatorios.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.R
import com.example.recordatorios.data.auth.ErrorAuth
import com.example.recordatorios.data.auth.MotivoSinSesion
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun AccesoScreen(viewModel: AccesoViewModel, motivo: MotivoSinSesion?) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccesoContenido(
        uiState = uiState,
        motivo = motivo,
        onEmailChange = viewModel::onEmailChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onConfirmacionChange = viewModel::onConfirmacionChange,
        onEnviar = viewModel::onEnviar,
        onCambiarModo = viewModel::onCambiarModo
    )
}

@Composable
fun AccesoContenido(
    uiState: AccesoUiState,
    motivo: MotivoSinSesion?,
    onEmailChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onConfirmacionChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onCambiarModo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val registro = uiState.modo == ModoAcceso.REGISTRARSE
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(if (registro) R.string.acceso_titulo_registro else R.string.acceso_titulo_inicio),
                style = MaterialTheme.typography.titleMedium
            )

            // Aviso cuando la sesión anterior se perdió (cuenta deshabilitada o eliminada)
            if (motivo == MotivoSinSesion.SESION_INVALIDA) {
                Aviso(stringResource(R.string.acceso_sesion_invalida))
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.campo_email)) },
                singleLine = true,
                enabled = !uiState.enCurso,
                isError = uiState.mostrarErrores && uiState.errorEmail != null,
                supportingText = errorTexto(uiState.mostrarErrores, uiState.errorEmail),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("campo_email")
            )
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                label = { Text(stringResource(R.string.campo_contrasena)) },
                singleLine = true,
                enabled = !uiState.enCurso,
                // La contraseña se muestra con puntos y el teclado no la guarda ni la sugiere
                visualTransformation = PasswordVisualTransformation(),
                isError = uiState.mostrarErrores && uiState.errorContrasena != null,
                supportingText = errorTexto(uiState.mostrarErrores, uiState.errorContrasena),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (registro) ImeAction.Next else ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth().testTag("campo_contrasena")
            )
            if (registro) {
                OutlinedTextField(
                    value = uiState.confirmacion,
                    onValueChange = onConfirmacionChange,
                    label = { Text(stringResource(R.string.campo_confirmar_contrasena)) },
                    singleLine = true,
                    enabled = !uiState.enCurso,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = uiState.mostrarErrores && uiState.errorConfirmacion != null,
                    supportingText = errorTexto(uiState.mostrarErrores, uiState.errorConfirmacion),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("campo_confirmacion")
                )
            }

            val estado = uiState.estado
            if (estado is EstadoAcceso.Error) Aviso(stringResource(estado.motivo.mensaje()))

            Button(
                onClick = onEnviar,
                enabled = !uiState.enCurso,
                modifier = Modifier.fillMaxWidth().testTag("boton_acceso")
            ) {
                if (uiState.enCurso) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.acceso_en_curso), Modifier.padding(start = 8.dp))
                } else {
                    Text(stringResource(if (registro) R.string.accion_registrarse else R.string.accion_iniciar_sesion))
                }
            }
            TextButton(onClick = onCambiarModo, enabled = !uiState.enCurso) {
                Text(stringResource(if (registro) R.string.acceso_ir_inicio else R.string.acceso_ir_registro))
            }
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(texto, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun errorTexto(mostrar: Boolean, error: ErrorCampo?): (@Composable () -> Unit)? {
    if (!mostrar || error == null) return null
    return { Text(stringResource(error.mensaje())) }
}

private fun ErrorCampo.mensaje(): Int = when (this) {
    ErrorCampo.VACIO -> R.string.error_campo_vacio
    ErrorCampo.EMAIL_FORMATO -> R.string.error_email_formato
    ErrorCampo.CONTRASENA_CORTA -> R.string.error_contrasena_corta
    ErrorCampo.NO_COINCIDEN -> R.string.error_contrasenas_distintas
}

// Cada error del servicio tiene un mensaje propio; la persona nunca ve el texto técnico de Firebase
private fun ErrorAuth.mensaje(): Int = when (this) {
    ErrorAuth.CREDENCIALES_INVALIDAS -> R.string.error_auth_credenciales
    ErrorAuth.EMAIL_INVALIDO -> R.string.error_email_formato
    ErrorAuth.EMAIL_EN_USO -> R.string.error_auth_email_en_uso
    ErrorAuth.CONTRASENA_DEBIL -> R.string.error_contrasena_corta
    ErrorAuth.CUENTA_DESHABILITADA -> R.string.error_auth_deshabilitada
    ErrorAuth.DEMASIADOS_INTENTOS -> R.string.error_auth_demasiados_intentos
    ErrorAuth.SIN_CONEXION -> R.string.error_auth_sin_conexion
    ErrorAuth.DESCONOCIDO -> R.string.error_auth_desconocido
}

@Preview(showBackground = true)
@Composable
private fun AccesoPreview() {
    RecordatoriosTheme {
        AccesoContenido(
            uiState = AccesoUiState(email = "ana@ejemplo.com", estado = EstadoAcceso.Error(ErrorAuth.CREDENCIALES_INVALIDAS)),
            motivo = null,
            onEmailChange = {}, onContrasenaChange = {}, onConfirmacionChange = {}, onEnviar = {}, onCambiarModo = {}
        )
    }
}
