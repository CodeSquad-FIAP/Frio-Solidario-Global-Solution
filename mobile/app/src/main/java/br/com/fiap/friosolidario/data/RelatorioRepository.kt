package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.chamar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A API recusa mais que isso por pedido (um trimestre), contando os dois dias das pontas. */
const val DIAS_MAXIMOS_DO_RELATORIO = 92L

/** Confere o período antes de chamar a rede; devolve a frase do erro ou `null`. */
fun validarPeriodo(de: LocalDate, ate: LocalDate): String? = when {
    de.isAfter(ate) -> "A data inicial precisa ser anterior à final."
    ChronoUnit.DAYS.between(de, ate) + 1 > DIAS_MAXIMOS_DO_RELATORIO -> "O relatório cobre no máximo 92 dias por vez."
    else -> null
}

/**
 * Baixa o log térmico em PDF para a pasta de cache [pasta], de onde o FileProvider o entrega à
 * folha de compartilhamento e à impressão. O download vai para um arquivo temporário e só é
 * renomeado no fim: conexão que cai no meio não deixa PDF cortado para alguém imprimir.
 */
class RelatorioRepository(
    private val api: FrioApi,
    private val aoExpirar: suspend () -> Unit,
    private val pasta: () -> File,
) {

    suspend fun baixar(equipamentoId: Long, de: LocalDate, ate: LocalDate): Resultado<File> {
        validarPeriodo(de, ate)?.let { return Resultado.Falha(ErroApp.Validacao(it)) }

        return chamar(aoExpirar) {
            withContext(Dispatchers.IO) {
                val resposta = api.relatorio(equipamentoId, de.toString(), ate.toString())
                if (!resposta.isSuccessful) throw HttpException(resposta)
                val corpo = resposta.body() ?: throw HttpException(resposta)

                val destino = File(
                    pasta().apply { mkdirs() },
                    nomeSeguro(resposta.headers()["Content-Disposition"]) ?: "relatorio-$equipamentoId-$de-a-$ate.pdf",
                )
                val parcial = File(destino.parentFile, "${destino.name}.parcial")
                try {
                    corpo.byteStream().use { entrada -> parcial.outputStream().use { saida -> entrada.copyTo(saida) } }
                    destino.delete()
                    if (!parcial.renameTo(destino)) throw java.io.IOException("Não foi possível gravar o relatório.")
                } finally {
                    parcial.delete()
                }
                destino
            }
        }
    }

    private companion object {
        val NOME_NO_CABECALHO = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)

        /** Só o nome final, sem caminho e sem caractere estranho: o cabeçalho vem de fora do app. */
        fun nomeSeguro(disposicao: String?): String? {
            val bruto = disposicao?.let { NOME_NO_CABECALHO.find(it)?.groupValues?.get(1) } ?: return null
            val nome = bruto.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .trimStart('.')
            return nome.takeIf { it.length > ".pdf".length && it.endsWith(".pdf", ignoreCase = true) }
        }
    }
}
