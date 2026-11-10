using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Application.Security;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Login por e-mail e senha. E-mail inexistente e senha errada devolvem exatamente a mesma
/// mensagem (401): diferenciar as duas respostas conta para quem tenta descobrir quais e-mails
/// existem na base. A senha so existe em texto no corpo da requisicao.
/// </summary>
public sealed class AuthService(
    IUsuarioRepository usuarios,
    IInstituicaoRepository instituicoes,
    IPasswordHasher hasher,
    ITokenService tokens) : IAuthService
{
    private const string MensagemDeRecusa = "E-mail ou senha invalidos.";

    public async Task<LoginResponse> LoginAsync(
        LoginRequest request,
        CancellationToken cancellationToken = default)
    {
        var email = (request.Email ?? string.Empty).Trim().ToLowerInvariant();
        var usuario = await usuarios.ObterPorEmailAsync(email, cancellationToken);

        if (usuario is null || !hasher.Verificar(request.Senha ?? string.Empty, usuario.SenhaHash))
        {
            throw new CredenciaisInvalidasException(MensagemDeRecusa);
        }

        var instituicao = await instituicoes.ObterAsync(usuario.InstituicaoId, cancellationToken);
        var token = tokens.Gerar(usuario);

        return new LoginResponse(
            token.Valor,
            token.ExpiraEm,
            usuario.Nome,
            usuario.Papel,
            usuario.InstituicaoId,
            instituicao?.Nome ?? string.Empty);
    }
}
