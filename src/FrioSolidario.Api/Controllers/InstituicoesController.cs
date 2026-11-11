using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using FrioSolidario.Domain;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// CRUD de instituicoes. A controller so traduz HTTP: validacao de forma fica nas
/// DataAnnotations do DTO, regra de negocio no InstituicaoService e banco no repositorio.
/// Exige token em todas as rotas; escrever exige ADMIN, e alterar/remover so a propria instituicao.
/// </summary>
[ApiController]
[Authorize]
[Route("api/instituicoes")]
[Produces("application/json")]
public sealed class InstituicoesController(IInstituicaoService servico, IEquipamentoService equipamentos) : ControllerBase
{
    /// <summary>Lista todas as instituicoes em ordem alfabetica.</summary>
    [HttpGet]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(IReadOnlyList<InstituicaoResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<InstituicaoResponse>>> Listar(CancellationToken cancellationToken)
        => Ok(await servico.ListarAsync(cancellationToken));

    /// <summary>Busca uma instituicao pelo id.</summary>
    /// <response code="404">Instituicao nao encontrada.</response>
    [HttpGet("{id:long}")]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(InstituicaoResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<InstituicaoResponse>> Obter(long id, CancellationToken cancellationToken)
        => Ok(await servico.ObterAsync(id, cancellationToken));

    /// <summary>Lista os equipamentos de uma instituicao.</summary>
    /// <response code="403">Instituicao de outro usuario.</response>
    [HttpGet("{id:long}/equipamentos")]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(IReadOnlyList<EquipamentoResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<EquipamentoResponse>>> ListarEquipamentos(
        long id,
        [FromQuery] bool? ativo,
        CancellationToken cancellationToken)
        => Ok(await equipamentos.ListarAsync(id, User.InstituicaoObrigatoria(), ativo, cancellationToken));

    /// <summary>Cadastra uma instituicao. CNPJ vai sem pontuacao e nao pode repetir.</summary>
    /// <response code="409">CNPJ ja cadastrado.</response>
    [HttpPost]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(InstituicaoResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<ActionResult<InstituicaoResponse>> Criar(
        InstituicaoRequest request,
        CancellationToken cancellationToken)
    {
        var criada = await servico.CriarAsync(request, cancellationToken);
        return CreatedAtAction(nameof(Obter), new { id = criada.Id }, criada);
    }

    /// <summary>Atualiza os dados de uma instituicao existente.</summary>
    /// <response code="403">Usuario nao e ADMIN ou a instituicao nao e a dele.</response>
    [HttpPut("{id:long}")]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(InstituicaoResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<ActionResult<InstituicaoResponse>> Atualizar(
        long id,
        InstituicaoRequest request,
        CancellationToken cancellationToken)
        => Ok(await servico.AtualizarAsync(id, request, User.InstituicaoObrigatoria(), cancellationToken));

    /// <summary>Remove uma instituicao sem equipamentos, usuarios ou lotes vinculados.</summary>
    /// <response code="403">Usuario nao e ADMIN ou a instituicao nao e a dele.</response>
    /// <response code="409">Instituicao possui vinculos (FK ON DELETE RESTRICT).</response>
    [HttpDelete("{id:long}")]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Remover(long id, CancellationToken cancellationToken)
    {
        await servico.RemoverAsync(id, User.InstituicaoObrigatoria(), cancellationToken);
        return NoContent();
    }
}
