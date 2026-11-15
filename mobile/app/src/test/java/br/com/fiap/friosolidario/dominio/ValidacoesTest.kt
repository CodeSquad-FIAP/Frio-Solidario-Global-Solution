package br.com.fiap.friosolidario.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidacoesTest {

    @Test
    fun `numero aceita virgula ou ponto, sinal e espacos`() {
        assertEquals(-18.5, paraDecimal("-18,5")!!, 0.0)
        assertEquals(-18.5, paraDecimal("-18.5")!!, 0.0)
        assertEquals(4.0, paraDecimal(" 4 ")!!, 0.0)
        assertEquals(120.5, paraDecimal("120,50")!!, 0.0)
        assertEquals(2.0, paraDecimal("+2")!!, 0.0)
    }

    @Test
    fun `texto que nao e numero vira nulo`() {
        assertNull(paraDecimal(""))
        assertNull(paraDecimal("abc"))
        assertNull(paraDecimal("1,2,3"))
        assertNull(paraDecimal("-"))
        assertNull(paraDecimal("NaN"))
        assertNull(paraDecimal("Infinity"))
        assertNull(paraDecimal("1e3"))
    }

    @Test
    fun `equipamento valido nao tem erros`() {
        assertTrue(validarEquipamento("Freezer de proteínas", "-25", "-18", "90").isEmpty())
        assertTrue(validarEquipamento("Câmara", "0", "4,5", "85,5").isEmpty())
    }

    @Test
    fun `minima precisa ser menor que a maxima, como na api`() {
        val erros = validarEquipamento("Geladeira", "5", "1", "80")
        assertEquals(mapOf(Campo.TEMP_MAX to "A máxima precisa ser maior que a mínima."), erros)

        assertEquals(
            "iguais também não valem",
            setOf(Campo.TEMP_MAX),
            validarEquipamento("Geladeira", "4", "4", "80").keys,
        )
    }

    @Test
    fun `nome curto, numero invalido e umidade fora do limite apontam o campo certo`() {
        val erros = validarEquipamento("Ge", "abc", "", "0")

        assertEquals("O nome precisa ter de 3 a 150 caracteres.", erros[Campo.NOME])
        assertEquals("Informe um número, como -18 ou 2,5.", erros[Campo.TEMP_MIN])
        assertEquals("Informe um número, como -18 ou 2,5.", erros[Campo.TEMP_MAX])
        assertEquals("A umidade máxima fica entre 0,01 e 100%.", erros[Campo.UMID_MAX])
    }

    @Test
    fun `temperatura fora do limite fisico da api`() {
        assertEquals(
            "A temperatura fica entre -40 e 80 °C.",
            validarEquipamento("Freezer", "-41", "0", "80")[Campo.TEMP_MIN],
        )
        assertEquals(
            "A temperatura fica entre -40 e 80 °C.",
            validarEquipamento("Estufa", "0", "80,5", "80")[Campo.TEMP_MAX],
        )
        assertEquals("A umidade máxima fica entre 0,01 e 100%.", validarEquipamento("Câmara", "0", "4", "100,1")[Campo.UMID_MAX])
    }

    @Test
    fun `nome com marcacao e recusado antes de a api recusar`() {
        assertEquals(
            "O nome não pode ter os sinais < ou >.",
            validarEquipamento("<b>Freezer</b>", "-25", "-18", "90")[Campo.NOME],
        )
    }

    @Test
    fun `lote valido nao tem erros`() {
        assertTrue(validarLote("Caixas de tomate", "Hortifruti", "120,5", LocalDate.of(2026, 11, 20)).isEmpty())
    }

    @Test
    fun `lote aponta cada campo invalido`() {
        val erros = validarLote("ab", "x", "0", null)

        assertEquals("A descrição precisa ter de 3 a 200 caracteres.", erros[Campo.DESCRICAO])
        assertEquals("A categoria precisa ter de 3 a 80 caracteres.", erros[Campo.CATEGORIA])
        assertEquals("Informe o peso em kg, maior que zero.", erros[Campo.KG])
        assertEquals("Informe a data de validade.", erros[Campo.VALIDADE])
        assertEquals("Informe o peso em kg, maior que zero.", validarLote("abc", "abc", "muito", LocalDate.now())[Campo.KG])
    }

    @Test
    fun `umidade maxima aceita a mesma faixa da api, de 0,01 a 100`() {
        assertNull(validarEquipamento("Câmara", "0", "4", "0,01")[Campo.UMID_MAX])
        assertNull(validarEquipamento("Câmara", "0", "4", "0,5")[Campo.UMID_MAX])
        assertNull(validarEquipamento("Câmara", "0", "4", "100")[Campo.UMID_MAX])
        assertEquals("A umidade máxima fica entre 0,01 e 100%.", validarEquipamento("Câmara", "0", "4", "0,009")[Campo.UMID_MAX])
    }
}
