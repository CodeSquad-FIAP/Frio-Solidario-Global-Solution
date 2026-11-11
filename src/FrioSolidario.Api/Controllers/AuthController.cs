using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace FrioSolidario.Api.Controllers;

/// <summary>
/// Porta de entrada do sistema. Unico endpoint publico da API: sem o token devolvido aqui nenhuma
/// tela do app funciona. Por ser publico, e o alvo natural de quem tenta adivinhar senha: o login
/// tem limite de tentativas por IP (politica de Program.cs), e o excesso volta 429.
/// </summary>
[ApiController]
[Route("api/auth")]
[AllowAnonymous]
public sealed class AuthController(IAuthService autenticacao) : ControllerBase
{
    /// <summary>Autentica e-mail e senha e devolve o token do app.</summary>
    /// <response code="401">E-mail inexistente ou senha errada (mesma mensagem nos dois casos).</response>
    /// <response code="429">Tentativas demais deste IP; espere um minuto.</response>
    [HttpPost("login")]
    [EnableRateLimiting(PoliticasDeLimite.Login)]
    [ProducesResponseType(typeof(LoginResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status429TooManyRequests)]
    public async Task<ActionResult<LoginResponse>> Login(
        LoginRequest request,
        CancellationToken cancellationToken)
        => Ok(await autenticacao.LoginAsync(request, cancellationToken));
}
