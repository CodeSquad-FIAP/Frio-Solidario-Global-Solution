package br.com.fiap.friosolidario.core

/**
 * Tudo o que pode dar errado em uma chamada à API, já com a frase que vai para a tela. As cinco
 * telas tratam erro por este tipo, então nenhuma delas conhece status HTTP nem exceção de rede.
 */
sealed interface ErroApp {
    val mensagem: String

    data object SemConexao : ErroApp {
        override val mensagem = "Sem conexão com o servidor. Confira a rede e tente de novo."
    }

    data object SessaoExpirada : ErroApp {
        override val mensagem = "Sua sessão expirou. Entre de novo."
    }

    data object CredenciaisInvalidas : ErroApp {
        override val mensagem = "E-mail ou senha incorretos."
    }

    data object SemPermissao : ErroApp {
        override val mensagem = "Seu perfil não tem permissão para esta ação."
    }

    /** 400: a frase é a da própria API (`detail` ou a primeira de `errors`). */
    data class Validacao(override val mensagem: String) : ErroApp

    data object NaoEncontrado : ErroApp {
        override val mensagem = "Registro não encontrado. Ele pode ter sido removido."
    }

    /** 409: a frase é o `detail` da API ("Alerta 2 ja foi resolvido."). */
    data class Conflito(override val mensagem: String) : ErroApp

    data object MuitasTentativas : ErroApp {
        override val mensagem = "Muitas tentativas seguidas. Espere um minuto e tente de novo."
    }

    data object Servidor : ErroApp {
        override val mensagem = "O servidor encontrou um erro. Tente de novo em instantes."
    }

    data object Inesperado : ErroApp {
        override val mensagem = "Algo deu errado. Tente de novo."
    }
}

sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>
    data class Falha(val erro: ErroApp) : Resultado<Nothing>
}

inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Ok -> Resultado.Ok(transformar(valor))
    is Resultado.Falha -> this
}
