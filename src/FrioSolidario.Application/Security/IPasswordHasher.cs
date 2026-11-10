namespace FrioSolidario.Application.Security;

/// <summary>
/// Conferencia de senha contra o hash guardado. A Application nao depende de BCrypt: quem
/// implementa (Infrastructure) e que escolhe o algoritmo.
/// </summary>
public interface IPasswordHasher
{
    bool Verificar(string senha, string hash);
}
