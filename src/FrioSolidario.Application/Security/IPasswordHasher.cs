namespace FrioSolidario.Application.Security;

/// <summary>
/// Geracao e conferencia de senha. A Application nao depende de BCrypt: quem implementa
/// (Infrastructure) e que escolhe o algoritmo.
/// </summary>
public interface IPasswordHasher
{
    /// <summary>Hash da senha para gravar em <c>usuario.senha_hash</c>; cada chamada usa um sal novo.</summary>
    string Gerar(string senha);

    bool Verificar(string senha, string hash);
}
