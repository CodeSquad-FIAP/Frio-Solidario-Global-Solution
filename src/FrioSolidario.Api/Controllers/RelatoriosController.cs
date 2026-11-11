using FrioSolidario.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Log termico em PDF. O periodo padrao e de 7 dias e o fim e inclusivo no dia inteiro. Periodo sem
/// leitura devolve PDF com "Sem leituras no periodo" e nao 500 -- o documento precisa existir.
/// </summary>
[ApiController]
[Authorize]
public sealed class RelatoriosController(IRelatorioService relatorios) : ControllerBase
{
    /// <summary>Baixa o PDF do log termico do equipamento no periodo.</summary>
    /// <response code="400">Periodo invalido (de posterior a ate, ou mais de 92 dias).</response>
    /// <response code="403">Equipamento de outra instituicao.</response>
    /// <response code="404">Equipamento nao encontrado.</response>
    [HttpGet("api/equipamentos/{equipamentoId:long}/relatorio")]
    [Produces("application/pdf")]
    [ProducesResponseType(typeof(FileContentResult), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Baixar(
        long equipamentoId,
        [FromQuery] DateOnly? de,
        [FromQuery] DateOnly? ate,
        CancellationToken cancellationToken)
    {
        var relatorio = await relatorios.GerarAsync(equipamentoId, de, ate, User.InstituicaoObrigatoria(), cancellationToken);
        return File(relatorio.Conteudo, "application/pdf", relatorio.NomeArquivo);
    }
}
