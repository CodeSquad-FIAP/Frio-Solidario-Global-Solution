using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using FrioSolidario.Domain;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Fila de alertas do app e a resolucao feita no local pelo voluntario. A fila vem ordenada por
/// severidade (CRITICA primeiro) e, dentro da mesma severidade, do mais recente para o mais antigo.
/// </summary>
[ApiController]
[Authorize]
[Route("api/alertas")]
[Produces("application/json")]
public sealed class AlertasController(IAlertaService alertas) : ControllerBase
{
    /// <summary>Lista os alertas da instituicao do token, opcionalmente por status e por equipamento.</summary>
    /// <response code="403">equipamentoId de outra instituicao.</response>
    [HttpGet]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(IReadOnlyList<AlertaResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<AlertaResponse>>> Listar(
        [FromQuery] StatusAlerta? status,
        [FromQuery] long? equipamentoId,
        CancellationToken cancellationToken)
        => Ok(await alertas.ListarAsync(status, equipamentoId, User.InstituicaoObrigatoria(), cancellationToken));

    /// <summary>Marca o alerta como resolvido, gravando quem resolveu e quando.</summary>
    /// <response code="401">Token sem identificacao de usuario.</response>
    /// <response code="403">Alerta de equipamento de outra instituicao.</response>
    /// <response code="404">Alerta nao encontrado.</response>
    /// <response code="409">Alerta ja resolvido.</response>
    [HttpPatch("{id:long}/resolver")]
    [ProducesResponseType(typeof(AlertaResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<ActionResult<AlertaResponse>> Resolver(long id, CancellationToken cancellationToken)
    {
        var usuarioId = User.UsuarioId();
        if (usuarioId is null)
        {
            return Unauthorized();
        }

        return Ok(await alertas.ResolverAsync(id, usuarioId.Value, User.InstituicaoObrigatoria(), cancellationToken));
    }
}
