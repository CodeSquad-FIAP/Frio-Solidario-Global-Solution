namespace FrioSolidario.Domain.Entities;

/// <summary>
/// Equipamento monitorado. A faixa (<see cref="TempMin"/>..<see cref="TempMax"/>) e o
/// <see cref="UmidMax"/> sao o que a API compara com cada leitura para abrir alerta.
/// </summary>
public sealed class Equipamento
{
    public long Id { get; set; }

    public long InstituicaoId { get; set; }

    public string Nome { get; set; } = string.Empty;

    public TipoEquipamento Tipo { get; set; }

    public decimal TempMin { get; set; }

    public decimal TempMax { get; set; }

    public decimal UmidMax { get; set; }

    public bool Ativo { get; set; } = true;
}
