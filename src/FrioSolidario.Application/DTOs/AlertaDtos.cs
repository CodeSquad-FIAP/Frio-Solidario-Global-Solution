using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.DTOs;

/// <summary>Alerta como o app recebe: com o nome do equipamento para a lista de alertas.</summary>
public sealed record AlertaResponse(
    long Id,
    long EquipamentoId,
    string Equipamento,
    long LeituraId,
    TipoAlerta Tipo,
    SeveridadeAlerta Severidade,
    StatusAlerta Status,
    DateTimeOffset CriadoEm,
    DateTimeOffset? ResolvidoEm,
    long? ResolvidoPorId)
{
    public static AlertaResponse De(Alerta alerta, string equipamento) => new(
        alerta.Id,
        alerta.EquipamentoId,
        equipamento,
        alerta.LeituraId,
        alerta.Tipo,
        alerta.Severidade,
        alerta.Status,
        alerta.CriadoEm,
        alerta.ResolvidoEm,
        alerta.ResolvidoPorId);
}
