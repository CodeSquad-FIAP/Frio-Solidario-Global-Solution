using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Tests;

/// <summary>
/// Monta equipamentos e sequencias de leituras para os testes da regra de alerta. Os valores
/// repetem os do plano de testes (docs/plano-de-testes.md): o equipamento e o "Freezer de
/// congelados" do seed (faixa -25 a -18 C, umidade maxima 85%), e as leituras chegam de 2 em
/// 2 minutos, que e o intervalo dos sensores.
/// </summary>
internal static class Sequencias
{
    public static readonly TimeSpan Intervalo = TimeSpan.FromMinutes(2);

    /// <summary>
    /// Instante fixo: o teste nao pode depender do relogio da maquina. Fica no passado de proposito,
    /// porque o servico recusa leitura com data futura (alem de 5 minutos de tolerancia).
    /// </summary>
    public static readonly DateTimeOffset Inicio = new(2026, 1, 15, 10, 0, 0, TimeSpan.Zero);

    /// <summary>Freezer de congelados do seed: temp_min -25, temp_max -18, umid_max 85.</summary>
    public static Equipamento FreezerDoSeed() => new()
    {
        Id = 4,
        InstituicaoId = 2,
        Nome = "Freezer de congelados",
        Tipo = TipoEquipamento.FREEZER,
        TempMin = -25m,
        TempMax = -18m,
        UmidMax = 85m,
        Ativo = true
    };

    /// <summary>Uma leitura de sensor com umidade dentro do limite e porta fechada.</summary>
    public static Leitura Leitura(long id, decimal temperatura, DateTimeOffset registradaEm, decimal umidade = 75m, bool portaAberta = false) => new()
    {
        Id = id,
        EquipamentoId = 4,
        Temperatura = temperatura,
        Umidade = umidade,
        PortaAberta = portaAberta,
        RegistradaEm = registradaEm,
        Origem = OrigemLeitura.SIMULADO
    };

    /// <summary>
    /// Leituras com as temperaturas dadas, em ordem de chegada (id crescente, 2 min entre uma e
    /// outra). A ultima e a "nova"; as anteriores ficam em <c>anteriores</c>, como a regra recebe.
    /// </summary>
    public static (Leitura Nova, IReadOnlyList<Leitura> Anteriores) Chegada(params decimal[] temperaturas)
    {
        var todas = temperaturas
            .Select((t, i) => Leitura(i + 1, t, Inicio + Intervalo * i))
            .ToList();

        return (todas[^1], todas.Take(todas.Count - 1).ToList());
    }

    /// <summary>Resultado da regra para a ultima leitura de uma sequencia que chegou na ordem dada.</summary>
    public static IReadOnlyList<Application.Services.AlertaProposto> AvaliarUltima(Equipamento equipamento, params decimal[] temperaturas)
    {
        var (nova, anteriores) = Chegada(temperaturas);
        return Application.Services.RegraDeAlerta.Avaliar(equipamento, nova, anteriores);
    }
}
