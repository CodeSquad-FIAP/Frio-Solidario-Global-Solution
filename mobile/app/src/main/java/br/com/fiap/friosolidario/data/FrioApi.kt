package br.com.fiap.friosolidario.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** As rotas da API que o app usa. O token entra pelo interceptor de `core/Rede.kt`. */
interface FrioApi {

    @POST("api/auth/login")
    suspend fun login(@Body corpo: LoginRequest): LoginResponse

    @GET("api/equipamentos")
    suspend fun equipamentos(@Query("ativo") ativo: Boolean? = true): List<EquipamentoDto>

    @GET("api/equipamentos/{id}")
    suspend fun equipamento(@Path("id") id: Long): EquipamentoDto

    @POST("api/equipamentos")
    suspend fun criarEquipamento(@Body corpo: EquipamentoRequest): EquipamentoDto

    /** Série em ordem cronológica (a mais antiga primeiro). [periodo] é `24h` ou `7d`. */
    /** 200 com a leitura mais recente, ou 204 (corpo vazio) se o equipamento nunca enviou nenhuma. */
    @GET("api/equipamentos/{id}/leituras/ultima")
    suspend fun ultimaLeitura(@Path("id") equipamentoId: Long): retrofit2.Response<LeituraDto>

    @GET("api/equipamentos/{id}/leituras")
    suspend fun leituras(@Path("id") equipamentoId: Long, @Query("periodo") periodo: String): List<LeituraDto>

    @GET("api/alertas")
    suspend fun alertas(@Query("status") status: StatusAlerta): List<AlertaDto>

    @PATCH("api/alertas/{id}/resolver")
    suspend fun resolverAlerta(@Path("id") id: Long, @Body corpo: ResolverAlertaRequest): AlertaDto

    @GET("api/lotes")
    suspend fun lotes(@Query("status") status: StatusLote? = null): List<LoteDto>

    @POST("api/lotes")
    suspend fun criarLote(@Body corpo: LoteRequest): LoteDto

    @PATCH("api/lotes/{id}/retirada")
    suspend fun retirarLote(@Path("id") id: Long, @Body corpo: RetiradaDeLoteRequest): LoteDto

    /** O corpo é o PDF. `de`/`ate` em `yyyy-MM-dd`. */
    @Streaming
    @GET("api/equipamentos/{id}/relatorio")
    suspend fun relatorio(
        @Path("id") equipamentoId: Long,
        @Query("de") de: String,
        @Query("ate") ate: String,
    ): Response<ResponseBody>
}
