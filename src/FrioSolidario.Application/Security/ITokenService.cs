using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Security;

/// <summary>Token gerado e o instante em que ele expira (o app usa para renovar antes de vencer).</summary>
public sealed record TokenGerado(string Valor, DateTimeOffset ExpiraEm);

/// <summary>
/// Emissao do token de acesso. A implementacao (Infrastructure) conhece o algoritmo e a chave;
/// a Application so sabe que existe um token a devolver para o app.
/// </summary>
public interface ITokenService
{
    TokenGerado Gerar(Usuario usuario);
}
