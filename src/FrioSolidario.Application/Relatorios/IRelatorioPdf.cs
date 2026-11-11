using FrioSolidario.Domain;

namespace FrioSolidario.Application.Relatorios;

/// <summary>Uma linha da tabela de leituras do PDF.</summary>
public sealed record RelatorioLeitura(
    DateTimeOffset RegistradaEm,
    decimal Temperatura,
    decimal Umidade,
    bool PortaAberta,
    OrigemLeitura Origem,
    bool ForaDaFaixa);

/// <summary>Um alerta do periodo, para a secao de ocorrencias do PDF.</summary>
public sealed record RelatorioAlerta(
    DateTimeOffset CriadoEm,
    TipoAlerta Tipo,
    SeveridadeAlerta Severidade,
    StatusAlerta Status,
    DateTimeOffset? ResolvidoEm);

/// <summary>
/// Tudo o que o PDF precisa, ja resolvido pela Application: o gerador nao consulta banco nem
/// conhece regra de negocio, so desenha.
/// </summary>
public sealed record RelatorioDados(
    string Instituicao,
    string Equipamento,
    TipoEquipamento TipoEquipamento,
    decimal TempMin,
    decimal TempMax,
    decimal UmidMax,
    DateTimeOffset De,
    DateTimeOffset Ate,
    IReadOnlyList<RelatorioLeitura> Leituras,
    IReadOnlyList<RelatorioAlerta> Alertas);

/// <summary>Geracao do PDF do log termico (vigilancia sanitaria). Implementacao na Infrastructure.</summary>
public interface IRelatorioPdf
{
    byte[] Gerar(RelatorioDados dados);
}

/// <summary>
/// Fuso do projeto, num lugar so. A operacao e em Sao Paulo e o PDF do log e um documento impresso
/// para a equipe local: exibir em UTC deixaria o horario 3h adiantado e o "ate" no dia seguinte.
/// Fixo em America/Sao_Paulo (e nao <c>TimeZoneInfo.Local</c>) para o documento sair igual rodando no
/// Windows ou num container cujo fuso padrao e UTC.
/// </summary>
public static class FusoDoProjeto
{
    public static readonly TimeZoneInfo SaoPaulo = Resolver();

    /// <summary>Mesmo instante, no fuso em que o relatorio e lido e impresso.</summary>
    public static DateTimeOffset Local(DateTimeOffset instante) =>
        TimeZoneInfo.ConvertTime(instante, SaoPaulo);

    private static TimeZoneInfo Resolver()
    {
        try
        {
            // Ids IANA funcionam no Linux e, desde o .NET 6, tambem no Windows (via ICU).
            return TimeZoneInfo.FindSystemTimeZoneById("America/Sao_Paulo");
        }
        catch (Exception excecao) when (excecao is TimeZoneNotFoundException or InvalidTimeZoneException)
        {
            // Nome do registro do Windows: so chega aqui num host sem o banco de fusos IANA.
            return TimeZoneInfo.FindSystemTimeZoneById("E. South America Standard Time");
        }
    }
}
