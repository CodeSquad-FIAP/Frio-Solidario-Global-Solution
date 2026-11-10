using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

/// <summary>
/// Usuarios do login. O e-mail e comparado em minusculas porque o cadastro normaliza assim:
/// sem isso "Ana@..." e "ana@..." seriam contas diferentes na busca.
/// </summary>
public sealed class UsuarioRepository(FrioSolidarioDbContext contexto) : Application.Repositories.IUsuarioRepository
{
    public Task<Usuario?> ObterPorEmailAsync(string email, CancellationToken cancellationToken = default)
    {
        var normalizado = (email ?? string.Empty).Trim().ToLowerInvariant();
        return contexto.Usuarios
            .AsNoTracking()
            .FirstOrDefaultAsync(
                usuario => usuario.Email.ToLower() == normalizado,
                cancellationToken);
    }

    public Task<Usuario?> ObterAsync(long id, CancellationToken cancellationToken = default)
        => contexto.Usuarios
            .AsNoTracking()
            .FirstOrDefaultAsync(usuario => usuario.Id == id, cancellationToken);
}
