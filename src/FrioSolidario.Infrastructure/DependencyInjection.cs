using FrioSolidario.Application.Repositories;
using FrioSolidario.Application.Relatorios;
using FrioSolidario.Application.Security;
using FrioSolidario.Infrastructure.Persistence;
using FrioSolidario.Infrastructure.Relatorios;
using FrioSolidario.Infrastructure.Repositories;
using FrioSolidario.Infrastructure.Security;
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
        services.AddScoped<ILeituraRepository, LeituraRepository>();
        services.AddScoped<IAlertaRepository, AlertaRepository>();
        services.AddScoped<IUsuarioRepository, UsuarioRepository>();
        services.AddScoped<ILoteRepository, LoteRepository>();

        // Seguranca: BCrypt para conferir senha e JWT para o token de acesso do app.
        services.Configure<JwtOptions>(configuration.GetSection(JwtOptions.Secao));
        services.AddSingleton<IPasswordHasher, BcryptPasswordHasher>();
        services.AddScoped<ITokenService, JwtTokenService>();

        // Relatorio em PDF (QuestPDF). Sem estado, por isso singleton.
        services.AddSingleton<IRelatorioPdf, QuestPdfRelatorioPdf>();
        return services;
    }
}
