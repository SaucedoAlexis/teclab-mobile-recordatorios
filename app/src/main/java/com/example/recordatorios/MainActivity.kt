package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recordatorios.ui.navegacion.NavegacionRecordatorios
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RecordatoriosApplication
        setContent {
            RecordatoriosTheme {
                NavegacionRecordatorios(repositorio = app.repositorio, simulacion = app.simulacion)
            }
        }
    }
}
