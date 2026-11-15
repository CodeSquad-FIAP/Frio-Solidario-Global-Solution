package br.com.fiap.friosolidario.dominio

import br.com.fiap.friosolidario.data.EquipamentoDto
import br.com.fiap.friosolidario.data.LeituraDto

enum class Semaforo(val rotulo: String) {
    VERDE("Dentro da faixa"),
    AMARELO("Atenção"),
    VERMELHO("Fora da faixa"),
    SEM_LEITURA("Sem leitura"),
}

/**
 * Cor do equipamento pela última leitura contra a faixa **dele** (`tempMin`..`tempMax`): 5 °C é
 * bom na geladeira e grave no freezer, então não existe valor fixo aqui.
 *
 * - vermelho: temperatura fora da faixa (é o que a regra da API transforma em alerta);
 * - amarelo: ainda dentro, mas encostando em um limite, com umidade acima do máximo ou com a
 *   porta aberta — o momento de agir antes de virar alerta;
 * - verde: o resto.
 */
fun semaforo(equipamento: EquipamentoDto, ultima: LeituraDto?): Semaforo {
    if (ultima == null) return Semaforo.SEM_LEITURA
    if (foraDaFaixa(ultima, equipamento)) return Semaforo.VERMELHO

    val margem = margemDeAtencao(equipamento)
    val encostando = ultima.temperatura > equipamento.tempMax - margem ||
        ultima.temperatura < equipamento.tempMin + margem

    return if (encostando || ultima.umidade > equipamento.umidMax || ultima.portaAberta) {
        Semaforo.AMARELO
    } else {
        Semaforo.VERDE
    }
}

fun foraDaFaixa(leitura: LeituraDto, equipamento: EquipamentoDto): Boolean =
    leitura.temperatura > equipamento.tempMax || leitura.temperatura < equipamento.tempMin

/** 10% da largura da faixa, com piso de 0,5 °C e teto de um quarto da faixa (faixas estreitas). */
private fun margemDeAtencao(equipamento: EquipamentoDto): Double {
    val largura = equipamento.tempMax - equipamento.tempMin
    return (largura * 0.10).coerceAtLeast(0.5).coerceAtMost(largura * 0.25)
}
