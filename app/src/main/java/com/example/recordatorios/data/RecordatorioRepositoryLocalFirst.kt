package com.example.recordatorios.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.red.ObservadorConexion
import com.example.recordatorios.data.remoto.FuenteRemota
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.data.remoto.ResultadoRemoto
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.ordenarPorFecha
import com.example.recordatorios.domain.ordenarPorPrioridad
import com.example.recordatorios.domain.ordenarPorTitulo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

private const val ETIQUETA_LOG = "Repositorio"
private const val INTENTOS_MAXIMOS = 3
private const val ESPERA_INICIAL_MS = 2_000L

// Repositorio "local-first": Room es la fuente de verdad para la interfaz.
// El servidor solo actualiza Room; las pantallas nunca leen directamente de la red.
class RecordatorioRepositoryLocalFirst(
    private val dao: RecordatorioDao,
    private val remota: FuenteRemota,
    private val preferencias: PreferenciasRepository,
    private val conexion: ObservadorConexion,
    private val alcance: CoroutineScope          // alcance de toda la app (no se cancela al cambiar de pantalla)
) : RecordatorioRepository {

    private val _estadoSincronizacion = MutableStateFlow<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
    override val estadoSincronizacion: StateFlow<EstadoSincronizacion> = _estadoSincronizacion.asStateFlow()

    override val hayConexion: StateFlow<Boolean> = conexion.hayConexion

    // Mutex: permite que una sola sincronización corra a la vez aunque la pidan varios lugares
    private val candado = Mutex()

    // Si llega un pedido mientras otra sincronización corre, se repite una vez al terminar
    @Volatile
    private var repetirAlTerminar = false

    init {
        // Cada vez que vuelve la conexión (y al abrir la app con red) se sincroniza.
        // collectLatest: si la red se corta en el medio, se cancela lo que estaba esperando.
        alcance.launch {
            conexion.hayConexion.collectLatest { hayRed ->
                if (hayRed) sincronizar()
            }
        }
    }

    // ---------------------------------------------------------------- lecturas (Room)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarRecordatorios(): Flow<List<Recordatorio>> =
        // Coordina DataStore y Room: según la preferencia se usa una consulta u otra del DAO
        preferencias.preferencias.flatMapLatest { prefs ->
            val consulta = if (prefs.mostrarCompletados) dao.observarTodos() else dao.observarSinCompletar()
            consulta.map { filas ->
                val lista = filas.map { it.aModelo() }
                when (prefs.orden) {
                    OrdenListado.PRIORIDAD -> lista.ordenarPorPrioridad()
                    OrdenListado.FECHA -> lista.ordenarPorFecha()
                    OrdenListado.TITULO -> lista.ordenarPorTitulo()
                }
            }
        }

    override fun observarRecordatorio(id: String): Flow<Recordatorio?> =
        dao.observarPorId(id).map { it?.aModelo() }

    override suspend fun buscarRecordatorio(id: String): Recordatorio? =
        dao.buscarPorId(id)?.takeIf { it.estadoSync != EstadoSync.PENDIENTE_BORRAR }?.aModelo()

    override fun observarCantidadSinSincronizar(): Flow<Int> = dao.observarCantidadSinSincronizar()

    override fun obtenerCategorias(): List<Categoria> = Categorias.todas

    // ---------------------------------------------------------------- escrituras (primero Room)

    override suspend fun guardarRecordatorio(recordatorio: Recordatorio): ResultadoOperacion {
        val ahora = System.currentTimeMillis()
        // Recordatorio nuevo: se le crea un id estable acá, una sola vez
        val nuevo = recordatorio.id.isBlank()
        val aGuardar = recordatorio.copy(
            id = if (nuevo) UUID.randomUUID().toString() else recordatorio.id,
            fechaCreacion = if (nuevo) ahora else recordatorio.fechaCreacion,
            actualizadoEn = ahora
        )
        return operacionLocal("guardar") {
            dao.guardar(aGuardar.aEntidad(EstadoSync.PENDIENTE))
        }
    }

    override suspend fun cambiarCompletado(id: String, completado: Boolean): ResultadoOperacion {
        val ahora = System.currentTimeMillis()
        return operacionLocal("actualizar") {
            dao.cambiarCompletado(id, completado, fechaCompletado = if (completado) ahora else null, ahora = ahora)
        }
    }

    // No se borra enseguida: se marca PENDIENTE_BORRAR (desaparece de la pantalla) y se borra
    // de Room recién cuando el servidor lo confirma. Si el servidor nunca lo tuvo, responde 404
    // y eso también se toma como confirmación.
    override suspend fun eliminarRecordatorio(id: String): ResultadoOperacion =
        operacionLocal("eliminar") {
            dao.marcarParaBorrar(id, System.currentTimeMillis())
        }

    // Ejecuta la escritura en Room. Si falla, devuelve un mensaje claro en vez de cerrar la app;
    // el detalle técnico queda en el registro (Logcat) para el equipo.
    private suspend fun operacionLocal(nombre: String, bloque: suspend () -> Unit): ResultadoOperacion =
        try {
            bloque()
            solicitarSincronizacion()
            ResultadoOperacion.Exito
        } catch (e: SQLiteFullException) {
            Log.e(ETIQUETA_LOG, "Sin espacio al $nombre", e)
            ResultadoOperacion.Error("No hay espacio suficiente para guardar el recordatorio. Liberá espacio y volvé a intentarlo.")
        } catch (e: SQLiteException) {
            Log.e(ETIQUETA_LOG, "Error de base de datos al $nombre", e)
            ResultadoOperacion.Error("No se pudo $nombre el recordatorio en el teléfono. Intentá nuevamente.")
        }

    // ---------------------------------------------------------------- sincronización

    // Después de un cambio local se intenta enviar enseguida, solo si hay red
    private fun solicitarSincronizacion() {
        if (conexion.hayConexion.value) {
            alcance.launch { sincronizar() }
        }
    }

    // Punto de entrada de toda sincronización (automática o con el botón).
    // Reintentos controlados: solo ante errores temporales (timeout o 5xx), con un máximo
    // de 3 intentos y una espera que se duplica (2 s, 4 s) para no saturar al servidor.
    override suspend fun sincronizar(): ResultadoSincronizacion {
        var espera = ESPERA_INICIAL_MS
        repeat(INTENTOS_MAXIMOS - 1) {
            val resultado = intentarSincronizar()
            if (resultado !is ResultadoSincronizacion.ErrorReintentable) return resultado
            delay(espera)
            espera *= 2
        }
        return intentarSincronizar()
    }

    private suspend fun intentarSincronizar(): ResultadoSincronizacion {
        // tryLock: si ya hay una sincronización corriendo no se inicia otra (evita envíos repetidos)
        if (!candado.tryLock()) {
            repetirAlTerminar = true
            return ResultadoSincronizacion.EnCurso
        }
        try {
            _estadoSincronizacion.value = EstadoSincronizacion.EnCurso
            var resultado: ResultadoSincronizacion
            do {
                repetirAlTerminar = false
                resultado = sincronizarUnaVez()
            } while (repetirAlTerminar && resultado is ResultadoSincronizacion.Exito)
            _estadoSincronizacion.value = EstadoSincronizacion.Terminada(resultado, System.currentTimeMillis())
            return resultado
        } finally {
            // Si la coroutine se canceló a mitad de camino, no queda "sincronizando" para siempre
            if (_estadoSincronizacion.value == EstadoSincronizacion.EnCurso) {
                _estadoSincronizacion.value = EstadoSincronizacion.Inactiva
            }
            candado.unlock()
        }
    }

    // 1) envía lo pendiente  2) descarga los cambios del servidor
    private suspend fun sincronizarUnaVez(): ResultadoSincronizacion {
        var enviados = 0
        var rechazados = 0

        for (pendiente in dao.buscarPendientes()) {
            if (pendiente.estadoSync == EstadoSync.PENDIENTE_BORRAR) {
                when (val respuesta = remota.eliminar(pendiente.id)) {
                    is ResultadoRemoto.Exito -> {
                        dao.eliminarDefinitivo(pendiente.id)
                        enviados++
                    }
                    else -> return traducirFallo(respuesta)
                }
            } else {
                when (val respuesta = remota.guardar(pendiente.aDto())) {
                    is ResultadoRemoto.Exito -> {
                        // Mismo id: el servidor actualiza el registro y no crea uno nuevo
                        dao.marcarSincronizado(pendiente.id, pendiente.actualizadoEn)
                        enviados++
                    }
                    is ResultadoRemoto.ErrorHttp -> {
                        // 400, 404, 422...: el servidor rechazó ESTE recordatorio. Se marca con error
                        // y se sigue con los demás. El 401 (clave) y los 5xx cortan la sincronización.
                        if (respuesta.codigo in 400..499 && respuesta.codigo != 401) {
                            Log.w(ETIQUETA_LOG, "El servidor rechazó ${pendiente.id} con ${respuesta.codigo}")
                            dao.marcarConError(pendiente.id)
                            rechazados++
                        } else {
                            return traducirFallo(respuesta)
                        }
                    }
                    else -> return traducirFallo(respuesta)
                }
            }
        }

        val descargados = when (val respuesta = remota.listar()) {
            is ResultadoRemoto.Exito -> incorporarCambiosRemotos(respuesta.datos)
            else -> return traducirFallo(respuesta)
        }
        return ResultadoSincronizacion.Exito(enviados, descargados, rechazados)
    }

    // Guarda en Room lo que llegó del servidor (transformado de DTO a entidad)
    private suspend fun incorporarCambiosRemotos(remotos: List<RecordatorioDto>): Int {
        var cambios = 0
        val idsRemotos = remotos.map { it.id }.toSet()
        for (dto in remotos) {
            val local = dao.buscarPorId(dto.id)
            val aGuardar = when {
                local == null -> dto.aEntidad()                                        // nuevo en el servidor
                local.estadoSync == EstadoSync.SINCRONIZADO -> dto.aEntidad()          // sin cambios locales
                local.estadoSync == EstadoSync.PENDIENTE_BORRAR -> null                // se respeta el borrado local
                else -> resolverConflicto(local, dto)                                  // cambió en los dos lados
            }
            if (aGuardar != null && aGuardar != local) {
                dao.guardar(aGuardar)
                cambios++
            }
        }
        // Los que estaban sincronizados y ya no existen en el servidor fueron borrados desde otro lado
        for (id in dao.buscarIdsSincronizados()) {
            if (id !in idsRemotos) {
                dao.eliminarDefinitivo(id)
                cambios++
            }
        }
        return cambios
    }

    // Decide qué significa cada falla de red para la sincronización
    private fun traducirFallo(respuesta: ResultadoRemoto<*>): ResultadoSincronizacion = when (respuesta) {
        is ResultadoRemoto.Exito -> ResultadoSincronizacion.Exito(0, 0, 0)
        ResultadoRemoto.SinConexion -> ResultadoSincronizacion.SinConexion
        ResultadoRemoto.Timeout -> ResultadoSincronizacion.ErrorReintentable("El servidor no respondió a tiempo")
        is ResultadoRemoto.ErrorHttp -> when {
            respuesta.codigo >= 500 -> ResultadoSincronizacion.ErrorReintentable("El servidor tuvo un error (${respuesta.codigo})")
            respuesta.codigo == 401 -> ResultadoSincronizacion.ErrorPermanente("El servidor rechazó la clave de acceso (401)")
            else -> ResultadoSincronizacion.ErrorPermanente("El servidor rechazó la solicitud (${respuesta.codigo})")
        }
        is ResultadoRemoto.DatosInvalidos -> {
            Log.e(ETIQUETA_LOG, "Respuesta inválida del servidor", respuesta.causa)
            ResultadoSincronizacion.ErrorPermanente("El servidor envió datos que no se pudieron leer")
        }
    }
}
