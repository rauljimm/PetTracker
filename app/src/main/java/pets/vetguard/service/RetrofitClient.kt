package pets.vetguard.service

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    private const val TAG = "RetrofitClient"
    var jwtToken: String? = null

    // Callback para manejar tokens expirados
    var onTokenExpired: (() -> Unit)? = null

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url.toString()
            val requestBuilder = originalRequest.newBuilder()
                .header("Content-Type", "application/json") // Asegura el tipo de contenido

            // No agregar el token a endpoints públicos como login y register
            val isPublicEndpoint = url.endsWith("/auth/login") || url.endsWith("/auth/register")
            if (!isPublicEndpoint && !jwtToken.isNullOrEmpty()) {
                Log.d(TAG, "Añadiendo token a la solicitud: Bearer ${jwtToken?.take(10)}...")
                requestBuilder.header("Authorization", "Bearer $jwtToken")
            } else {
                Log.d(TAG, "Omitiendo token para la solicitud: $url")
            }

            val request = requestBuilder.build()
            Log.d(TAG, "Solicitud: ${request.method} ${request.url}, Headers: ${request.headers}")

            val response = chain.proceed(request)
            Log.d(TAG, "Respuesta de ${request.url}: ${response.code}, Body: ${response.peekBody(2048).string()}")

            // Maneja 401 (token expirado) o 403 (permisos insuficientes)
            if (response.code == 401) {
                Log.w(TAG, "Token expirado detectado (401). Invocando onTokenExpired.")
                onTokenExpired?.invoke()
            } else if (response.code == 403) {
                Log.w(TAG, "Acceso denegado (403). Posible token inválido o permisos insuficientes.")
            }
            response
        }
        .build()

    val apiServices: ApiServices by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiServices::class.java)
    }

    // Método para actualizar el token
    fun updateToken(newToken: String) {
        jwtToken = newToken
        if (newToken.isNotEmpty()) {
            Log.d(TAG, "Token actualizado: ${newToken.take(10)}...")
        } else {
            Log.w(TAG, "Token actualizado a valor vacío")
        }
    }
}