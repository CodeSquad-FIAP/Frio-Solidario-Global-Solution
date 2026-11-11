using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

/// <summary>Alertas no Postgres, sempre com o nome do equipamento (a tela lista pelo nome, nao pelo id).</summary>
public sealed class AlertaRepository(FrioSolidarioDbContext contexto) : Application.Repositories.IAlertaRepository
{
    public async Task<Alerta> AdicionarAsync(Alerta alerta, CancellationToken cancellationToken = default)
    {
        contexto.Alertas.Add(alerta);
        await contexto.SaveChangesAsync(cancellationToken);
        return alerta;
    }

    public Task<Alerta?> ObterAsync(long id, CancellationToken cancellationToken = default)
        => contexto.Alertas.FirstOrDefaultAsync(alerta => alerta.Id == id, cancellationToken);

    public async Task AtualizarAsync(Alerta alerta, CancellationToken cancellationToken = default)
    {
        contexto.Alertas.Update(alerta);
        await contexto.SaveChangesAsync(cancellationToken);
    }

    public Task<bool> ExisteAbertoAsync(
        long equipamentoId,
        TipoAlerta tipo,
        CancellationToken cancellationToken = default)
        => contexto.Alertas
            .AsNoTracking()
            .AnyAsync(
                alerta =>
                    alerta.EquipamentoId == equipamentoId &&
                    alerta.Tipo == tipo &&
                    alerta.Status == StatusAlerta.ABERTO,
                cancellationToken);

    public async Task<IReadOnlyList<(Alerta Alerta, string Equipamento)>> ListarAsync(
        StatusAlerta? status = null,
        long? equipamentoId = null,
        long? instituicaoId = null,
        CancellationToken cancellationToken = default)
    {
        var consulta =
            from alerta in contexto.Alertas.AsNoTracking()
            join equipamento in contexto.Equipamentos.AsNoTracking()
                on alerta.EquipamentoId equals equipamento.Id
            where (status == null || alerta.Status == status)
                && (equipamentoId == null || alerta.EquipamentoId == equipamentoId)
                && (instituicaoId == null || equipamento.InstituicaoId == instituicaoId)
            select new { Alerta = alerta, Equipamento = equipamento.Nome };

        var linhas = await consulta.ToListAsync(cancellationToken);
        return linhas.Select(linha => (linha.Alerta, linha.Equipamento)).ToList();
    }

    public async Task<IReadOnlyList<Alerta>> ListarPorPeriodoAsync(
        long equipamentoId,
        DateTimeOffset desde,
        DateTimeOffset ate,
        CancellationToken cancellationToken = default)
        => await contexto.Alertas
            .AsNoTracking()
            .Where(alerta =>
                alerta.EquipamentoId == equipamentoId &&
                alerta.CriadoEm >= desde &&
                alerta.CriadoEm <= ate)
            .OrderBy(alerta => alerta.CriadoEm)
            .ToListAsync(cancellationToken);
}
