package br.com.fiap.friosolidario.dominio

import java.time.LocalDate

// Conferência dos formulários antes do envio. Repete as regras da API (limites físicos, faixa
// coerente, tamanhos) para o erro aparecer no campo, sem esperar a resposta; o que escapar daqui
// ainda volta como 400 e a tela mostra a frase da API.

enum class Campo { NOME, TEMP_MIN, TEMP_MAX, UMID_MAX, DESCRICAO, CATEGORIA, KG, VALIDADE }

private val NUMERO = Regex("^[+-]?\\d+([.,]\\d+)?$")

/** Aceita vírgula ou ponto como separador decimal ("-18,5" e "-18.5"); o resto é `null`. */
fun paraDecimal(texto: String): Double? {
    val limpo = texto.trim()
    if (!NUMERO.matches(limpo)) return null
    return limpo.replace(',', '.').toDoubleOrNull()
}

private const val NUMERO_INVALIDO = "Informe um número, como -18 ou 2,5."
private const val TEMPERATURA_FORA = "A temperatura fica entre -40 e 80 °C."
private const val UMIDADE_FORA = "A umidade máxima fica entre 1 e 100%."

fun validarEquipamento(nome: String, tempMin: String, tempMax: String, umidMax: String): Map<Campo, String> {
    val erros = linkedMapOf<Campo, String>()

    val nomeLimpo = nome.trim()
    when {
        nomeLimpo.length !in 3..150 -> erros[Campo.NOME] = "O nome precisa ter de 3 a 150 caracteres."
        '<' in nomeLimpo || '>' in nomeLimpo -> erros[Campo.NOME] = "O nome não pode ter os sinais < ou >."
    }

    val minima = paraDecimal(tempMin)
    val maxima = paraDecimal(tempMax)
    erroDeTemperatura(minima)?.let { erros[Campo.TEMP_MIN] = it }
    erroDeTemperatura(maxima)?.let { erros[Campo.TEMP_MAX] = it }
    if (Campo.TEMP_MIN !in erros && Campo.TEMP_MAX !in erros && minima!! >= maxima!!) {
        erros[Campo.TEMP_MAX] = "A máxima precisa ser maior que a mínima."
    }

    val umidade = paraDecimal(umidMax)
    if (umidade == null || umidade < 1.0 || umidade > 100.0) erros[Campo.UMID_MAX] = UMIDADE_FORA

    return erros
}

private fun erroDeTemperatura(valor: Double?): String? = when {
    valor == null -> NUMERO_INVALIDO
    valor < -40.0 || valor > 80.0 -> TEMPERATURA_FORA
    else -> null
}

fun validarLote(descricao: String, categoria: String, kg: String, validade: LocalDate?): Map<Campo, String> {
    val erros = linkedMapOf<Campo, String>()
    if (descricao.trim().length !in 3..200) erros[Campo.DESCRICAO] = "A descrição precisa ter de 3 a 200 caracteres."
    if (categoria.trim().length !in 3..80) erros[Campo.CATEGORIA] = "A categoria precisa ter de 3 a 80 caracteres."
    val peso = paraDecimal(kg)
    if (peso == null || peso <= 0.0) erros[Campo.KG] = "Informe o peso em kg, maior que zero."
    if (validade == null) erros[Campo.VALIDADE] = "Informe a data de validade."
    return erros
}
