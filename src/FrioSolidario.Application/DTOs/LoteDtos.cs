using System.ComponentModel.DataAnnotations;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.DTOs;

/// <summary>
/// Um lote doado, como o app do voluntario cadastra. A instituicao vem do token, nunca do corpo:
/// quem cadastra so escreve na propria. Os limites repetem o CHECK do schema (kg > 0, texto com
/// tamanho maximo) para o erro sair em 400 com mensagem, e nao como 500 do banco.
/// </summary>
public sealed record LoteRequest(
    [Required, StringLength(200, MinimumLength = 3, ErrorMessage = "Descricao deve ter de 3 a 200 caracteres.")]
    string Descricao,

    [Required, StringLength(80, MinimumLength = 3, ErrorMessage = "Categoria deve ter de 3 a 80 caracteres.")]
    string Categoria,

    [Range(0.01, 99999999, ErrorMessage = "Peso em kg deve ser maior que zero.")]
    decimal Kg,

    [Required]
    DateOnly Validade);

/// <summary>
/// Retirada do lote: sai de DISPONIVEL como DISTRIBUIDO (entregue) ou PERDIDO (descartado). O CHECK
/// ck_lote_retirada_coerente exige a data nos dois casos, entao ela e gravada sempre.
/// </summary>
public sealed record RetiradaDeLoteRequest(
    [Required]
    StatusLote Status,

    /// <summary>Quando saiu. Sem valor, o servidor usa o instante do recebimento.</summary>
    DateTimeOffset? RetiradoEm = null);

/// <summary>Lote como o app recebe (dados do indicador de desperdicio).</summary>
public sealed record LoteResponse(
    long Id,
    long InstituicaoId,
    string Descricao,
    string Categoria,
    decimal Kg,
    DateOnly Validade,
    StatusLote Status,
    DateTimeOffset? RetiradoEm)
{
    public static LoteResponse De(Lote lote) => new(
        lote.Id,
        lote.InstituicaoId,
        lote.Descricao,
        lote.Categoria,
        lote.Kg,
        lote.Validade,
        lote.Status,
        lote.RetiradoEm);
}
