using FrioSolidario.Application.Repositories;
using FrioSolidario.Infrastructure.Repositories;
using Microsoft.Extensions.DependencyInjection;

namespace FrioSolidario.Infrastructure;

/// <summary>Registro dos repositorios no container de DI.</summary>
public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(this IServiceCollection services)
    {
        // Provisorio: em memoria, sem banco. Trocar aqui quando o schema estiver fechado.
        services.AddScoped<IHealthRepository, HealthRepository>();
        return services;
    }
}
