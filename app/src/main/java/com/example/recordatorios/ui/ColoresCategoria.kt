package com.example.recordatorios.ui

import androidx.compose.ui.graphics.Color
import com.example.recordatorios.data.Categorias
import com.example.recordatorios.domain.Categoria

// Map: asocia cada categoría con un color
val coloresPorCategoria: Map<Categoria, Color> = mapOf(
    Categorias.trabajo to Color(0xFF1E88E5),
    Categorias.salud to Color(0xFF43A047),
    Categorias.hogar to Color(0xFFFB8C00),
    Categorias.estudio to Color(0xFF8E24AA)
)
