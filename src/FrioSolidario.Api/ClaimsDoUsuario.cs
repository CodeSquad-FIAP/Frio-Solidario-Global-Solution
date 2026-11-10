using System.Security.Claims;

namespace FrioSolidario.Api;

/// <summary>
/// Le as claims do token uma vez, num lugar so. O papel e a instituicao vem do JWT: a Api nao
/// consulta o banco para saber de quem e o request.
/// </summary>
internal static class ClaimsDoUsuario
{
    /// <summary>Instituicao do usuario logado, usada para restringir leituras, alertas e relatorio.</summary>
    public static long? Instituicao(this ClaimsPrincipal usuario) =>
        long.TryParse(usuario.FindFirstValue("instituicao"), out var id) ? id : null;

    /// <summary>Id do usuario logado (claim <c>sub</c>), gravado em alerta.resolvido_por_id.</summary>
    public static long? UsuarioId(this ClaimsPrincipal usuario) =>
        long.TryParse(usuario.FindFirstValue("sub"), out var id) ? id : null;
}
