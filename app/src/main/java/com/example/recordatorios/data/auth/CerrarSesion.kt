package com.example.recordatorios.data.auth

import com.example.recordatorios.data.RecordatorioRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// Caso de uso "cerrar sesión": coordina las dos fuentes involucradas.
//  1) borra del teléfono (Room) los recordatorios de la cuenta; en la nube siguen guardados
//  2) cierra la sesión de Firebase
// Así la próxima persona que use el dispositivo no encuentra datos de la cuenta anterior.
class CerrarSesion(
    private val auth: AuthRepository,
    private val recordatorios: RecordatorioRepository
) {
    suspend operator fun invoke() {
        // NonCancellable: el borrado termina aunque la pantalla que lo pidió se cierre en el medio
        // (al cerrar sesión, la raíz de la app descarta Ajustes y su ViewModel).
        withContext(NonCancellable) {
            val uid = (auth.estado.value as? EstadoSesion.Activa)?.usuario?.uid
            if (uid != null) recordatorios.borrarDatosLocales(uid)
            auth.cerrarSesion()
        }
    }
}
