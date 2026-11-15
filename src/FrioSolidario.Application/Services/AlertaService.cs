using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Fila de alertas e resolucao. A ordem da fila e a que o voluntario precisa na tela: o mais grave
/// primeiro e, entre iguais, o mais recente -- nao a ordem de insercao no banco.
/// </summary>
public sealed class AlertaService(
    IAlertaRepository alertas,
    IEquipamentoRepository equipamentos) : IAlertaService
{
    public async Task<IReadOnlyList<AlertaResponse>> ListarAsync(
        StatusAlerta? status = null,
        long? equipamentoId = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        // Filtrar por equipamento de outra instituicao e 403, como nas leituras; sem filtro, a
        // consulta ja sai restrita a instituicao do token.
        if (equipamentoId is not null && await equipamentos.ObterAsync(equipamentoId.Value, cancellationToken) is { } filtrado)
        {
            EscopoDaInstituicao.Garantir(
                filtrado.Equipamento.InstituicaoId,
                instituicaoDoToken,
                $"Equipamento {equipamentoId} pertence a outra instituicao.");
        }

        var lista = await alertas.ListarAsync(status, equipamentoId, instituicaoDoToken, cancellationToken);

        return lista
            .OrderByDescending(item => Peso(item.Alerta.Severidade))
            .ThenByDescending(item => item.Alerta.CriadoEm)
            .Select(item => AlertaResponse.De(item.Alerta, item.Equipamento))
            .ToList();
    }

    public async Task<AlertaResponse> ResolverAsync(
        long id,
        long resolvidoPorId,
        long? instituicaoDoToken = null,
        AcaoTomada? acao = null,
        CancellationToken cancellationToken = default)
    {
        var alerta = await alertas.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Alerta {id} nao encontrado.");

        var equipamento = await equipamentos.ObterAsync(alerta.EquipamentoId, cancellationToken);
        if (equipamento is not null)
        {
            EscopoDaInstituicao.Garantir(
                equipamento.Value.Equipamento.InstituicaoId,
                instituicaoDoToken,
                $"Alerta {id} e de equipamento de outra instituicao.");
        }

        if (alerta.Status == StatusAlerta.RESOLVIDO)
        {
            throw new ConflitoException($"Alerta {id} ja foi resolvido.");
        }

        // O CHECK ck_alerta_resolucao_coerente exige resolvido_em >= criado_em: uma leitura com
        // registrada_em no futuro (relogio do sensor adiantado) faria o UPDATE falhar no banco.
        var agora = DateTimeOffset.UtcNow;
        alerta.Status = StatusAlerta.RESOLVIDO;
        alerta.ResolvidoEm = agora < alerta.CriadoEm ? alerta.CriadoEm : agora;
        alerta.ResolvidoPorId = resolvidoPorId;
        alerta.AcaoTomada = acao;

        await alertas.AtualizarAsync(alerta, cancellationToken);

        return AlertaResponse.De(alerta, equipamento?.Equipamento.Nome ?? string.Empty);
    }

    /// <summary>CRITICA antes de ALTA, ALTA antes de MEDIA, MEDIA antes de BAIXA.</summary>
    private static int Peso(SeveridadeAlerta severidade) => severidade switch
    {
        SeveridadeAlerta.CRITICA => 4,
        SeveridadeAlerta.ALTA => 3,
        SeveridadeAlerta.MEDIA => 2,
        _ => 1
    };
}
