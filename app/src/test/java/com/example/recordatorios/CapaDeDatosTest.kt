package com.example.recordatorios

import com.example.recordatorios.data.aEntidad
import com.example.recordatorios.data.aModelo
import com.example.recordatorios.data.aRemoto
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.Tema
import com.example.recordatorios.data.preferencias.ordenDesdeTexto
import com.example.recordatorios.data.preferencias.temaDesdeTexto
import com.example.recordatorios.data.remoto.RecordatorioRemoto
import com.example.recordatorios.data.resolverConflicto
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de las funciones puras de la capa de datos (corren en la PC, sin emulador ni Firebase)
class CapaDeDatosTest {

    private val remoto = RecordatorioRemoto(
        id = "abc", uid = "uid-ana", titulo = "Pagar la luz", descripcion = null, categoria = "Hogar",
        prioridad = "ALTA", completado = false, fechaCompletado = null, fechaCreacion = 1_000, actualizadoEn = 2_000
    )

    @Test
    fun documento_remoto_se_convierte_en_entidad_sincronizada_de_la_cuenta() {
        val entidad = remoto.aEntidad("uid-ana")
        assertEquals("ALTA", entidad.prioridad)
        assertEquals(EstadoSync.SINCRONIZADO, entidad.estadoSync)
        assertEquals("abc", entidad.id)
        assertEquals("uid-ana", entidad.uid)
    }

    @Test
    fun prioridad_desconocida_de_la_nube_queda_como_media() {
        val entidad = remoto.copy(prioridad = "URGENTISIMA").aEntidad("uid-ana")
        assertEquals(Prioridad.Media, entidad.aModelo().prioridad)
    }

    @Test
    fun ida_y_vuelta_entidad_remoto_conserva_los_datos_y_el_id() {
        // El id es el mismo en Room y en Firestore: base de la sincronización sin duplicados
        val entidad = remoto.aEntidad("uid-ana")
        assertEquals(remoto, entidad.aRemoto())
    }

    @Test
    fun ida_y_vuelta_por_mapa_de_firestore_conserva_los_datos() {
        val leido = RecordatorioRemoto.desdeMapa("abc", remoto.aMapa())
        assertEquals(remoto, leido)
    }

    @Test
    fun documento_con_campos_faltantes_o_de_otro_tipo_se_ignora() {
        assertNull(RecordatorioRemoto.desdeMapa("abc", null))
        assertNull(RecordatorioRemoto.desdeMapa("abc", remoto.aMapa() - RecordatorioRemoto.CAMPO_TITULO))
        assertNull(RecordatorioRemoto.desdeMapa("abc", remoto.aMapa() + (RecordatorioRemoto.CAMPO_ACTUALIZADO_EN to "ayer")))
    }

    @Test
    fun entidad_completada_se_convierte_en_estado_completado() {
        val entidad = remoto.copy(completado = true, fechaCompletado = 5_000).aEntidad("uid-ana")
        assertEquals(EstadoRecordatorio.Completado(5_000), entidad.aModelo().estado)
    }

    @Test
    fun conflicto_gana_la_version_mas_reciente() {
        val local: RecordatorioEntity = remoto.aEntidad("uid-ana")
            .copy(titulo = "Local", actualizadoEn = 3_000, estadoSync = EstadoSync.PENDIENTE)
        // La nube tiene una versión más vieja: se conserva la local
        assertSame(local, resolverConflicto(local, remoto))
        // La nube tiene una versión más nueva (por ejemplo, editada en otro dispositivo): gana la remota
        val ganador = resolverConflicto(local, remoto.copy(titulo = "Remoto", actualizadoEn = 9_000))
        assertEquals("Remoto", ganador.titulo)
        assertEquals(EstadoSync.SINCRONIZADO, ganador.estadoSync)
        assertEquals("uid-ana", ganador.uid)
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
