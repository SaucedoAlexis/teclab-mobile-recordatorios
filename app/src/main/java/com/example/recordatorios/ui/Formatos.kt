package com.example.recordatorios.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Convierte los milisegundos guardados en Room en un texto para mostrar
private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR"))
private val formatoHora = SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-AR"))

fun formatearFecha(milisegundos: Long): String = formatoFechaHora.format(Date(milisegundos))

fun formatearHora(milisegundos: Long): String = formatoHora.format(Date(milisegundos))
