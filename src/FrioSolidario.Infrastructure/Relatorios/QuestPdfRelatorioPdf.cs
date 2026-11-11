using System.Globalization;
using FrioSolidario.Application.Relatorios;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;

namespace FrioSolidario.Infrastructure.Relatorios;

/// <summary>
/// PDF do log termico com QuestPDF (licenca Community, gratuita para o projeto academico).
/// O documento existe para ser impresso: cabecalho com instituicao e faixa, uma linha por leitura
/// com as fora da faixa marcadas, e a secao de alertas do periodo.
/// </summary>
public sealed class QuestPdfRelatorioPdf : IRelatorioPdf
{
    static QuestPdfRelatorioPdf()
    {
        // QuestPDF exige a licenca declarada antes do primeiro documento.
        QuestPDF.Settings.License = LicenseType.Community;
    }

    public byte[] Gerar(RelatorioDados dados) =>
        Document.Create(documento =>
        {
            documento.Page(pagina =>
            {
                pagina.Size(PageSizes.A4);
                pagina.Margin(28);
                pagina.DefaultTextStyle(estilo => estilo.FontSize(9));

                pagina.Header().Column(cabecalho =>
                {
                    cabecalho.Item().Text("FRIO SOLIDARIO - LOG TERMICO").FontSize(14).Bold();
                    cabecalho.Item().Text($"{dados.Instituicao} - {dados.Equipamento} ({dados.TipoEquipamento})");
                    cabecalho.Item().Text(
                        $"Faixa: {dados.TempMin:0.0} C a {dados.TempMax:0.0} C | Umidade maxima: {dados.UmidMax:0.0}%");
                    cabecalho.Item().Text(
                        $"Periodo (horario de Brasilia): {Hora(dados.De)} a {Hora(dados.Ate)}");
                    cabecalho.Item().PaddingBottom(8).Text($"Emitido em {DateTimeOffset.Now:dd/MM/yyyy HH:mm}");
                });

                pagina.Content().Column(conteudo =>
                {
                    if (dados.Leituras.Count == 0)
                    {
                        conteudo.Item().PaddingTop(12).Text("Sem leituras no periodo.").Italic();
                    }
                    else
                    {
                        conteudo.Item().Text($"{dados.Leituras.Count} leitura(s) no periodo").Bold();
                        conteudo.Item().Table(TabelaDeLeituras(dados));
                    }

                    conteudo.Item().PaddingTop(14).Text($"Alertas no periodo ({dados.Alertas.Count})").Bold();
                    if (dados.Alertas.Count == 0)
                    {
                        conteudo.Item().Text("Nenhum alerta aberto no periodo.").Italic();
                    }
                    else
                    {
                        conteudo.Item().Table(TabelaDeAlertas(dados));
                    }

                    conteudo.Item().PaddingTop(18).Text(
                        "Documento gerado automaticamente pela API do Frio Solidario a partir das leituras " +
                        "registradas pelos sensores.").FontSize(8).Italic();
                });

                pagina.Footer().AlignCenter().Text(texto =>
                {
                    texto.Span($"{dados.Equipamento} - pagina ");
                    texto.CurrentPageNumber();
                    texto.Span(" de ");
                    texto.TotalPages();
                });
            });
        }).GeneratePdf();

    private static Action<TableDescriptor> TabelaDeLeituras(RelatorioDados dados) => tabela =>
    {
        tabela.ColumnsDefinition(colunas =>
        {
            colunas.ConstantColumn(96);
            colunas.RelativeColumn();
            colunas.RelativeColumn();
            colunas.ConstantColumn(44);
            colunas.ConstantColumn(72);
            colunas.ConstantColumn(66);
        });

        tabela.Header(cabecalho =>
        {
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Horario").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Temperatura").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Umidade").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Porta").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Origem").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Situacao").Bold();
        });

        foreach (var leitura in dados.Leituras)
        {
            var fundo = leitura.ForaDaFaixa ? Colors.Red.Lighten4 : Colors.White;
            tabela.Cell().Background(fundo).Padding(3).Text(Hora(leitura.RegistradaEm));
            tabela.Cell().Background(fundo).Padding(3).Text($"{leitura.Temperatura:0.0} C");
            tabela.Cell().Background(fundo).Padding(3).Text($"{leitura.Umidade:0.0}%");
            tabela.Cell().Background(fundo).Padding(3).Text(leitura.PortaAberta ? "Aberta" : "Fechada");
            tabela.Cell().Background(fundo).Padding(3).Text(leitura.Origem.ToString());
            tabela.Cell().Background(fundo).Padding(3).Text(leitura.ForaDaFaixa ? "Fora da faixa" : "Normal");
        }
    };

    private static Action<TableDescriptor> TabelaDeAlertas(RelatorioDados dados) => tabela =>
    {
        tabela.ColumnsDefinition(colunas =>
        {
            colunas.ConstantColumn(96);
            colunas.RelativeColumn();
            colunas.ConstantColumn(58);
            colunas.ConstantColumn(60);
            colunas.ConstantColumn(96);
        });

        tabela.Header(cabecalho =>
        {
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Criado em").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Tipo").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Severidade").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Status").Bold();
            cabecalho.Cell().Background(Colors.Grey.Lighten2).Padding(3).Text("Resolvido em").Bold();
        });

        foreach (var alerta in dados.Alertas)
        {
            tabela.Cell().Padding(3).Text(Hora(alerta.CriadoEm));
            tabela.Cell().Padding(3).Text(alerta.Tipo.ToString());
            tabela.Cell().Padding(3).Text(alerta.Severidade.ToString());
            tabela.Cell().Padding(3).Text(alerta.Status.ToString());
            tabela.Cell().Padding(3).Text(alerta.ResolvidoEm is null
                ? "-"
                : Hora(alerta.ResolvidoEm.Value));
        }
    };

    /// <summary>
    /// Instante no fuso do projeto antes de virar texto. Sem isso o PDF imprime o valor UTC que veio
    /// do banco: a leitura das 18:01 aparecia como 21:01 e o periodo terminava no dia seguinte.
    /// </summary>
    private static string Hora(DateTimeOffset instante) =>
        FusoDoProjeto.Local(instante).ToString("dd/MM/yyyy HH:mm", CultureInfo.InvariantCulture);
}
