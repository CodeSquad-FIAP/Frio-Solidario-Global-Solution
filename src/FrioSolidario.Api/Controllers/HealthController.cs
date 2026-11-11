using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Camada de controller: so recebe a requisicao, chama o servico e devolve o status HTTP.
/// Nenhuma regra de negocio mora aqui. Publico de proposito: e o que se testa antes de ter token.
/// </summary>
[ApiController]
[AllowAnonymous]
[Route("api/[controller]")]
[Produces("application/json")]
public sealed class HealthController : ControllerBase
{
    private readonly IHealthService _healthService;

    public HealthController(IHealthService healthService) => _healthService = healthService;

    /// <summary>Confere se a API esta no ar e em qual camada ela esta respondendo.</summary>
    /// <response code="200">API no ar.</response>
    [HttpGet]
    [ProducesResponseType(typeof(HealthResponse), StatusCodes.Status200OK)]
    public async Task<ActionResult<HealthResponse>> Get(CancellationToken cancellationToken)
        => Ok(await _healthService.VerificarAsync(cancellationToken));
}
