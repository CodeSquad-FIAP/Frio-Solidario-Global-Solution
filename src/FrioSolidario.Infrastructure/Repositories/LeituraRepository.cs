using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

/// <summary>
/// Leituras no Postgres. A serie do grafico sobe em ordem cronologica (o app desenha da esquerda
/// para a direita) e a janela da regra desce da mais nova, porque a regra anda para tras a partir
/// da leitura recem-gravada.
/// </summary>
public sealed class LeituraRepository(FrioSolidarioDbContext contexto) : Application.Repositories.ILeituraRepository
{
    public async Task<Leitura> AdicionarAsync(Leitura leitura, CancellationToken cancellationToken = default)
    {
        contexto.Leituras.Add(leitura);
        await contexto.SaveChangesAsync(cancellationToken);
        return leitura;
    }

    public async Task<IReadOnlyList<Leitura>> ListarAsync(
        long equipamentoId,
        DateTimeOffset desde,
        DateTimeOffset ate,
        CancellationToken cancellationToken = default)
        => await contexto.Leituras
            .AsNoTracking()
            .Where(leitura =>
                leitura.EquipamentoId == equipamentoId &&
                leitura.RegistradaEm >= desde &&
                leitura.RegistradaEm <= ate)
            .OrderBy(leitura => leitura.RegistradaEm)
            .ToListAsync(cancellationToken);

    public async Task<IReadOnlyList<Leitura>> UltimasAsync(
        long equipamentoId,
        int quantidade,
        CancellationToken cancellationToken = default)
        => await contexto.Leituras
            .AsNoTracking()
            .Where(leitura => leitura.EquipamentoId == equipamentoId)
            .OrderByDescending(leitura => leitura.RegistradaEm)
            .ThenByDescending(leitura => leitura.Id)
            .Take(quantidade)
            .ToListAsync(cancellationToken);
}
