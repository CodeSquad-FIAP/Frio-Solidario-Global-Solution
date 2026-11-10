using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para as leituras dos sensores.</summary>
public interface ILeituraRepository
{
    Task<Leitura> AdicionarAsync(Leitura leitura, CancellationToken cancellationToken = default);

    /// <summary>Leituras do equipamento no periodo, da mais antiga para a mais nova (ordem do grafico).</summary>
    Task<IReadOnlyList<Leitura>> ListarAsync(
        long equipamentoId,
        DateTimeOffset desde,
        DateTimeOffset ate,
        CancellationToken cancellationToken = default);

    /// <summary>Ultimas leituras do equipamento, da mais nova para a mais antiga (a regra de alerta anda para tras).</summary>
    Task<IReadOnlyList<Leitura>> UltimasAsync(
        long equipamentoId,
        int quantidade,
        CancellationToken cancellationToken = default);
}
