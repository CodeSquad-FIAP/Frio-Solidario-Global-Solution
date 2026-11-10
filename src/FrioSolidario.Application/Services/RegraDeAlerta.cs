using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>Alerta que a regra concluiu que deve ser aberto, com o motivo que vai no log e no relatorio.</summary>
public sealed record AlertaProposto(TipoAlerta Tipo, SeveridadeAlerta Severidade, string Motivo);

/// <summary>
/// A regra do produto, isolada e sem banco: decide, para cada leitura, se ela fecha uma sequencia
/// que merece alerta. Cada decisao e uma comparacao com a faixa do equipamento, nunca com um valor
/// fixo no codigo -- trocar a faixa no cadastro muda o comportamento aqui sem tocar em codigo.
///
/// Decisoes tomadas (documentadas no README, seção "Regra de alerta"):
/// - temperatura e umidade exigem <see cref="LeiturasConsecutivas"/> leituras seguidas fora da faixa,
///   porque a porta abrindo por um minuto derruba a leitura de um sensor so;
/// - porta aberta nao conta leituras, conta tempo: o alerta nasce quando a sequencia ininterrupta de
///   porta aberta passa de <see cref="PortaLimite"/> e sobe para ALTA em <see cref="PortaLimiteAlta"/>;
/// - a severidade da temperatura cresce com o tamanho do desvio (a mesma faixa de severidade que o
///   grupo definiu no schema: BAIXA, MEDIA, ALTA, CRITICA).
/// </summary>
public static class RegraDeAlerta
{
    /// <summary>Leituras seguidas fora da faixa necessarias para abrir alerta de temperatura ou umidade.</summary>
    public const int LeiturasConsecutivas = 3;

    /// <summary>Porta aberta por mais que isso abre alerta.</summary>
    public static readonly TimeSpan PortaLimite = TimeSpan.FromMinutes(5);

    /// <summary>Porta aberta por mais que isso eleva a severidade para ALTA.</summary>
    public static readonly TimeSpan PortaLimiteAlta = TimeSpan.FromMinutes(15);

    public static IReadOnlyList<AlertaProposto> Avaliar(
        Equipamento equipamento,
        Leitura nova,
        IReadOnlyList<Leitura> anteriores)
    {
        var recentes = new List<Leitura>(LeiturasConsecutivas) { nova };
        recentes.AddRange(
            anteriores
                .Where(leitura => leitura.Id != nova.Id && leitura.RegistradaEm <= nova.RegistradaEm)
                .OrderByDescending(leitura => leitura.RegistradaEm)
                .Take(LeiturasConsecutivas - 1));

        var propostos = new List<AlertaProposto>();

        if (recentes.Count == LeiturasConsecutivas)
        {
            if (recentes.All(leitura => leitura.Temperatura > equipamento.TempMax))
            {
                var desvio = nova.Temperatura - equipamento.TempMax;
                propostos.Add(new AlertaProposto(
                    TipoAlerta.TEMP_ALTA,
                    SeveridadePorDesvio(desvio),
                    $"{LeiturasConsecutivas} leituras acima de {equipamento.TempMax} C (+{desvio:0.0} C)"));
            }
            else if (recentes.All(leitura => leitura.Temperatura < equipamento.TempMin))
            {
                var desvio = equipamento.TempMin - nova.Temperatura;
                propostos.Add(new AlertaProposto(
                    TipoAlerta.TEMP_BAIXA,
                    SeveridadePorDesvio(desvio),
                    $"{LeiturasConsecutivas} leituras abaixo de {equipamento.TempMin} C (-{desvio:0.0} C)"));
            }

            if (recentes.All(leitura => leitura.Umidade > equipamento.UmidMax))
            {
                var excesso = nova.Umidade - equipamento.UmidMax;
                propostos.Add(new AlertaProposto(
                    TipoAlerta.UMIDADE,
                    SeveridadePorUmidade(excesso),
                    $"{LeiturasConsecutivas} leituras acima de {equipamento.UmidMax}% de umidade (+{excesso:0.0}%)"));
            }
        }

        var portaAberta = TempoDePortaAberta(nova, anteriores);
        if (portaAberta >= PortaLimite)
        {
            propostos.Add(new AlertaProposto(
                TipoAlerta.PORTA_ABERTA,
                portaAberta >= PortaLimiteAlta ? SeveridadeAlerta.ALTA : SeveridadeAlerta.MEDIA,
                $"porta aberta ha {portaAberta.TotalMinutes:0} min"));
        }

        return propostos;
    }

    /// <summary>Ha quanto tempo a porta esta aberta sem interrupcao, contando a leitura nova.</summary>
    private static TimeSpan TempoDePortaAberta(Leitura nova, IReadOnlyList<Leitura> anteriores)
    {
        if (!nova.PortaAberta)
        {
            return TimeSpan.Zero;
        }

        var inicio = nova.RegistradaEm;
        foreach (var anterior in anteriores.OrderByDescending(leitura => leitura.RegistradaEm))
        {
            if (anterior.RegistradaEm > nova.RegistradaEm)
            {
                continue;
            }

            if (!anterior.PortaAberta)
            {
                break;
            }

            inicio = anterior.RegistradaEm;
        }

        return nova.RegistradaEm - inicio;
    }

    /// <summary>Temperatura: 1,5 C de desvio ja e ALTA e 3 C e CRITICA (proposta para o grupo fechar).</summary>
    private static SeveridadeAlerta SeveridadePorDesvio(decimal desvio) => desvio switch
    {
        >= 3m => SeveridadeAlerta.CRITICA,
        >= 1.5m => SeveridadeAlerta.ALTA,
        _ => SeveridadeAlerta.MEDIA
    };

    /// <summary>Umidade: ate 5 pontos acima do limite incomoda (BAIXA); acima disso e MEDIA.</summary>
    private static SeveridadeAlerta SeveridadePorUmidade(decimal excesso) =>
        excesso > 5m ? SeveridadeAlerta.MEDIA : SeveridadeAlerta.BAIXA;
}
