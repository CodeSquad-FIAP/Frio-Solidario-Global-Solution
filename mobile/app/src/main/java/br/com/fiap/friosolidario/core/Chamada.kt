package br.com.fiap.friosolidario.core

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private const val VALIDACAO_GENERICA = "Dados inválidos. Confira os campos e tente de novo."
private const val CONFLITO_GENERICO = "Este registro já foi alterado. Atualize a tela."

/** Corpo de erro da API (`ProblemDetails`); `errors` só vem na validação dos campos. */
@Serializable
private data class ProblemDetails(
    val detail: String? = null,
    val errors: Map<String, List<String>>? = null,
)

private val leitorDeErro = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Toda chamada à API passa por aqui: devolve o valor ou um [ErroApp], nunca uma exceção. Timeout
 * e API desligada chegam como [IOException]; os demais erros vêm pelo status. [aoExpirar] é
 * avisado só no 401, para a sessão ser limpa e a navegação voltar ao login.
 */
suspend fun <T> chamar(aoExpirar: suspend () -> Unit = {}, bloco: suspend () -> T): Resultado<T> =
    try {
        Resultado.Ok(bloco())
    } catch (cancelada: CancellationException) {
        throw cancelada
    } catch (http: HttpException) {
        val erro = erroDoHttp(http.code(), runCatching { http.response()?.errorBody()?.string() }.getOrNull())
        if (erro == ErroApp.SessaoExpirada) aoExpirar()
        Resultado.Falha(erro)
    } catch (_: IOException) {
        Resultado.Falha(ErroApp.SemConexao)
    } catch (_: Exception) {
        Resultado.Falha(ErroApp.Inesperado)
    }

fun erroDoHttp(status: Int, corpo: String?): ErroApp {
    val problema = corpo?.takeIf { it.isNotBlank() }?.let {
        runCatching { leitorDeErro.decodeFromString<ProblemDetails>(it) }.getOrNull()
    }
    val primeiraDeErrors = problema?.errors?.values?.firstNotNullOfOrNull { it.firstOrNull() }
    val detalhe = problema?.detail?.takeIf { it.isNotBlank() }

    return when (status) {
        400 -> ErroApp.Validacao(primeiraDeErrors ?: detalhe ?: VALIDACAO_GENERICA)
        401 -> ErroApp.SessaoExpirada
        403 -> ErroApp.SemPermissao
        404 -> ErroApp.NaoEncontrado
        409 -> ErroApp.Conflito(detalhe ?: CONFLITO_GENERICO)
        429 -> ErroApp.MuitasTentativas
        in 500..599 -> ErroApp.Servidor
        else -> ErroApp.Inesperado
    }
}
