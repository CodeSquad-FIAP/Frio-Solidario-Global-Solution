using System.Security.Claims;
using FrioSolidario.Application.Common;

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

    /// <summary>
    /// Instituicao do token, obrigatoria. Os servicos tratam instituicao nula como "sem restricao"
    /// (chamada interna); se um token sem a claim chegasse ate eles, o usuario veria tudo. Por isso a
    /// Api falha fechado: token sem instituicao e 403.
    /// </summary>
    public static long InstituicaoObrigatoria(this ClaimsPrincipal usuario) =>
        usuario.Instituicao() ?? throw new AcessoNegadoException("Token sem instituicao do usuario.");

    /// <summary>Id do usuario logado (claim <c>sub</c>), gravado em alerta.resolvido_por_id.</summary>
    public static long? UsuarioId(this ClaimsPrincipal usuario) =>
        long.TryParse(usuario.FindFirstValue("sub"), out var id) ? id : null;
}
