namespace FrioSolidario.Domain.Entities;

/// <summary>
/// Alerta aberto a partir de uma leitura. Nasce ABERTO (sem data nem responsavel) e, ao ser
/// resolvido, exige <see cref="ResolvidoEm"/> e <see cref="ResolvidoPorId"/> preenchidos.
/// </summary>
public sealed class Alerta
{
    public long Id { get; set; }

    public long EquipamentoId { get; set; }

    /// <summary>Leitura que disparou o alerta; precisa ser do mesmo equipamento (FK composta no banco).</summary>
    public long LeituraId { get; set; }

    public TipoAlerta Tipo { get; set; }

    public SeveridadeAlerta Severidade { get; set; }

    public StatusAlerta Status { get; set; } = StatusAlerta.ABERTO;

    public DateTimeOffset CriadoEm { get; set; }

    public DateTimeOffset? ResolvidoEm { get; set; }

    public long? ResolvidoPorId { get; set; }
}
