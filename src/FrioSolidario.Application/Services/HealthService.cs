using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;

namespace FrioSolidario.Application.Services;

public sealed class HealthService : IHealthService
{
    private readonly IHealthRepository _repository;

    public HealthService(IHealthRepository repository) => _repository = repository;

    public async Task<HealthResponse> VerificarAsync(CancellationToken cancellationToken = default)
    {
        var totalEquipamentos = await _repository.ContarEquipamentosAsync(cancellationToken);

        return new HealthResponse(
            Servico: "Frio Solidario API",
            Status: "ok",
            Versao: "1.0.0",
            Ambiente: Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT") ?? "Development",
            VerificadoEm: DateTimeOffset.Now,
            OrigemDosDados: totalEquipamentos is null
                ? "banco indisponivel (schema nao carregado ou Postgres fora do ar)"
                : $"{totalEquipamentos} equipamentos no banco");
    }
}
