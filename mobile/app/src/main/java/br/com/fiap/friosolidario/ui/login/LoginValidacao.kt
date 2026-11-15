package br.com.fiap.friosolidario.ui.login

/** Conferência antes de chamar a API; devolve a frase do erro ou `null` se pode enviar. */
fun validarLogin(email: String, senha: String): String? = when {
    email.isBlank() || senha.isBlank() -> "Informe o e-mail e a senha."
    !email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) -> "E-mail inválido."
    senha.length < 6 -> "A senha tem pelo menos 6 caracteres."
    else -> null
}
