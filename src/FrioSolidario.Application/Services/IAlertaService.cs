using FrioSolidario.Application.DTOs;
using FrioSolidario.Domain;

namespace FrioSolidario.Application.Services;

/// <summary>A fila de alertas do app e a resolucao feita pelo voluntario no local.</summary>
public interface IAlertaService
{
    Task<IReadOnlyList<AlertaResponse>> ListarAsync(
        StatusAlerta? status = null,
        long? equipamentoId = null,
        CancellationToken cancellationToken = default);

    Task<AlertaResponse> ResolverAsync(
        long id,
        long resolvidoPorId,
        CancellationToken cancellationToken = default);
}
