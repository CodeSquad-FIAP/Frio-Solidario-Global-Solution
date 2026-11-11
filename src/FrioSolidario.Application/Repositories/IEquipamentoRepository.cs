using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para os equipamentos. Implementacao com EF Core na Infrastructure.</summary>
public interface IEquipamentoRepository
{
    Task<IReadOnlyList<(Equipamento Equipamento, string Instituicao)>> ListarAsync(
        long? instituicaoId = null,
        CancellationToken cancellationToken = default);

    Task<(Equipamento Equipamento, string Instituicao)?> ObterAsync(
        long id,
        CancellationToken cancellationToken = default);

    Task<bool> ExisteAsync(long id, CancellationToken cancellationToken = default);

    Task<Equipamento> AdicionarAsync(Equipamento equipamento, CancellationToken cancellationToken = default);

    Task AtualizarAsync(Equipamento equipamento, CancellationToken cancellationToken = default);
}
