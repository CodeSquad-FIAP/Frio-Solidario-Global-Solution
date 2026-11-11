using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using FrioSolidario.Domain;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Lotes doados: o dado que sustenta o indicador de desperdicio e o de kg doados. A controller so
/// traduz HTTP; a regra (instituicao do token, retirada coerente) fica no LoteService.
/// </summary>
[ApiController]
[Authorize]
[Route("api/lotes")]
[Produces("application/json")]
public sealed class LotesController(ILoteService servico) : ControllerBase
{
    /// <summary>Lista os lotes da instituicao do token, por validade (o que vence primeiro).</summary>
    [HttpGet]
    [ProducesResponseType(typeof(IReadOnlyList<LoteResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<LoteResponse>>> Listar(
        [FromQuery] StatusLote? status,
        CancellationToken cancellationToken)
        => Ok(await servico.ListarAsync(User.InstituicaoObrigatoria(), status, cancellationToken));

    /// <summary>Busca um lote pelo id.</summary>
    /// <response code="403">Lote de outra instituicao.</response>
    /// <response code="404">Lote nao encontrado.</response>
    [HttpGet("{id:long}")]
    [ProducesResponseType(typeof(LoteResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<LoteResponse>> Obter(long id, CancellationToken cancellationToken)
        => Ok(await servico.ObterAsync(id, User.InstituicaoObrigatoria(), cancellationToken));

    /// <summary>Cadastra um lote doado na instituicao do token. Nasce DISPONIVEL.</summary>
    /// <response code="403">Usuario nao e ADMIN.</response>
    [HttpPost]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(LoteResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<LoteResponse>> Criar(LoteRequest request, CancellationToken cancellationToken)
    {
        var criado = await servico.CriarAsync(request, User.InstituicaoObrigatoria(), cancellationToken);
        return CreatedAtAction(nameof(Obter), new { id = criado.Id }, criado);
    }

    /// <summary>Registra a saida do lote: DISTRIBUIDO ou PERDIDO, com data.</summary>
    /// <response code="403">Usuario nao e ADMIN ou lote de outra instituicao.</response>
    /// <response code="409">Lote ja saiu do estoque.</response>
    [HttpPatch("{id:long}/retirada")]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(LoteResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<ActionResult<LoteResponse>> RegistrarRetirada(
        long id,
        RetiradaDeLoteRequest request,
        CancellationToken cancellationToken)
        => Ok(await servico.RegistrarRetiradaAsync(id, request, User.InstituicaoObrigatoria(), cancellationToken));
}
