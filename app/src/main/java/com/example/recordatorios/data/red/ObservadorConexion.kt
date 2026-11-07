package com.example.recordatorios.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

// Convierte los avisos del sistema (NetworkCallback) en un Flow<Boolean>.
// Es solo una señal: aunque diga "hay red", cada solicitud igual puede fallar y se controla.
class ObservadorConexion(context: Context, alcance: CoroutineScope) {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    val hayConexion: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        // Cuando nadie más escucha, se libera el callback para no gastar recursos
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .stateIn(alcance, SharingStarted.Eagerly, initialValue = hayRedAhora())

    private fun hayRedAhora(): Boolean {
        val red = connectivityManager.activeNetwork ?: return false
        val capacidades = connectivityManager.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
