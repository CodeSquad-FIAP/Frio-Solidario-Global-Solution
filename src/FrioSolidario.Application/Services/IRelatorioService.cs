namespace FrioSolidario.Application.Services;

/// <summary>PDF pronto para o app baixar e compartilhar.</summary>
public sealed record RelatorioGerado(byte[] Conteudo, string NomeArquivo);

/// <summary>
/// Log termico exportavel: e o documento que substitui a planilha de papel exigida pela
/// vigilancia sanitaria (decisao da reuniao de 07/11).
/// </summary>
public interface IRelatorioService
{
    Task<RelatorioGerado> GerarAsync(
        long equipamentoId,
        DateOnly? de = null,
        DateOnly? ate = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);
}
