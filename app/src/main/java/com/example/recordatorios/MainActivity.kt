package com.example.recordatorios

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.data.preferencias.Tema
import androidx.lifecycle.lifecycleScope
import com.example.recordatorios.ui.navegacion.RaizApp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RecordatoriosApplication
        setContent {
            // El tema elegido en Ajustes (guardado en DataStore) se aplica a toda la app
            val preferencias by app.preferencias.preferencias.collectAsStateWithLifecycle(initialValue = null)
            val oscuro = when (preferencias?.tema) {
                Tema.CLARO -> false
                Tema.OSCURO -> true
                Tema.SISTEMA, null -> isSystemInDarkTheme()
            }
            // Los íconos de la barra de estado (hora, batería) siguen el tema elegido, no el del sistema
            LaunchedEffect(oscuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                )
            }
            RecordatoriosTheme(darkTheme = oscuro) {
                RaizApp(
                    auth = app.auth,
                    repositorio = app.repositorio,
                    preferencias = app.preferencias,
                    pruebaAcceso = app.pruebaAccesoCruzado
                )
            }
        }
    }

    // Cada vez que la app vuelve a primer plano (incluida la reapertura) se consulta al servicio
    // si la sesión guardada sigue siendo válida. Sin conexión, la sesión se mantiene.
    override fun onStart() {
        super.onStart()
        val app = application as RecordatoriosApplication
        lifecycleScope.launch { app.auth.verificarSesion() }
    }
}
