namespace FrioSolidario.Application.Common;

/// <summary>
/// Login recusado. A mensagem e sempre a mesma para e-mail inexistente e senha errada: dizer
/// qual dos dois falhou entrega para quem tenta adivinhar quais e-mails existem na base.
/// A Api traduz em HTTP 401.
/// </summary>
public sealed class CredenciaisInvalidasException(string mensagem) : Exception(mensagem);
