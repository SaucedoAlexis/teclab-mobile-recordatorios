package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria

// Categorías definidas: el formulario solo acepta estas opciones.
// (Antes estaban en DatosSimulados; con Room los recordatorios de ejemplo ya no hacen falta.)
object Categorias {
    val trabajo = Categoria("Trabajo")
    val salud = Categoria("Salud")
    val hogar = Categoria("Hogar")
    val estudio = Categoria("Estudio")

    val todas: List<Categoria> = listOf(trabajo, salud, hogar, estudio)
}
