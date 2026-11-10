using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para os usuarios do login.</summary>
public interface IUsuarioRepository
{
    Task<Usuario?> ObterPorEmailAsync(string email, CancellationToken cancellationToken = default);

    Task<Usuario?> ObterAsync(long id, CancellationToken cancellationToken = default);
}
