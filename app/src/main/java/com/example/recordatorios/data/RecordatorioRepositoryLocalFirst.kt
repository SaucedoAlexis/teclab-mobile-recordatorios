package com.example.recordatorios.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.EstadoSesion
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.red.ObservadorConexion
import com.example.recordatorios.data.remoto.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remoto.RecordatorioRemoto
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

private const val ETIQUETA_LOG = "Repositorio"
private const val INTENTOS_MAXIMOS = 3
private const val ESPERA_INICIAL_MS = 2_000L

// Repositorio "local-first": Room es la fuente de verdad para la interfaz.
// La nube (Firestore) solo actualiza Room; las pantallas nunca leen directamente de la red.
// Actividad 4: todo se hace en nombre de la cuenta activa (uid) y la nube guarda users/{uid}/reminders/{id}.
class RecordatorioRepositoryLocalFirst(
    private val dao: RecordatorioDao,
    private val remota: FuenteRemotaRecordatorios,   // Actividad 4: Cloud Firestore
    private val preferencias: PreferenciasRepository,
    private val conexion: ObservadorConexion,
    private val auth: AuthRepository,            // Actividad 4: de acá sale la cuenta activa (uid)
    private val alcance: CoroutineScope          // alcance de toda la app (no se cancela al cambiar de pantalla)
) : RecordatorioRepository {

    // uid de la cuenta activa, o null si no hay sesión
    private val uidActivo: Flow<String?> = auth.estado
        .map { (it as? EstadoSesion.Activa)?.usuario?.uid }
        .distinctUntilChanged()

    private fun uidActual(): String? = (auth.estado.value as? EstadoSesion.Activa)?.usuario?.uid

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
                if (hayRed && uidActual() != null) sincronizar()
            }
        }
        // Cada vez que inicia sesión una cuenta: se borra del teléfono lo que haya quedado de otras
        // cuentas (por ejemplo, si la sesión anterior se invalidó sin pasar por "Cerrar sesión")
        // y se sincroniza para traer sus recordatorios desde la nube.
        alcance.launch {
            uidActivo.collectLatest { uid ->
                if (uid == null) return@collectLatest
                val borradas = dao.eliminarDeOtrasCuentas(uid)
                if (borradas > 0) Log.i(ETIQUETA_LOG, "Se borraron $borradas filas de otras cuentas")
                if (conexion.hayConexion.value) sincronizar()
            }
        }
    }

    // ---------------------------------------------------------------- lecturas (Room)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarRecordatorios(): Flow<List<Recordatorio>> =
        // Coordina la sesión, DataStore y Room: según la cuenta y la preferencia se usa una consulta u otra.
        // Sin sesión, la lista es vacía.
        combine(uidActivo, preferencias.preferencias) { uid, prefs -> uid to prefs }.flatMapLatest { (uid, prefs) ->
            if (uid == null) return@flatMapLatest flowOf(emptyList())
            val consulta = if (prefs.mostrarCompletados) dao.observarTodos(uid) else dao.observarSinCompletar(uid)
            consulta.map { filas ->
                val lista = filas.map { it.aModelo() }
                when (prefs.orden) {
                    OrdenListado.PRIORIDAD -> lista.ordenarPorPrioridad()
                    OrdenListado.FECHA -> lista.ordenarPorFecha()
                    OrdenListado.TITULO -> lista.ordenarPorTitulo()
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarRecordatorio(id: String): Flow<Recordatorio?> =
        uidActivo.flatMapLatest { uid ->
            if (uid == null) flowOf(null) else dao.observarPorId(id, uid).map { it?.aModelo() }
        }

    override suspend fun buscarRecordatorio(id: String): Recordatorio? {
        val uid = uidActual() ?: return null
        return dao.buscarPorId(id, uid)?.takeIf { it.estadoSync != EstadoSync.PENDIENTE_BORRAR }?.aModelo()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarCantidadSinSincronizar(): Flow<Int> =
        uidActivo.flatMapLatest { uid -> if (uid == null) flowOf(0) else dao.observarCantidadSinSincronizar(uid) }

    override suspend fun borrarDatosLocales(uid: String) {
        val borradas = dao.eliminarDeCuenta(uid)
        Log.i(ETIQUETA_LOG, "Cierre de sesión: se borraron $borradas recordatorios locales de la cuenta")
    }

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
        return operacionLocal("guardar") { uid ->
            dao.guardar(aGuardar.aEntidad(EstadoSync.PENDIENTE, uid))
        }
    }

    override suspend fun cambiarCompletado(id: String, completado: Boolean): ResultadoOperacion {
        val ahora = System.currentTimeMillis()
        return operacionLocal("actualizar") { uid ->
            dao.cambiarCompletado(id, uid, completado, fechaCompletado = if (completado) ahora else null, ahora = ahora)
        }
    }

    // No se borra enseguida: se marca PENDIENTE_BORRAR (desaparece de la pantalla) y se borra
    // de Room recién cuando la nube lo confirma. Borrar un documento que no existe también es éxito.
    override suspend fun eliminarRecordatorio(id: String): ResultadoOperacion =
        operacionLocal("eliminar") { uid ->
            dao.marcarParaBorrar(id, uid, System.currentTimeMillis())
        }

    // Ejecuta la escritura en Room. Si falla, devuelve un mensaje claro en vez de cerrar la app;
    // el detalle técnico queda en el registro (Logcat) para el equipo.
    // Sin sesión activa no se escribe nada: cada fila tiene que tener dueño.
    private suspend fun operacionLocal(nombre: String, bloque: suspend (uid: String) -> Unit): ResultadoOperacion {
        val uid = uidActual() ?: return ResultadoOperacion.Error("La sesión se cerró. Iniciá sesión para guardar cambios.")
        return try {
            bloque(uid)
            solicitarSincronizacion()
            ResultadoOperacion.Exito
        } catch (e: SQLiteFullException) {
            Log.e(ETIQUETA_LOG, "Sin espacio al $nombre", e)
            ResultadoOperacion.Error("No hay espacio suficiente para guardar el recordatorio. Liberá espacio y volvé a intentarlo.")
        } catch (e: SQLiteException) {
            Log.e(ETIQUETA_LOG, "Error de base de datos al $nombre", e)
            ResultadoOperacion.Error("No se pudo $nombre el recordatorio en el teléfono. Intentá nuevamente.")
        }
    }

    // ---------------------------------------------------------------- sincronización

    // Después de un cambio local se intenta enviar enseguida, solo si hay red
    private fun solicitarSincronizacion() {
        if (conexion.hayConexion.value) {
            alcance.launch { sincronizar() }
        }
    }

    // Punto de entrada de toda sincronización (automática o con el botón).
    // Reintentos controlados: solo ante errores temporales (timeout o falla del servicio), con un máximo
    // de 3 intentos y una espera que se duplica (2 s, 4 s) para no saturar al servicio.
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

    // 1) envía lo pendiente  2) descarga los cambios de la nube
    private suspend fun sincronizarUnaVez(): ResultadoSincronizacion {
        val uid = uidActual() ?: return ResultadoSincronizacion.Exito(0, 0, 0)
        var enviados = 0
        var rechazados = 0

        for (pendiente in dao.buscarPendientes(uid)) {
            val respuesta = if (pendiente.estadoSync == EstadoSync.PENDIENTE_BORRAR) {
                remota.eliminar(uid, pendiente.id)
            } else {
                // Mismo id que en Room: la nube reemplaza el documento existente, no crea otro
                remota.guardar(uid, pendiente.aRemoto())
            }
            when (respuesta) {
                is ResultadoRemoto.Exito -> {
                    if (pendiente.estadoSync == EstadoSync.PENDIENTE_BORRAR) dao.eliminarDefinitivo(pendiente.id, uid)
                    else dao.marcarSincronizado(pendiente.id, uid, pendiente.actualizadoEn)
                    enviados++
                }
                is ResultadoRemoto.Rechazado -> {
                    // Las reglas o la validación rechazaron ESTE recordatorio: se marca con error y se sigue
                    Log.w(ETIQUETA_LOG, "La nube rechazó ${pendiente.id}: ${respuesta.motivo}")
                    dao.marcarConError(pendiente.id, uid)
                    rechazados++
                }
                else -> return traducirFallo(respuesta)
            }
        }

        val descargados = when (val respuesta = remota.listar(uid)) {
            is ResultadoRemoto.Exito -> incorporarCambiosRemotos(uid, respuesta.datos)
            else -> return traducirFallo(respuesta)
        }
        return ResultadoSincronizacion.Exito(enviados, descargados, rechazados)
    }

    // Guarda en Room lo que llegó de la nube (transformado de documento a entidad)
    private suspend fun incorporarCambiosRemotos(uid: String, remotos: List<RecordatorioRemoto>): Int {
        var cambios = 0
        val idsRemotos = remotos.map { it.id }.toSet()
        for (remoto in remotos) {
            val local = dao.buscarPorId(remoto.id, uid)
            val aGuardar = when {
                local == null -> remoto.aEntidad(uid)                                   // nuevo (creado en otro dispositivo)
                local.estadoSync == EstadoSync.SINCRONIZADO -> remoto.aEntidad(uid)     // sin cambios locales
                local.estadoSync == EstadoSync.PENDIENTE_BORRAR -> null                 // se respeta el borrado local
                else -> resolverConflicto(local, remoto)                                // cambió en los dos lados
            }
            if (aGuardar != null && aGuardar != local) {
                dao.guardar(aGuardar)
                cambios++
            }
        }
        // Los que estaban sincronizados y ya no existen en la nube fueron borrados desde otro dispositivo
        for (id in dao.buscarIdsSincronizados(uid)) {
            if (id !in idsRemotos) {
                dao.eliminarDefinitivo(id, uid)
                cambios++
            }
        }
        return cambios
    }

    // Decide qué significa cada falla de la nube para la sincronización
    private fun traducirFallo(respuesta: ResultadoRemoto<*>): ResultadoSincronizacion = when (respuesta) {
        is ResultadoRemoto.Exito -> ResultadoSincronizacion.Exito(0, 0, 0)
        ResultadoRemoto.SinConexion -> ResultadoSincronizacion.SinConexion
        ResultadoRemoto.Timeout -> ResultadoSincronizacion.ErrorReintentable("La nube no respondió a tiempo")
        is ResultadoRemoto.ErrorTemporal -> ResultadoSincronizacion.ErrorReintentable(respuesta.motivo)
        is ResultadoRemoto.Rechazado -> ResultadoSincronizacion.ErrorPermanente(respuesta.motivo)
        ResultadoRemoto.NoAutenticado -> {
            // La nube dice que la sesión ya no vale: se cierra y la raíz de la app vuelve al acceso
            auth.invalidarSesion()
            ResultadoSincronizacion.ErrorPermanente("La sesión ya no es válida")
        }
        is ResultadoRemoto.DatosInvalidos -> {
            Log.e(ETIQUETA_LOG, "Respuesta inválida de la nube", respuesta.causa)
            ResultadoSincronizacion.ErrorPermanente("La nube envió datos que no se pudieron leer")
        }
    }
}
