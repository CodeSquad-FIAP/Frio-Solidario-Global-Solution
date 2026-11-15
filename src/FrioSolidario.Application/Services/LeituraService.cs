using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Grava a leitura e, na mesma chamada, aplica a regra de faixa. Sempre devolve o que gravou e os
/// alertas que a leitura abriu, para o sensor e a demo verem o alarme sem uma segunda chamada.
/// </summary>
public sealed class LeituraService(
    ILeituraRepository leituras,
    IAlertaRepository alertas,
    IEquipamentoRepository equipamentos) : ILeituraService
{
    /// <summary>Quantas leituras anteriores a regra consulta; 50 cobre a sequencia de porta aberta com folga.</summary>
    private const int JanelaDaRegra = 50;

    /// <summary>Tolerancia para sensor com relogio levemente adiantado.</summary>
    private static readonly TimeSpan ToleranciaDeRelogio = TimeSpan.FromMinutes(5);

    public async Task<LeituraRegistradaResponse> RegistrarAsync(
        LeituraRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(request.EquipamentoId, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {request.EquipamentoId} nao encontrado.");

        GarantirInstituicaoDoToken(encontrado.Equipamento, instituicaoDoToken);
        ValidarFaixaFisica(request);

        // Sempre UTC: o Npgsql nao aceita DateTimeOffset com offset -03:00 numa coluna timestamptz,
        // e o sensor/app pode mandar o horario no fuso de Brasilia.
        var registradaEm = (request.RegistradaEm ?? DateTimeOffset.UtcNow).ToUniversalTime();
        if (registradaEm > DateTimeOffset.UtcNow + ToleranciaDeRelogio)
        {
            throw new RegraDeNegocioException(
                "registradaEm nao pode estar no futuro (tolerancia de 5 minutos para o relogio do sensor).");
        }

        var leitura = new Leitura
        {
            EquipamentoId = request.EquipamentoId,
            Temperatura = request.Temperatura,
            Umidade = request.Umidade,
            PortaAberta = request.PortaAberta,
            RegistradaEm = registradaEm,
            Origem = request.Origem
        };

        var gravada = await leituras.AdicionarAsync(leitura, cancellationToken);

        var abertos = await AvaliarRegraAsync(encontrado.Equipamento, gravada, cancellationToken);

        return new LeituraRegistradaResponse(LeituraResponse.De(gravada), abertos);
    }

    public async Task<IReadOnlyList<LeituraResponse>> ListarAsync(
        long equipamentoId,
        string? periodo = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(equipamentoId, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {equipamentoId} nao encontrado.");

        GarantirInstituicaoDoToken(encontrado.Equipamento, instituicaoDoToken);

        var janela = Janela(periodo);
        var ate = DateTimeOffset.UtcNow;
        var lista = await leituras.ListarAsync(equipamentoId, ate - janela, ate, cancellationToken);

        return lista.Select(LeituraResponse.De).ToList();
    }

    public async Task<LeituraResponse?> UltimaAsync(
        long equipamentoId,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(equipamentoId, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {equipamentoId} nao encontrado.");

        GarantirInstituicaoDoToken(encontrado.Equipamento, instituicaoDoToken);

        var ultimas = await leituras.UltimasAsync(equipamentoId, 1, cancellationToken);
        return ultimas.Count == 0 ? null : LeituraResponse.De(ultimas[0]);
    }

    /// <summary>Traduz "24h"/"7d" na janela consultada; qualquer outro valor e erro do cliente (400).</summary>
    private static TimeSpan Janela(string? periodo) => (periodo ?? "24h").Trim().ToLowerInvariant() switch
    {
        "24h" => TimeSpan.FromHours(24),
        "7d" => TimeSpan.FromDays(7),
        _ => throw new RegraDeNegocioException("periodo aceita apenas 24h ou 7d.")
    };

    private async Task<IReadOnlyList<AlertaResponse>> AvaliarRegraAsync(
        Equipamento equipamento,
        Leitura gravada,
        CancellationToken cancellationToken)
    {
        var anteriores = await leituras.UltimasAsync(equipamento.Id, JanelaDaRegra, cancellationToken);
        var criados = new List<AlertaResponse>();

        foreach (var proposto in RegraDeAlerta.Avaliar(equipamento, gravada, anteriores))
        {
            // Um alerta aberto por tipo basta: sem isso a fila do app ganharia um alerta por leitura
            // enquanto a temperatura segue fora da faixa.
            if (await alertas.ExisteAbertoAsync(equipamento.Id, proposto.Tipo, cancellationToken))
            {
                continue;
            }

            var alerta = await alertas.AdicionarAsync(
                new Alerta
                {
                    EquipamentoId = equipamento.Id,
                    LeituraId = gravada.Id,
                    Tipo = proposto.Tipo,
                    Severidade = proposto.Severidade,
                    Status = Domain.StatusAlerta.ABERTO,
                    CriadoEm = gravada.RegistradaEm
                },
                cancellationToken);

            criados.Add(AlertaResponse.De(alerta, equipamento.Nome));
        }

        return criados;
    }

    /// <summary>Um usuario so grava leitura de equipamento da propria instituicao (quando o token traz uma).</summary>
    private static void GarantirInstituicaoDoToken(Equipamento equipamento, long? instituicaoDoToken)
    {
        if (instituicaoDoToken is not null && instituicaoDoToken != equipamento.InstituicaoId)
        {
            throw new AcessoNegadoException(
                $"Equipamento {equipamento.Id} pertence a outra instituicao.");
        }
    }

    /// <summary>
    /// Mesmos limites fisicos do schema (temperatura -40..80, umidade 0..100). Validar aqui devolve
    /// 400 com a mensagem certa em vez de deixar o CHECK do banco virar 500.
    /// </summary>
    private static void ValidarFaixaFisica(LeituraRequest request)
    {
        if (request.Temperatura is < -40 or > 80)
        {
            throw new RegraDeNegocioException(
                $"Temperatura {request.Temperatura} fora da faixa fisica (-40 a 80 C).");
        }

        if (request.Umidade is < 0 or > 100)
        {
            throw new RegraDeNegocioException(
                $"Umidade {request.Umidade} fora da faixa fisica (0 a 100%).");
        }
    }
}
