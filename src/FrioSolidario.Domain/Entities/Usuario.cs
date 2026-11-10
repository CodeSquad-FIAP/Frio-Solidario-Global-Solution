namespace FrioSolidario.Domain.Entities;

/// <summary>Voluntario ou administrador da instituicao; o login usa o <see cref="SenhaHash"/>.</summary>
public sealed class Usuario
{
    public long Id { get; set; }

    public string Nome { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    /// <summary>Hash BCrypt (60 caracteres). A senha em texto puro nunca chega ao banco.</summary>
    public string SenhaHash { get; set; } = string.Empty;

    public PapelUsuario Papel { get; set; }

    public long InstituicaoId { get; set; }
}
