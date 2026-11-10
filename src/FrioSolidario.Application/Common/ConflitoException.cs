namespace FrioSolidario.Application.Common;

/// <summary>
/// Conflito com o estado atual do banco (CNPJ repetido, exclusao bloqueada por vinculo).
/// A Api traduz em HTTP 409.
/// </summary>
public sealed class ConflitoException(string mensagem) : Exception(mensagem);
