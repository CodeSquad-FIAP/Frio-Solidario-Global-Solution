using System.ComponentModel.DataAnnotations;
using FrioSolidario.Application.Validation;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.DTOs;

/// <summary>
/// Dados de um equipamento monitorado. A faixa (<see cref="TempMin"/>..<see cref="TempMax"/>) e a
/// umidade maxima precisam ser plausiveis -- as mesmas regras do CHECK do schema. O nome recusa
/// marcacao (<see cref="SemMarcacaoHtmlAttribute"/>) porque aparece no app e no relatorio.
/// </summary>
public sealed record EquipamentoRequest(
    [Range(1, long.MaxValue, ErrorMessage = "Instituicao obrigatoria.")]
    long InstituicaoId,

    [Required, StringLength(150, MinimumLength = 3), SemMarcacaoHtml]
    string Nome,

    [Required]
    TipoEquipamento Tipo,

    [Range(-40, 80, ErrorMessage = "Temperatura minima deve ficar entre -40 e 80.")]
    decimal TempMin,

    [Range(-40, 80, ErrorMessage = "Temperatura maxima deve ficar entre -40 e 80.")]
    decimal TempMax,

    [Range(0.01, 100, ErrorMessage = "Umidade maxima deve ficar entre 0,01 e 100.")]
    decimal UmidMax,

    bool Ativo = true);

/// <summary>Equipamento como o app recebe, ja com o nome da instituicao para a tela de detalhe.</summary>
public sealed record EquipamentoResponse(
    long Id,
    long InstituicaoId,
    string Instituicao,
    string Nome,
    TipoEquipamento Tipo,
    decimal TempMin,
    decimal TempMax,
    decimal UmidMax,
    bool Ativo)
{
    public static EquipamentoResponse De(Equipamento equipamento, string instituicao) => new(
        equipamento.Id,
        equipamento.InstituicaoId,
        instituicao,
        equipamento.Nome,
        equipamento.Tipo,
        equipamento.TempMin,
        equipamento.TempMax,
        equipamento.UmidMax,
        equipamento.Ativo);
}
