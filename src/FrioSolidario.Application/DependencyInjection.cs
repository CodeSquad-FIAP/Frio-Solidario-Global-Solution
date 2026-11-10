using FrioSolidario.Application.Repositories;
using FrioSolidario.Application.Services;
using Microsoft.Extensions.DependencyInjection;

namespace FrioSolidario.Application;

/// <summary>Registro dos servicos (regra de negocio) no container de DI.</summary>
public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<IHealthService, HealthService>();
        services.AddScoped<IInstituicaoService, InstituicaoService>();
        services.AddScoped<IEquipamentoService, EquipamentoService>();
        return services;
    }
}
