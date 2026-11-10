using System.Data.Common;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

/// <summary>
/// Conta os equipamentos direto na tabela <c>equipamento</c> do schema do Andre.
/// Se o Postgres estiver fora do ar (ou o schema ainda nao tiver sido carregado com
/// <c>db/schema.sql</c>), devolve <c>null</c> em vez de derrubar o health check.
/// </summary>
public sealed class HealthRepository(FrioSolidarioDbContext contexto) : IHealthRepository
{
    public async Task<int?> ContarEquipamentosAsync(CancellationToken cancellationToken = default)
    {
        try
        {
            return await contexto.Equipamentos.AsNoTracking().CountAsync(cancellationToken);
        }
        // Banco fora do ar: o Npgsql lanca DbException direto, e o EF ainda envolve a falha de
        // conexao em InvalidOperationException dentro da estrategia de execucao. Nos dois casos
        // o health check responde "banco indisponivel" em vez de estourar 500.
        catch (Exception ex) when (ex is DbException or InvalidOperationException { InnerException: DbException })
        {
            return null;
        }
    }
}
