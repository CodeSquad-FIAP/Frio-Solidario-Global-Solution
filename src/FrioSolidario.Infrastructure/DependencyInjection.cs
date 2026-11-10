using FrioSolidario.Application.Repositories;
using FrioSolidario.Infrastructure.Persistence;
using FrioSolidario.Infrastructure.Repositories;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace FrioSolidario.Infrastructure;

/// <summary>Registro do acesso a dados (Postgres do Andre) no container de DI.</summary>
public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(this IServiceCollection services, IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("FrioSolidario")
            ?? throw new InvalidOperationException(
                "Configure ConnectionStrings:FrioSolidario no appsettings.json " +
                "ou a variavel de ambiente ConnectionStrings__FrioSolidario.");

        services.AddDbContext<FrioSolidarioDbContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IHealthRepository, HealthRepository>();
        services.AddScoped<IInstituicaoRepository, InstituicaoRepository>();
        services.AddScoped<IEquipamentoRepository, EquipamentoRepository>();
        return services;
    }
}
