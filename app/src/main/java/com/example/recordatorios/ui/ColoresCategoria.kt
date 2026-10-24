package com.example.recordatorios.ui

import androidx.compose.ui.graphics.Color
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.Categoria

// Map: asocia cada categoría con un color
val coloresPorCategoria: Map<Categoria, Color> = mapOf(
    DatosSimulados.trabajo to Color(0xFF1E88E5),
    DatosSimulados.salud to Color(0xFF43A047),
    DatosSimulados.hogar to Color(0xFFFB8C00),
    DatosSimulados.estudio to Color(0xFF8E24AA)
)