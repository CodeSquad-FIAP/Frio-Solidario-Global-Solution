using FrioSolidario.Application.Security;

namespace FrioSolidario.Infrastructure.Security;

/// <summary>
/// BCrypt para gerar e conferir a senha (work factor 10, o mesmo dos hashes do seed). A senha em
/// texto puro nao e gravada nem comparada por igualdade: o BCrypt refaz o hash com o sal que esta
/// no proprio hash guardado.
/// </summary>
public sealed class BcryptPasswordHasher : IPasswordHasher
{
    /// <summary>Igual ao do seed: hashes novos e antigos custam o mesmo para conferir no login.</summary>
    private const int WorkFactor = 10;

    public string Gerar(string senha)
    {
        // Senha vazia gerando hash valido seria uma conta com senha "adivinhavel": falha cedo.
        ArgumentException.ThrowIfNullOrWhiteSpace(senha);
        return BCrypt.Net.BCrypt.HashPassword(senha, WorkFactor);
    }

    public bool Verificar(string senha, string hash)
    {
        if (string.IsNullOrWhiteSpace(senha) || string.IsNullOrWhiteSpace(hash))
        {
            return false;
        }

        try
        {
            return BCrypt.Net.BCrypt.Verify(senha, hash);
        }
        catch (BCrypt.Net.SaltParseException)
        {
            // Hash malformado no banco nao pode virar 500: e login recusado, como senha errada.
            return false;
        }
    }
}
