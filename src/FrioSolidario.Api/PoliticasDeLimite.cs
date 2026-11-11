namespace FrioSolidario.Api;

/// <summary>
/// Nomes das politicas do limitador de requisicoes. Constante num lugar so para o Program.cs
/// (que registra) e o controller (que aplica) nao divergirem por um erro de digitacao.
/// </summary>
internal static class PoliticasDeLimite
{
    /// <summary>Tentativas de POST /api/auth/login por IP.</summary>
    public const string Login = "login";
}
