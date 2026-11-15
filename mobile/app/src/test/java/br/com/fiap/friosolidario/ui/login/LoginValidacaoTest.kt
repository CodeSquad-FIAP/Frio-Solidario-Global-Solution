package br.com.fiap.friosolidario.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginValidacaoTest {

    @Test
    fun `campo vazio pede e-mail e senha`() {
        assertEquals("Informe o e-mail e a senha.", validarLogin("", ""))
        assertEquals("Informe o e-mail e a senha.", validarLogin("ana@x.example", "  "))
        assertEquals("Informe o e-mail e a senha.", validarLogin("   ", "Frio@2026"))
    }

    @Test
    fun `e-mail sem arroba e invalido`() {
        assertEquals("E-mail inválido.", validarLogin("ana.x.example", "Frio@2026"))
    }

    @Test
    fun `senha curta e recusada antes de chamar a api`() {
        assertEquals("A senha tem pelo menos 6 caracteres.", validarLogin("ana@x.example", "12345"))
    }

    @Test
    fun `dados validos passam`() {
        assertNull(validarLogin("ana.rocha@caminhosdobem.example", "Frio@2026"))
    }
}
