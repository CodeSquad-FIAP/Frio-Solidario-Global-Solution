using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para os alertas (fila do app e historico do relatorio).</summary>
public interface IAlertaRepository
{
    Task<Alerta> AdicionarAsync(Alerta alerta, CancellationToken cancellationToken = default);

    Task<Alerta?> ObterAsync(long id, CancellationToken cancellationToken = default);

    Task AtualizarAsync(Alerta alerta, CancellationToken cancellationToken = default);

    /// <summary>
    /// Ja existe alerta ABERTO do mesmo tipo para o equipamento? E o que evita a fila do app
    /// encher de um alerta novo a cada leitura enquanto a temperatura segue fora da faixa.
    /// </summary>
    Task<bool> ExisteAbertoAsync(
        long equipamentoId,
        TipoAlerta tipo,
        CancellationToken cancellationToken = default);

    /// <summary>Fila de alertas; <paramref name="instituicaoId"/> restringe aos equipamentos dessa instituicao.</summary>
    Task<IReadOnlyList<(Alerta Alerta, string Equipamento)>> ListarAsync(
        StatusAlerta? status = null,
        long? equipamentoId = null,
        long? instituicaoId = null,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<Alerta>> ListarPorPeriodoAsync(
        long equipamentoId,
        DateTimeOffset desde,
        DateTimeOffset ate,
        CancellationToken cancellationToken = default);
}
