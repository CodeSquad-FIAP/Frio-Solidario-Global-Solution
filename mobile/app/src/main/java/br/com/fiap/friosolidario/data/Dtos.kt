package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.Papel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

// Corpos da API, com os nomes do contrato do README ("Contrato para o app"). Enums trafegam como
// texto; datas vêm em ISO 8601 UTC ("2026-11-10T23:30:00+00:00") e viram Instant.

object InstanteIso : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instante", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Instant = OffsetDateTime.parse(decoder.decodeString()).toInstant()
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
}

object DataIso : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("Data", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}

typealias Instante = @Serializable(InstanteIso::class) Instant
typealias Data = @Serializable(DataIso::class) LocalDate

@Serializable
data class LoginRequest(val email: String, val senha: String)

@Serializable
data class LoginResponse(
    val token: String,
    val expiraEm: Instante,
    val nome: String,
    val papel: Papel,
    val instituicaoId: Long,
    val instituicao: String,
)

enum class TipoEquipamento(val rotulo: String) {
    CAMARA("Câmara fria"),
    GELADEIRA("Geladeira"),
    FREEZER("Freezer"),
}

@Serializable
data class EquipamentoDto(
    val id: Long,
    val instituicaoId: Long,
    val instituicao: String = "",
    val nome: String,
    val tipo: TipoEquipamento,
    val tempMin: Double,
    val tempMax: Double,
    val umidMax: Double,
    val ativo: Boolean = true,
    /** Vem preenchida nas listagens: o painel não precisa de uma requisição por equipamento. */
    val ultimaLeitura: LeituraDto? = null,
)

@Serializable
data class EquipamentoRequest(
    val instituicaoId: Long,
    val nome: String,
    val tipo: TipoEquipamento,
    val tempMin: Double,
    val tempMax: Double,
    val umidMax: Double,
    val ativo: Boolean = true,
)

@Serializable
data class LeituraDto(
    val id: Long,
    val equipamentoId: Long,
    val temperatura: Double,
    val umidade: Double,
    val portaAberta: Boolean = false,
    val registradaEm: Instante,
    val origem: String = "SENSOR",
)

enum class TipoAlerta(val rotulo: String) {
    TEMP_ALTA("Temperatura alta"),
    TEMP_BAIXA("Temperatura baixa"),
    PORTA_ABERTA("Porta aberta"),
    UMIDADE("Umidade alta"),
}

enum class Severidade(val rotulo: String) {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta"),
    CRITICA("Crítica"),
}

enum class StatusAlerta { ABERTO, RESOLVIDO }

/** O que o voluntário fez ao resolver o alerta; vai no corpo do PATCH e volta no histórico. */
enum class AcaoTomada(val rotulo: String, val descricao: String) {
    AJUSTOU("Ajustei o equipamento", "Fechei a porta, regulei o termostato ou religuei"),
    TRANSFERIU_LOTE("Transferi o lote", "Levei os alimentos para outro equipamento"),
    DESCARTOU("Descartei o lote", "Os alimentos passaram do ponto seguro"),
}

@Serializable
data class AlertaDto(
    val id: Long,
    val equipamentoId: Long,
    val equipamento: String = "",
    val leituraId: Long = 0,
    val tipo: TipoAlerta,
    val severidade: Severidade,
    val status: StatusAlerta,
    val criadoEm: Instante,
    val resolvidoEm: Instante? = null,
    val resolvidoPorId: Long? = null,
    val acaoTomada: AcaoTomada? = null,
)

@Serializable
data class ResolverAlertaRequest(val acao: AcaoTomada)

enum class StatusLote(val rotulo: String) {
    DISPONIVEL("Disponível"),
    DISTRIBUIDO("Distribuído"),
    PERDIDO("Perdido"),
}

@Serializable
data class LoteDto(
    val id: Long,
    val instituicaoId: Long,
    val descricao: String,
    val categoria: String,
    val kg: Double,
    val validade: Data,
    val status: StatusLote,
    val retiradoEm: Instante? = null,
)

@Serializable
data class LoteRequest(val descricao: String, val categoria: String, val kg: Double, val validade: Data)

@Serializable
data class RetiradaDeLoteRequest(val status: StatusLote)
