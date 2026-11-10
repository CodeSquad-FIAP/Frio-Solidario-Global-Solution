using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Porta de entrada do sistema. Unico endpoint publico da API: sem o token devolvido aqui nenhuma
/// tela do app funciona.
/// </summary>
[ApiController]
[Route("api/auth")]
[AllowAnonymous]
public sealed class AuthController(IAuthService autenticacao) : ControllerBase
{
    /// <summary>Autentica e-mail e senha e devolve o token do app.</summary>
    /// <response code="401">E-mail inexistente ou senha errada (mesma mensagem nos dois casos).</response>
    [HttpPost("login")]
    [ProducesResponseType(typeof(LoginResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    public async Task<ActionResult<LoginResponse>> Login(
        LoginRequest request,
        CancellationToken cancellationToken)
        => Ok(await autenticacao.LoginAsync(request, cancellationToken));
}
