package br.com.fiap.friosolidario.ui.detalhe

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import br.com.fiap.friosolidario.core.PT_BR
import br.com.fiap.friosolidario.core.temperatura
import br.com.fiap.friosolidario.data.EquipamentoDto
import br.com.fiap.friosolidario.data.LeituraDto
import br.com.fiap.friosolidario.dominio.escalaPara
import br.com.fiap.friosolidario.dominio.foraDaFaixa
import br.com.fiap.friosolidario.dominio.resumoDaSerie
import br.com.fiap.friosolidario.ui.tema.LocalCoresDeEstado
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Série de temperatura com a faixa do equipamento: a área entre `tempMin` e `tempMax` fica
 * sombreada, os dois limites são linhas tracejadas com o valor, e os pontos fora da faixa
 * aparecem em vermelho. Desenhado direto no Canvas, sem biblioteca de gráfico.
 */
@Composable
fun GraficoTemperatura(
    leituras: List<LeituraDto>,
    equipamento: EquipamentoDto,
    inicio: Instant,
    fim: Instant,
    modifier: Modifier = Modifier,
) {
    val medidor = rememberTextMeasurer()
    val estados = LocalCoresDeEstado.current
    val corDaLinha = MaterialTheme.colorScheme.primary
    val corDaGrade = MaterialTheme.colorScheme.outlineVariant
    val corDoFundo = MaterialTheme.colorScheme.surface
    val corDoTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val estiloDoEixo = MaterialTheme.typography.labelSmall.copy(color = corDoTexto)
    val estiloDoLimite = MaterialTheme.typography.labelSmall.copy(color = estados.critico.forte)

    val escala = remember(leituras, equipamento, inicio, fim) {
        escalaPara(leituras, equipamento.tempMin, equipamento.tempMax, inicio, fim)
    }
    val descricao = remember(leituras, equipamento) { descrever(leituras, equipamento) }

    val formatoDoEixoX = remember(inicio, fim) {
        val padrao = if (Duration.between(inicio, fim).toHours() > 48) "dd/MM" else "HH:mm"
        DateTimeFormatter.ofPattern(padrao, PT_BR).withZone(ZoneId.systemDefault())
    }

    Canvas(modifier.semantics { contentDescription = descricao }) {
        val marcas = escala.marcasDoEixoY()
        val rotuloMaisLargo = marcas.maxOfOrNull { medidor.measure(numero(it), estiloDoEixo).size.width } ?: 0
        val esquerda = rotuloMaisLargo + 8.dp.toPx()
        val base = medidor.measure("00:00", estiloDoEixo).size.height + 8.dp.toPx()
        val topo = 8.dp.toPx()
        val direita = 8.dp.toPx()
        val largura = size.width - esquerda - direita
        val altura = size.height - topo - base
        if (largura <= 0f || altura <= 0f) return@Canvas

        fun px(instante: Instant) = esquerda + escala.x(instante, largura)
        fun py(valor: Double) = topo + escala.y(valor, altura)

        // Grade e rótulos do eixo vertical, em valores redondos.
        for (valor in marcas) {
            val y = py(valor)
            drawLine(corDaGrade, Offset(esquerda, y), Offset(esquerda + largura, y), strokeWidth = 1.dp.toPx())
            val texto = medidor.measure(numero(valor), estiloDoEixo)
            drawText(texto, topLeft = Offset(esquerda - texto.size.width - 6.dp.toPx(), y - texto.size.height / 2f))
        }

        // Rótulos do eixo do tempo: começo, meio e fim da janela.
        val meio = Instant.ofEpochMilli((inicio.toEpochMilli() + fim.toEpochMilli()) / 2)
        listOf(inicio, meio, fim).forEachIndexed { indice, instante ->
            val texto = medidor.measure(formatoDoEixoX.format(instante), estiloDoEixo)
            val x = when (indice) {
                0 -> esquerda
                1 -> px(instante) - texto.size.width / 2f
                else -> esquerda + largura - texto.size.width
            }
            drawText(texto, topLeft = Offset(x, topo + altura + 6.dp.toPx()))
        }

        // Faixa segura sombreada e os dois limites tracejados, cada um com o seu valor.
        val yMax = py(equipamento.tempMax)
        val yMin = py(equipamento.tempMin)
        drawRect(estados.ok.forte.copy(alpha = 0.12f), topLeft = Offset(esquerda, yMax), size = Size(largura, yMin - yMax))
        val tracejado = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        listOf(equipamento.tempMax to "máx", equipamento.tempMin to "mín").forEach { (limite, nome) ->
            val y = py(limite)
            drawLine(
                estados.critico.forte,
                Offset(esquerda, y),
                Offset(esquerda + largura, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = tracejado,
            )
            // Rótulo no começo do eixo (a leitura mais recente fica à direita), sobre um fundo
            // próprio para a linha da série não atravessar o texto.
            val texto = medidor.measure("$nome ${temperatura(limite)}", estiloDoLimite)
            val acima = nome == "máx"
            val canto = Offset(
                esquerda + 6.dp.toPx(),
                if (acima) y - texto.size.height - 3.dp.toPx() else y + 3.dp.toPx(),
            )
            drawRoundRect(
                corDoFundo.copy(alpha = 0.85f),
                topLeft = Offset(canto.x - 4.dp.toPx(), canto.y),
                size = Size(texto.size.width + 8.dp.toPx(), texto.size.height.toFloat()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            )
            drawText(texto, topLeft = canto)
        }

        if (leituras.isEmpty()) return@Canvas

        clipRect(esquerda, topo, esquerda + largura, topo + altura) {
            if (leituras.size > 1) {
                val caminho = Path()
                leituras.forEachIndexed { indice, leitura ->
                    val x = px(leitura.registradaEm)
                    val y = py(leitura.temperatura)
                    if (indice == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
                }
                drawPath(
                    caminho,
                    corDaLinha,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            leituras.filter { foraDaFaixa(it, equipamento) }.forEach { leitura ->
                drawCircle(estados.critico.forte, radius = 3.dp.toPx(), center = Offset(px(leitura.registradaEm), py(leitura.temperatura)))
            }
        }

        // A leitura mais recente, destacada na cor do estado dela.
        val ultima = leituras.last()
        val centro = Offset(px(ultima.registradaEm).coerceIn(esquerda, esquerda + largura), py(ultima.temperatura))
        val corDaUltima = if (foraDaFaixa(ultima, equipamento)) estados.critico.forte else corDaLinha
        drawCircle(corDaUltima.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = centro)
        drawCircle(corDaUltima, radius = 4.dp.toPx(), center = centro)
    }
}

private fun numero(valor: Double): String = String.format(PT_BR, "%.1f", valor)

/** O que o leitor de tela fala no lugar do desenho. */
private fun descrever(leituras: List<LeituraDto>, equipamento: EquipamentoDto): String {
    val resumo = resumoDaSerie(leituras, equipamento.tempMin, equipamento.tempMax)
        ?: return "Gráfico de temperatura sem leituras no período."
    return "Gráfico de temperatura com ${resumo.total} leituras: mínima ${temperatura(resumo.minima)}, " +
        "máxima ${temperatura(resumo.maxima)}, ${resumo.foraDaFaixa} fora da faixa de " +
        "${temperatura(equipamento.tempMin)} a ${temperatura(equipamento.tempMax)}."
}

