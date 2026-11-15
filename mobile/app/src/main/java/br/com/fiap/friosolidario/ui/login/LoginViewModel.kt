package br.com.fiap.friosolidario.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginEstado(
    val email: String = "",
    val senha: String = "",
    val enviando: Boolean = false,
    /** Campo vazio ou mal preenchido: conferido no app, sem chamar a API. */
    val erroDeCampo: String? = null,
    /** Resposta da API ou falta de rede. */
    val erro: ErroApp? = null,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginEstado())
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    fun mudarEmail(valor: String) = _estado.update { it.copy(email = valor, erroDeCampo = null, erro = null) }

    fun mudarSenha(valor: String) = _estado.update { it.copy(senha = valor, erroDeCampo = null, erro = null) }

    /** Sucesso não precisa de retorno: a sessão salva faz a navegação trocar de tela. */
    fun entrar() {
        val atual = _estado.value
        if (atual.enviando) return

        val erroDeCampo = validarLogin(atual.email, atual.senha)
        if (erroDeCampo != null) {
            _estado.update { it.copy(erroDeCampo = erroDeCampo, erro = null) }
            return
        }

        _estado.update { it.copy(enviando = true, erroDeCampo = null, erro = null) }
        viewModelScope.launch {
            val resultado = auth.entrar(atual.email, atual.senha)
            _estado.update {
                when (resultado) {
                    // Este ViewModel sobrevive ao logout: a senha não pode ficar no campo para
                    // quem pegar o aparelho depois. O e-mail fica, para o próximo login.
                    is Resultado.Ok -> it.copy(enviando = false, erro = null, senha = "")
                    is Resultado.Falha -> it.copy(enviando = false, erro = resultado.erro)
                }
            }
        }
    }
}
