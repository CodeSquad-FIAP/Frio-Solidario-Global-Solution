using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using FrioSolidario.Domain;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// CRUD dos equipamentos monitorados. A faixa de temperatura de cada equipamento e o que a
/// regra de alerta vai comparar com cada leitura. Exige token em todas as rotas, escrever exige
/// ADMIN, e tudo fica restrito aos equipamentos da instituicao do token (403 para os de outra).
/// </summary>
[ApiController]
[Authorize]
[Route("api/equipamentos")]
[Produces("application/json")]
public sealed class EquipamentosController(IEquipamentoService servico) : ControllerBase
{
    /// <summary>Lista os equipamentos da instituicao do token (instituicaoId, se vier, precisa ser a mesma).</summary>
    /// <response code="403">instituicaoId de outra instituicao.</response>
    [HttpGet]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(IReadOnlyList<EquipamentoResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<EquipamentoResponse>>> Listar(
        [FromQuery] long? instituicaoId,
        [FromQuery] bool? ativo,
        CancellationToken cancellationToken)
        => Ok(await servico.ListarAsync(instituicaoId, User.InstituicaoObrigatoria(), ativo, cancellationToken));

    /// <summary>Busca um equipamento pelo id.</summary>
    /// <response code="403">Equipamento de outra instituicao.</response>
    [HttpGet("{id:long}")]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(EquipamentoResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<EquipamentoResponse>> Obter(long id, CancellationToken cancellationToken)
        => Ok(await servico.ObterAsync(id, User.InstituicaoObrigatoria(), cancellationToken));

    /// <summary>Cadastra um equipamento. temp_min precisa ser menor que temp_max.</summary>
    /// <response code="403">Usuario nao e ADMIN ou instituicaoId nao e a do token.</response>
    [HttpPost]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(EquipamentoResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<EquipamentoResponse>> Criar(
        EquipamentoRequest request,
        CancellationToken cancellationToken)
    {
        var criado = await servico.CriarAsync(request, User.InstituicaoObrigatoria(), cancellationToken);
        return CreatedAtAction(nameof(Obter), new { id = criado.Id }, criado);
    }

    /// <summary>Atualiza um equipamento existente (sem trocar de instituicao).</summary>
    /// <response code="403">Usuario nao e ADMIN ou equipamento/instituicaoId de outra instituicao.</response>
    [HttpPut("{id:long}")]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(EquipamentoResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<EquipamentoResponse>> Atualizar(
        long id,
        EquipamentoRequest request,
        CancellationToken cancellationToken)
        => Ok(await servico.AtualizarAsync(id, request, User.InstituicaoObrigatoria(), cancellationToken));

    /// <summary>
    /// Desativa o equipamento (DELETE e logico): as leituras e os alertas antigos continuam no banco,
    /// porque o log termico da vigilancia sanitaria nao pode perder historico.
    /// </summary>
    /// <response code="403">Usuario nao e ADMIN ou equipamento de outra instituicao.</response>
    /// <response code="409">Equipamento ja esta desativado.</response>
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
