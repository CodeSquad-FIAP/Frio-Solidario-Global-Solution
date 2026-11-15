package br.com.fiap.friosolidario.dominio

import br.com.fiap.friosolidario.data.LeituraDto
import java.time.Instant

/** Converte temperatura e horário em posição no gráfico. Sem Android, para poder ser testada. */
data class EscalaDoGrafico(val minY: Double, val maxY: Double, val inicio: Instant, val fim: Instant) {

    fun x(instante: Instant, largura: Float): Float {
        val janela = fim.toEpochMilli() - inicio.toEpochMilli()
        if (janela <= 0L) return largura / 2f
        return ((instante.toEpochMilli() - inicio.toEpochMilli()).toDouble() / janela * largura).toFloat()
    }

    fun y(valor: Double, altura: Float): Float = ((maxY - valor) / (maxY - minY) * altura).toFloat()

    /** Valores redondos (de 0,5 em 0,5, de 2 em 2, de 10 em 10...) para as linhas de grade. */
    fun marcasDoEixoY(maximoDeMarcas: Int = 6): List<Double> {
        // Estritamente menos que o máximo de intervalos, com folga para o arredondamento do double.
        val passo = PASSOS.firstOrNull { (maxY - minY) / it < maximoDeMarcas - 1e-6 } ?: PASSOS.last()
        val primeira = kotlin.math.ceil(minY / passo - 1e-9).toLong()
        val ultima = kotlin.math.floor(maxY / passo + 1e-9).toLong()
        return (primeira..ultima).map { it * passo }
    }

    private companion object {
        val PASSOS = listOf(0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0)
    }
}

/**
 * O eixo vertical sempre contém os dois limites da faixa e todas as leituras, com 10% de folga:
 * as linhas de `tempMin` e `tempMax` nunca somem e o ponto fora da faixa aparece fora delas.
 */
fun escalaPara(
    leituras: List<LeituraDto>,
    tempMin: Double,
    tempMax: Double,
    inicio: Instant,
    fim: Instant,
): EscalaDoGrafico {
    val menor = minOf(tempMin, leituras.minOfOrNull { it.temperatura } ?: tempMin)
    val maior = maxOf(tempMax, leituras.maxOfOrNull { it.temperatura } ?: tempMax)
    val folga = if (maior > menor) (maior - menor) * 0.10 else 1.0
    return EscalaDoGrafico(menor - folga, maior + folga, inicio, fim)
}

data class ResumoDaSerie(
    val minima: Double,
    val maxima: Double,
    val media: Double,
    val foraDaFaixa: Int,
    val total: Int,
)

fun resumoDaSerie(leituras: List<LeituraDto>, tempMin: Double, tempMax: Double): ResumoDaSerie? {
    if (leituras.isEmpty()) return null
    return ResumoDaSerie(
        minima = leituras.minOf { it.temperatura },
        maxima = leituras.maxOf { it.temperatura },
        media = leituras.sumOf { it.temperatura } / leituras.size,
        foraDaFaixa = leituras.count { it.temperatura > tempMax || it.temperatura < tempMin },
        total = leituras.size,
    )
}
