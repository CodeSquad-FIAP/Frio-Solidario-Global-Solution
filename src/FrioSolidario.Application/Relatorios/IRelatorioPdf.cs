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
