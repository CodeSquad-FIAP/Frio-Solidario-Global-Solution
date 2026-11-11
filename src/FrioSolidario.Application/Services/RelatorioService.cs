using FrioSolidario.Application.Common;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Application.Relatorios;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Monta os dados do log termico de um equipamento num periodo e entrega ao gerador de PDF.
/// Periodo sem leitura nao e erro: o PDF sai dizendo que nao houve medicao, porque quem imprime
/// precisa do documento na pasta, mesmo de um dia parado.
/// </summary>
public sealed class RelatorioService(
    IRelatorioPdf pdf,
    ILeituraRepository leituras,
    IAlertaRepository alertas,
    IEquipamentoRepository equipamentos) : IRelatorioService
{
    private const int DiasPadrao = 7;

    /// <summary>
    /// Teto do periodo. Sem ele, de=0001-01-01 faria a API carregar e desenhar a historia inteira do
    /// equipamento num PDF so: um request barato de mandar e caro de atender. 92 dias cobrem um trimestre.
    /// </summary>
    private const int DiasMaximo = 92;

    public async Task<RelatorioGerado> GerarAsync(
        long equipamentoId,
        DateOnly? de = null,
        DateOnly? ate = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(equipamentoId, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {equipamentoId} nao encontrado.");

        var equipamento = encontrado.Equipamento;
        if (instituicaoDoToken is not null && instituicaoDoToken != equipamento.InstituicaoId)
        {
            throw new AcessoNegadoException($"Equipamento {equipamentoId} pertence a outra instituicao.");
        }

        // O "hoje" e o do fuso do projeto: o relatorio e pedido e lido em Brasilia, nao em UTC.
        var hoje = DateOnly.FromDateTime(FusoDoProjeto.Local(DateTimeOffset.Now).DateTime);
        var inicio = de ?? hoje.AddDays(-DiasPadrao);
        var fim = ate ?? hoje;

        if (inicio > fim)
        {
            throw new RegraDeNegocioException($"Periodo invalido: de ({inicio}) e posterior a ate ({fim}).");
        }

        if (fim.DayNumber - inicio.DayNumber + 1 > DiasMaximo)
        {
            throw new RegraDeNegocioException($"Periodo maximo do relatorio e de {DiasMaximo} dias.");
        }

        // O fim e inclusivo no dia inteiro: quem pede ate=10/11 quer o dia 10 dentro.
        var desde = ParaInstante(inicio);
        var fimDoDia = ParaInstante(fim.AddDays(1));

        var lista = await leituras.ListarAsync(equipamento.Id, desde, fimDoDia, cancellationToken);
        var ocorrencias = await alertas.ListarPorPeriodoAsync(equipamento.Id, desde, fimDoDia, cancellationToken);

        var dados = new RelatorioDados(
            encontrado.Instituicao,
            equipamento.Nome,
            equipamento.Tipo,
            equipamento.TempMin,
            equipamento.TempMax,
            equipamento.UmidMax,
            desde,
            fimDoDia.AddTicks(-1),
            lista.Select(leitura => new RelatorioLeitura(
                leitura.RegistradaEm,
                leitura.Temperatura,
                leitura.Umidade,
                leitura.PortaAberta,
                leitura.Origem,
                ForaDaFaixa(equipamento, leitura))).ToList(),
            ocorrencias.Select(alerta => new RelatorioAlerta(
                alerta.CriadoEm,
                alerta.Tipo,
                alerta.Severidade,
                alerta.Status,
                alerta.ResolvidoEm)).ToList());

        var nome = $"relatorio-equipamento-{equipamento.Id}-{inicio:yyyyMMdd}-{fim:yyyyMMdd}.pdf";
        return new RelatorioGerado(pdf.Gerar(dados), nome);
    }

    /// <summary>Mesma comparacao da regra de alerta: temperatura fora da faixa ou umidade acima do limite.</summary>
    private static bool ForaDaFaixa(Equipamento equipamento, Leitura leitura) =>
        leitura.Temperatura > equipamento.TempMax ||
        leitura.Temperatura < equipamento.TempMin ||
        leitura.Umidade > equipamento.UmidMax;

    /// <summary>
    /// DateOnly no fuso do projeto vira instante UTC: o Npgsql recusa DateTimeOffset com offset
    /// diferente de zero em coluna timestamptz (o banco guarda UTC e converte na leitura).
    /// </summary>
    private static DateTimeOffset ParaInstante(DateOnly data) =>
        new DateTimeOffset(
            data.ToDateTime(TimeOnly.MinValue),
            FusoDoProjeto.SaoPaulo.GetUtcOffset(data.ToDateTime(TimeOnly.MinValue))).ToUniversalTime();
}
