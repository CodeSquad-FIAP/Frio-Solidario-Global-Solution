using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using FrioSolidario.Application.Security;
using FrioSolidario.Domain.Entities;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace FrioSolidario.Infrastructure.Security;

/// <summary>
/// Emite o JWT do app. As claims carregam o que a Api precisa decidir sem consultar o banco:
/// papel (ADMIN/VOLUNTARIO) para autorizacao e instituicao para restringir os dados do usuario
/// a instituicao dele.
/// </summary>
public sealed class JwtTokenService(IOptions<JwtOptions> options) : ITokenService
{
    public TokenGerado Gerar(Usuario usuario)
    {
        var opcoes = options.Value;
        var expiraEm = DateTimeOffset.UtcNow.AddMinutes(opcoes.ExpiraMinutos);

        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, usuario.Id.ToString()),
            new Claim(JwtRegisteredClaimNames.Email, usuario.Email),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new Claim(ClaimTypes.Name, usuario.Nome),
            new Claim(ClaimTypes.Role, usuario.Papel.ToString()),
            new Claim("instituicao", usuario.InstituicaoId.ToString())
        };

        var credenciais = new SigningCredentials(
            new SymmetricSecurityKey(Encoding.UTF8.GetBytes(opcoes.Key)),
            SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: opcoes.Issuer,
            audience: opcoes.Audience,
            claims: claims,
            notBefore: DateTime.UtcNow,
            expires: expiraEm.UtcDateTime,
            signingCredentials: credenciais);

        return new TokenGerado(new JwtSecurityTokenHandler().WriteToken(token), expiraEm);
    }
}
