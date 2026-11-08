using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Camada de servico: regra de negocio. O controller nunca fala com repositorio direto,
/// e o repositorio nunca devolve DTO de tela.
/// </summary>
public interface IHealthService
{
    Task<HealthResponse> VerificarAsync(CancellationToken cancellationToken = default);
}
