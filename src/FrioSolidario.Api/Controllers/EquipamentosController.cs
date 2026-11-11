using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using FrioSolidario.Domain;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// CRUD dos equipamentos monitorados. A faixa de temperatura de cada equipamento e o que a
/// regra de alerta vai comparar com cada leitura.
/// </summary>
[ApiController]
[Route("api/equipamentos")]
[Produces("application/json")]
public sealed class EquipamentosController(IEquipamentoService servico) : ControllerBase
{
    /// <summary>Lista os equipamentos, opcionalmente filtrando por instituicao.</summary>
    [HttpGet]
    [ProducesResponseType(typeof(IReadOnlyList<EquipamentoResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<EquipamentoResponse>>> Listar(
        [FromQuery] long? instituicaoId,
        CancellationToken cancellationToken)
        => Ok(await servico.ListarAsync(instituicaoId, cancellationToken));

    /// <summary>Busca um equipamento pelo id.</summary>
    [HttpGet("{id:long}")]
    [ProducesResponseType(typeof(EquipamentoResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<EquipamentoResponse>> Obter(long id, CancellationToken cancellationToken)
        => Ok(await servico.ObterAsync(id, cancellationToken));

    /// <summary>Cadastra um equipamento. temp_min precisa ser menor que temp_max.</summary>
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
        var criado = await servico.CriarAsync(request, cancellationToken);
        return CreatedAtAction(nameof(Obter), new { id = criado.Id }, criado);
    }

    /// <summary>Atualiza um equipamento existente.</summary>
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
        => Ok(await servico.AtualizarAsync(id, request, cancellationToken));

    /// <summary>Remove um equipamento sem leituras nem alertas vinculados.</summary>
    /// <response code="409">Equipamento possui leituras ou alertas (FK ON DELETE RESTRICT).</response>
    [HttpDelete("{id:long}")]
    [Authorize(Roles = nameof(PapelUsuario.ADMIN))]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Remover(long id, CancellationToken cancellationToken)
    {
        await servico.RemoverAsync(id, cancellationToken);
        return NoContent();
    }
}
