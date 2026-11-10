namespace FrioSolidario.Domain.Entities;

/// <summary>
/// Lote de alimento registrado por uma instituicao. Sai de DISPONIVEL apenas com
/// <see cref="RetiradoEm"/> preenchido (DISTRIBUIDO ou PERDIDO).
/// </summary>
public sealed class Lote
{
    public long Id { get; set; }

    public long InstituicaoId { get; set; }

    public string Descricao { get; set; } = string.Empty;

    public string Categoria { get; set; } = string.Empty;

    public decimal Kg { get; set; }

    public DateOnly Validade { get; set; }

    public StatusLote Status { get; set; } = StatusLote.DISPONIVEL;

    public DateTimeOffset? RetiradoEm { get; set; }
}
