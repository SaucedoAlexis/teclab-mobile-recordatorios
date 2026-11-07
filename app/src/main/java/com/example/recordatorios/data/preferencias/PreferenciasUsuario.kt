package com.example.recordatorios.data.preferencias

// Configuraciones simples que se guardan con DataStore (no son recordatorios)
enum class Tema(val etiqueta: String) {
    SISTEMA("Igual que el sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

enum class OrdenListado(val etiqueta: String) {
    PRIORIDAD("Por prioridad"),
    FECHA("Más nuevos primero"),
    TITULO("Por título (A-Z)")
}

// Modelo propio: el resto de la app trabaja con esto y no con las claves internas de DataStore.
// Los valores por defecto se usan cuando todavía no se guardó nada o el dato guardado no sirve.
data class PreferenciasUsuario(
    val tema: Tema = Tema.SISTEMA,
    val mostrarCompletados: Boolean = true,
    val orden: OrdenListado = OrdenListado.PRIORIDAD,
    // true cuando lo guardado no era una opción válida y se usó el valor por defecto
    val ordenGuardadoInvalido: Boolean = false,
    val temaGuardadoInvalido: Boolean = false
)

// Convierte el texto guardado en una opción. Si no coincide con ninguna devuelve null.
fun temaDesdeTexto(texto: String?): Tema? = Tema.entries.firstOrNull { it.name == texto }

fun ordenDesdeTexto(texto: String?): OrdenListado? = OrdenListado.entries.firstOrNull { it.name == texto }
