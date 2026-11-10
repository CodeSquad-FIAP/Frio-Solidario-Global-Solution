namespace FrioSolidario.Application.Common;

/// <summary>
/// O usuario autenticado nao pode agir sobre o recurso (equipamento de outra instituicao).
/// A Api traduz em HTTP 403 e nao no 404, para o app saber que o recurso existe mas nao e dele.
/// </summary>
public sealed class AcessoNegadoException(string mensagem) : Exception(mensagem);
