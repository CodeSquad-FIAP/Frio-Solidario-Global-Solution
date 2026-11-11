using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Tests;

/// <summary>
/// Repositorios de mentira para testar o <c>LeituraService</c> sem banco. Imitam o que os
/// repositorios reais (EF Core) prometem nas interfaces: ids gerados na gravacao e
/// <c>UltimasAsync</c> devolvendo da leitura mais nova para a mais antiga.
/// </summary>
internal sealed class LeiturasEmMemoria : ILeituraRepository
{
    private long _proximoId = 1;

    public List<Leitura> Itens { get; } = [];

    public Task<Leitura> AdicionarAsync(Leitura leitura, CancellationToken cancellationToken = default)
    {
        leitura.Id = _proximoId++;
        Itens.Add(leitura);
        return Task.FromResult(leitura);
    }

    public Task<IReadOnlyList<Leitura>> ListarAsync(
        long equipamentoId, DateTimeOffset desde, DateTimeOffset ate, CancellationToken cancellationToken = default)
        => Task.FromResult<IReadOnlyList<Leitura>>(Itens
            .Where(l => l.EquipamentoId == equipamentoId && l.RegistradaEm >= desde && l.RegistradaEm <= ate)
            .OrderBy(l => l.RegistradaEm)
            .ToList());

    public Task<IReadOnlyList<Leitura>> UltimasAsync(
        long equipamentoId, int quantidade, CancellationToken cancellationToken = default)
        => Task.FromResult<IReadOnlyList<Leitura>>(Itens
            .Where(l => l.EquipamentoId == equipamentoId)
            .OrderByDescending(l => l.RegistradaEm)
            .ThenByDescending(l => l.Id)
            .Take(quantidade)
            .ToList());
}

internal sealed class AlertasEmMemoria : IAlertaRepository
{
    private long _proximoId = 1;

    public List<Alerta> Itens { get; } = [];

    public Task<Alerta> AdicionarAsync(Alerta alerta, CancellationToken cancellationToken = default)
    {
        alerta.Id = _proximoId++;
        Itens.Add(alerta);
        return Task.FromResult(alerta);
    }

    public Task<Alerta?> ObterAsync(long id, CancellationToken cancellationToken = default)
        => Task.FromResult(Itens.FirstOrDefault(a => a.Id == id));

    public Task AtualizarAsync(Alerta alerta, CancellationToken cancellationToken = default) => Task.CompletedTask;

    public Task<bool> ExisteAbertoAsync(long equipamentoId, TipoAlerta tipo, CancellationToken cancellationToken = default)
        => Task.FromResult(Itens.Any(a => a.EquipamentoId == equipamentoId && a.Tipo == tipo && a.Status == StatusAlerta.ABERTO));

    public Task<IReadOnlyList<(Alerta Alerta, string Equipamento)>> ListarAsync(
        StatusAlerta? status = null, long? equipamentoId = null, long? instituicaoId = null, CancellationToken cancellationToken = default)
        => Task.FromResult<IReadOnlyList<(Alerta, string)>>(Itens
            .Where(a => status is null || a.Status == status)
            .Where(a => equipamentoId is null || a.EquipamentoId == equipamentoId)
            .Select(a => (a, "equipamento"))
            .ToList());

    public Task<IReadOnlyList<Alerta>> ListarPorPeriodoAsync(
        long equipamentoId, DateTimeOffset desde, DateTimeOffset ate, CancellationToken cancellationToken = default)
        => Task.FromResult<IReadOnlyList<Alerta>>(Itens
            .Where(a => a.EquipamentoId == equipamentoId && a.CriadoEm >= desde && a.CriadoEm <= ate)
            .ToList());
}

internal sealed class EquipamentosEmMemoria(params Equipamento[] equipamentos) : IEquipamentoRepository
{
    private readonly List<Equipamento> _itens = [.. equipamentos];

    public Task<IReadOnlyList<(Equipamento Equipamento, string Instituicao)>> ListarAsync(
        long? instituicaoId = null, CancellationToken cancellationToken = default)
        => Task.FromResult<IReadOnlyList<(Equipamento, string)>>(_itens
            .Where(e => instituicaoId is null || e.InstituicaoId == instituicaoId)
            .Select(e => (e, "instituicao"))
            .ToList());

    public Task<(Equipamento Equipamento, string Instituicao)?> ObterAsync(long id, CancellationToken cancellationToken = default)
        => Task.FromResult(_itens.FirstOrDefault(e => e.Id == id) is { } e ? ((Equipamento, string)?)(e, "instituicao") : null);

    public Task<bool> ExisteAsync(long id, CancellationToken cancellationToken = default)
        => Task.FromResult(_itens.Any(e => e.Id == id));

    public Task<Equipamento> AdicionarAsync(Equipamento equipamento, CancellationToken cancellationToken = default)
    {
        _itens.Add(equipamento);
        return Task.FromResult(equipamento);
    }

    public Task AtualizarAsync(Equipamento equipamento, CancellationToken cancellationToken = default) => Task.CompletedTask;

    public Task RemoverAsync(Equipamento equipamento, CancellationToken cancellationToken = default)
    {
        _itens.Remove(equipamento);
        return Task.CompletedTask;
    }
}
