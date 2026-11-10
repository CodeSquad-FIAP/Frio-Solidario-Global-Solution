namespace FrioSolidario.Application.Common;

/// <summary>
/// Regra de negocio violada (faixa invalida, CNPJ repetido, equipamento de instituicao
/// inexistente). A camada de Api traduz isso em HTTP 400/409; por isso a Application nao
/// conhece codigo HTTP.
/// </summary>
public sealed class RegraDeNegocioException(string mensagem) : Exception(mensagem);
