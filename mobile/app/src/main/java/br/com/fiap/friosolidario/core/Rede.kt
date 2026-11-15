package br.com.fiap.friosolidario.core

import br.com.fiap.friosolidario.data.FrioApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

fun criarJson(): Json = Json {
    ignoreUnknownKeys = true // campo novo na API não derruba o app
    explicitNulls = false
    encodeDefaults = true
}

/** [token] é consultado a cada pedido; o login é a única rota que vai sem `Authorization`. */
fun criarOkHttp(timeoutSegundos: Long = ApiConfig.TIMEOUT_SEGUNDOS, token: () -> String?): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(timeoutSegundos, TimeUnit.SECONDS)
        .readTimeout(timeoutSegundos, TimeUnit.SECONDS)
        .writeTimeout(timeoutSegundos, TimeUnit.SECONDS)
        .addInterceptor { cadeia ->
            val pedido = cadeia.request()
            val atual = token()
            if (atual == null || pedido.url.encodedPath.endsWith("/auth/login")) {
                cadeia.proceed(pedido)
            } else {
                cadeia.proceed(pedido.newBuilder().header("Authorization", "Bearer $atual").build())
            }
        }
        .build()

fun criarApi(baseUrl: String, cliente: OkHttpClient, json: Json): FrioApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FrioApi::class.java)
