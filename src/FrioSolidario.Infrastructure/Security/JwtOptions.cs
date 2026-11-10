namespace FrioSolidario.Infrastructure.Security;

/// <summary>
/// Configuracao do token (<c>Jwt</c> no appsettings). A chave fica em appsettings para o projeto
/// rodar na apresentacao e pode ser trocada sem tocar em codigo pela variavel Jwt__Key.
/// </summary>
public sealed class JwtOptions
{
    public const string Secao = "Jwt";

    public string Issuer { get; set; } = "FrioSolidario";

    public string Audience { get; set; } = "FrioSolidarioApp";

    /// <summary>Chave simetrica (HMAC-SHA256); precisa de pelo menos 32 caracteres.</summary>
    public string Key { get; set; } = string.Empty;

    public int ExpiraMinutos { get; set; } = 480;
}
