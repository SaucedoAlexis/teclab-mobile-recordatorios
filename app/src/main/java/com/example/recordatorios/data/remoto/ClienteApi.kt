package com.example.recordatorios.data.remoto

import com.example.recordatorios.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Agrega la clave del servicio a TODAS las solicitudes, sin repetirlo en cada endpoint.
// La clave viene de api.properties (BuildConfig), no está escrita en el código.
class ClaveApiInterceptor(private val clave: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitud = if (clave.isNotBlank()) {
            chain.request().newBuilder().addHeader("X-Api-Key", clave).build()
        } else {
            chain.request()
        }
        return chain.proceed(solicitud)
    }
}

object ClienteApi {

    // Si el servidor manda campos que la app no conoce, se ignoran en vez de fallar
    private val json = Json { ignoreUnknownKeys = true }

    fun crear(
        urlBase: String = BuildConfig.API_BASE_URL,   // configurable en api.properties (tiene que terminar con /)
        clave: String = BuildConfig.API_KEY
    ): RecordatoriosApi {
        val clienteHttp = OkHttpClient.Builder()
            // Tiempos máximos: si el servidor no responde, la solicitud termina con timeout
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(ClaveApiInterceptor(clave))
            .apply {
                // Registro de las llamadas en Logcat solo en debug. BASIC no muestra encabezados,
                // así que la clave no aparece en el registro.
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(clienteHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RecordatoriosApi::class.java)
    }
}
