package com.example.recordatorios.data.preferencias

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// ÚNICA instancia de DataStore para toda la app (el delegado se declara una sola vez, en el nivel superior).
// Se guarda en el archivo files/datastore/preferencias_usuario.preferences_pb del teléfono.
val Context.dataStorePreferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias_usuario")

// Claves tipadas. El texto de cada clave no se debe cambiar: si cambia, se pierde lo guardado.
private object Claves {
    val TEMA = stringPreferencesKey("tema")
    val MOSTRAR_COMPLETADOS = booleanPreferencesKey("mostrar_completados")
    val ORDEN = stringPreferencesKey("orden_listado")
}

class PreferenciasRepository(private val dataStore: DataStore<Preferences>) {

    // Flow: emite un valor nuevo cada vez que cambia alguna preferencia
    val preferencias: Flow<PreferenciasUsuario> = dataStore.data
        .catch { error ->
            // Error de lectura recuperable: se sigue con los valores por defecto
            if (error is IOException) {
                Log.e("Preferencias", "No se pudieron leer las preferencias", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { valores ->
            val temaGuardado = valores[Claves.TEMA]
            val ordenGuardado = valores[Claves.ORDEN]
            val tema = temaDesdeTexto(temaGuardado)
            val orden = ordenDesdeTexto(ordenGuardado)
            PreferenciasUsuario(
                tema = tema ?: Tema.SISTEMA,
                mostrarCompletados = valores[Claves.MOSTRAR_COMPLETADOS] ?: true,
                orden = orden ?: OrdenListado.PRIORIDAD,
                // Había algo guardado pero no era una opción conocida
                temaGuardadoInvalido = temaGuardado != null && tema == null,
                ordenGuardadoInvalido = ordenGuardado != null && orden == null
            )
        }

    // edit es una función suspendida: se llama desde una coroutine y no bloquea la interfaz
    suspend fun cambiarTema(tema: Tema) {
        dataStore.edit { it[Claves.TEMA] = tema.name }
    }

    suspend fun cambiarMostrarCompletados(mostrar: Boolean) {
        dataStore.edit { it[Claves.MOSTRAR_COMPLETADOS] = mostrar }
    }

    suspend fun cambiarOrden(orden: OrdenListado) {
        dataStore.edit { it[Claves.ORDEN] = orden.name }
    }

    // SOLO PARA PRUEBAS: guarda un texto que no es ninguna opción, para comprobar que la app
    // no se rompe y vuelve al orden por defecto (lo pide la consigna 1)
    suspend fun guardarOrdenInvalidoDePrueba() {
        dataStore.edit { it[Claves.ORDEN] = "ORDEN_QUE_NO_EXISTE" }
    }
}
