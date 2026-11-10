using System.ComponentModel.DataAnnotations;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.DTOs;

/// <summary>
/// Uma medicao vinda do sensor (ou da simulacao do Roger). <see cref="RegistradaEm"/> e opcional:
/// quando vem nula, o servidor usa o instante do recebimento, que e o caso do sensor publicando
/// ao vivo. Os limites fisicos (temperatura -40..80, umidade 0..100) sao os mesmos CHECK do schema.
/// </summary>
public sealed record LeituraRequest(
    [Range(1, long.MaxValue, ErrorMessage = "Equipamento obrigatorio.")]
    long EquipamentoId,

    [Range(-40, 80, ErrorMessage = "Temperatura deve ficar entre -40 e 80.")]
    decimal Temperatura,

    [Range(0, 100, ErrorMessage = "Umidade deve ficar entre 0 e 100.")]
    decimal Umidade,

    bool PortaAberta,

    DateTimeOffset? RegistradaEm = null,

    OrigemLeitura Origem = OrigemLeitura.SENSOR);

/// <summary>Leitura como a API devolve.</summary>
public sealed record LeituraResponse(
    long Id,
    long EquipamentoId,
    decimal Temperatura,
    decimal Umidade,
    bool PortaAberta,
    DateTimeOffset RegistradaEm,
    OrigemLeitura Origem)
{
    public static LeituraResponse De(Leitura leitura) => new(
        leitura.Id,
        leitura.EquipamentoId,
        leitura.Temperatura,
        leitura.Umidade,
        leitura.PortaAberta,
        leitura.RegistradaEm,
        leitura.Origem);
}

/// <summary>
/// Resposta do POST /api/leituras: a leitura gravada mais os alertas que ela abriu. Devolver os
/// alertas aqui e o que permite o sensor e a demo verem o alarme no mesmo request.
/// </summary>
public sealed record LeituraRegistradaResponse(
    LeituraResponse Leitura,
    IReadOnlyList<AlertaResponse> AlertasAbertos);
