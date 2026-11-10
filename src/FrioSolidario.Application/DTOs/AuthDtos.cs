using System.ComponentModel.DataAnnotations;
using FrioSolidario.Domain;

namespace FrioSolidario.Application.DTOs;

/// <summary>Credenciais do app. A senha trafega so no corpo do HTTPS e nunca e gravada em texto puro.</summary>
public sealed record LoginRequest(
    [Required, EmailAddress, StringLength(254)]
    string Email,

    [Required, StringLength(100, MinimumLength = 6)]
    string Senha);

/// <summary>
/// Resposta do login: o token e o que o app guarda e manda em <c>Authorization: Bearer</c>.
/// O nome, o papel e a instituicao vem junto para o app montar o cabecalho sem outra chamada.
/// </summary>
public sealed record LoginResponse(
    string Token,
    DateTimeOffset ExpiraEm,
    string Nome,
    PapelUsuario Papel,
    long InstituicaoId,
    string Instituicao);
