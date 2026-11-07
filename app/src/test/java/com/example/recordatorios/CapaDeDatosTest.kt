package com.example.recordatorios

import com.example.recordatorios.data.aDto
import com.example.recordatorios.data.aEntidad
import com.example.recordatorios.data.aModelo
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.Tema
import com.example.recordatorios.data.preferencias.ordenDesdeTexto
import com.example.recordatorios.data.preferencias.temaDesdeTexto
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.data.resolverConflicto
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de las funciones puras de la capa de datos (corren en la PC, sin emulador)
class CapaDeDatosTest {

    private val dto = RecordatorioDto(
        id = "abc", title = "Pagar la luz", description = null, category = "Hogar",
        priority = "HIGH", done = false, doneAt = null, createdAt = 1_000, updatedAt = 2_000
    )

    @Test
    fun dto_se_convierte_en_entidad_sincronizada() {
        val entidad = dto.aEntidad()
        assertEquals("ALTA", entidad.prioridad)
        assertEquals(EstadoSync.SINCRONIZADO, entidad.estadoSync)
        assertEquals("abc", entidad.id)
    }

    @Test
    fun prioridad_desconocida_del_servidor_queda_como_media() {
        val entidad = dto.copy(priority = "URGENTISIMA").aEntidad()
        assertEquals(Prioridad.Media, entidad.aModelo().prioridad)
    }

    @Test
    fun ida_y_vuelta_entidad_dto_conserva_los_datos() {
        val entidad = dto.aEntidad()
        assertEquals(dto, entidad.aDto())
    }

    @Test
    fun entidad_completada_se_convierte_en_estado_completado() {
        val entidad = dto.copy(done = true, doneAt = 5_000).aEntidad()
        assertEquals(EstadoRecordatorio.Completado(5_000), entidad.aModelo().estado)
    }

    @Test
    fun conflicto_gana_la_version_mas_reciente() {
        val local: RecordatorioEntity = dto.aEntidad().copy(titulo = "Local", actualizadoEn = 3_000, estadoSync = EstadoSync.PENDIENTE)
        // El servidor tiene una versión más vieja: se conserva la local
        assertSame(local, resolverConflicto(local, dto))
        // El servidor tiene una versión más nueva: gana la remota
        val ganador = resolverConflicto(local, dto.copy(title = "Remoto", updatedAt = 9_000))
        assertEquals("Remoto", ganador.titulo)
        assertEquals(EstadoSync.SINCRONIZADO, ganador.estadoSync)
    }

    @Test
    fun preferencias_invalidas_devuelven_null_para_usar_el_valor_por_defecto() {
        assertEquals(OrdenListado.FECHA, ordenDesdeTexto("FECHA"))
        assertNull(ordenDesdeTexto("ORDEN_QUE_NO_EXISTE"))
        assertNull(ordenDesdeTexto(null))
        assertEquals(Tema.OSCURO, temaDesdeTexto("OSCURO"))
        assertTrue(temaDesdeTexto("violeta") == null)
    }
}
