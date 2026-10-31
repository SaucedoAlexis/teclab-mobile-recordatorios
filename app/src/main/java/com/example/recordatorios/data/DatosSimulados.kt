package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio

object DatosSimulados {
    val trabajo = Categoria("Trabajo")
    val salud = Categoria("Salud")
    val hogar = Categoria("Hogar")
    val estudio = Categoria("Estudio")

    // Categorías definidas: el formulario solo acepta estas opciones
    val categorias: List<Categoria> = listOf(trabajo, salud, hogar, estudio)

    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(1, "Entregar informe mensual", trabajo, Prioridad.Alta, EstadoRecordatorio.Pendiente,
            descripcion = "Enviar al coordinador antes del viernes", fechaAviso = "28/10/2026 09:00"),
        Recordatorio(2, "Turno con el dentista", salud, Prioridad.Media, EstadoRecordatorio.Completado("20/10/2026"),
            ubicacion = "Clínica Centro"),
        Recordatorio(3, "Pagar la factura de luz", hogar, Prioridad.Alta, EstadoRecordatorio.Vencido(2)),
        Recordatorio(4, "Estudiar para el parcial", estudio, Prioridad.Baja, EstadoRecordatorio.Pendiente,
            descripcion = "Repasar los temas del Módulo 1"),
        Recordatorio(5, "Comprar alimento del perro", hogar, Prioridad.Media, EstadoRecordatorio.Pendiente,
            ubicacion = "Veterinaria del barrio"),
        Recordatorio(6, "Reunión de equipo", trabajo, Prioridad.Media, EstadoRecordatorio.Completado("22/10/2026")),
        // Inválido a propósito: la validación lo descarta
        Recordatorio(7, "", salud, Prioridad.Baja, EstadoRecordatorio.Pendiente)
    )
}