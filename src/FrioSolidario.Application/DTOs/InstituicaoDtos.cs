using System.ComponentModel.DataAnnotations;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.DTOs;

/// <summary>Dados que o app envia para cadastrar ou editar uma instituicao.</summary>
public sealed record InstituicaoRequest(
    [Required, StringLength(150, MinimumLength = 3)]
    string Nome,

    [Required(ErrorMessage = "CNPJ e obrigatorio.")]
    [RegularExpression("^[0-9]{14}$", ErrorMessage = "CNPJ deve ter 14 digitos, sem pontuacao.")]
    string Cnpj,

    [Required]
    TipoInstituicao Tipo,

    [Required, StringLength(120, MinimumLength = 2)]
    string Cidade,

    [Required, StringLength(255, MinimumLength = 3)]
    string Endereco);

/// <summary>Instituicao como o app recebe (sem objeto de banco).</summary>
public sealed record InstituicaoResponse(
    long Id,
    string Nome,
    string Cnpj,
    TipoInstituicao Tipo,
    string Cidade,
    string Endereco)
{
    public static InstituicaoResponse De(Instituicao instituicao) => new(
        instituicao.Id,
        instituicao.Nome,
        instituicao.Cnpj,
        instituicao.Tipo,
        instituicao.Cidade,
        instituicao.Endereco);
}
