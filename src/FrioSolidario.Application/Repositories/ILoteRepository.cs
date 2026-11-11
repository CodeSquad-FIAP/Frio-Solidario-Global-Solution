using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para os lotes doados. Implementacao com EF Core na Infrastructure.</summary>
public interface ILoteRepository
{
    Task<IReadOnlyList<Lote>> ListarAsync(
        long? instituicaoId = null,
        StatusLote? status = null,
        CancellationToken cancellationToken = default);

    Task<Lote?> ObterAsync(long id, CancellationToken cancellationToken = default);

    Task<Lote> AdicionarAsync(Lote lote, CancellationToken cancellationToken = default);

    Task AtualizarAsync(Lote lote, CancellationToken cancellationToken = default);
}
