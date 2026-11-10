namespace FrioSolidario.Domain.Entities;

/// <summary>Uma medicao do sensor (ou simulada) de temperatura, umidade e porta.</summary>
public sealed class Leitura
{
    public long Id { get; set; }

    public long EquipamentoId { get; set; }

    public decimal Temperatura { get; set; }

    public decimal Umidade { get; set; }

    public bool PortaAberta { get; set; }

    public DateTimeOffset RegistradaEm { get; set; }

    public OrigemLeitura Origem { get; set; }
}
