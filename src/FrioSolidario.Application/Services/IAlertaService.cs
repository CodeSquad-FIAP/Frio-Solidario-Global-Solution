using FrioSolidario.Application.DTOs;
using FrioSolidario.Domain;

namespace FrioSolidario.Application.Services;

/// <summary>
/// A fila de alertas do app e a resolucao feita pelo voluntario no local. As duas operacoes recebem
/// a instituicao do token: o voluntario so ve e resolve alerta de equipamento da propria instituicao.
/// </summary>
public interface IAlertaService
{
    Task<IReadOnlyList<AlertaResponse>> ListarAsync(
        StatusAlerta? status = null,
        long? equipamentoId = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task<AlertaResponse> ResolverAsync(
        long id,
        long resolvidoPorId,
        long? instituicaoDoToken = null,
        AcaoTomada? acao = null,
        CancellationToken cancellationToken = default);
}
