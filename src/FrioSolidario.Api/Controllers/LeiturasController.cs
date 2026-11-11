using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Ingestao das leituras e a serie que o grafico consome. E o endpoint que liga o sensor ao app:
/// cada leitura gravada passa pela regra de faixa e pode abrir alerta na mesma resposta.
/// </summary>
[ApiController]
[Authorize]
[Route("api")]
[Produces("application/json")]
public sealed class LeiturasController(ILeituraService leituras) : ControllerBase
{
    /// <summary>Registra uma leitura e devolve os alertas que ela abriu.</summary>
    /// <response code="400">Faixa fisica invalida, data no futuro ou payload incompleto.</response>
    /// <response code="403">Equipamento de outra instituicao.</response>
    /// <response code="404">Equipamento nao encontrado.</response>
    [HttpPost("leituras")]
    [ProducesResponseType(typeof(LeituraRegistradaResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<LeituraRegistradaResponse>> Registrar(
        LeituraRequest request,
        CancellationToken cancellationToken)
    {
        var registrada = await leituras.RegistrarAsync(request, User.InstituicaoObrigatoria(), cancellationToken);
        return Created($"/api/leituras/{registrada.Leitura.Id}", registrada);
    }

    /// <summary>Serie de leituras do equipamento para o grafico (24h por padrao, ou 7d).</summary>
    /// <response code="400">periodo diferente de 24h ou 7d.</response>
    [HttpGet("equipamentos/{equipamentoId:long}/leituras")]
    [ProducesResponseType(typeof(IReadOnlyList<LeituraResponse>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<IReadOnlyList<LeituraResponse>>> Listar(
        long equipamentoId,
        [FromQuery] string? periodo,
        CancellationToken cancellationToken)
        => Ok(await leituras.ListarAsync(equipamentoId, periodo, User.InstituicaoObrigatoria(), cancellationToken));
}
