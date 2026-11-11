using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Services;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Tests;

/// <summary>
/// Casos 5 e 6 do plano de testes no nivel do servico: o mesmo caminho do
/// <c>POST /api/leituras</c> (gravar a leitura, aplicar a regra, abrir o alerta), com repositorios
/// em memoria. Cobrem o que a regra pura nao decide: gravar o alerta uma vez so (um alerta aberto
/// por tipo e equipamento) e devolver, na resposta, o alerta que a leitura abriu.
/// </summary>
public class LeituraServiceTests
{
    private readonly LeiturasEmMemoria _leituras = new();
    private readonly AlertasEmMemoria _alertas = new();
    private readonly LeituraService _servico;

    public LeituraServiceTests()
    {
        var equipamentos = new EquipamentosEmMemoria(Sequencias.FreezerDoSeed());
        _servico = new LeituraService(_leituras, _alertas, equipamentos);
    }

    /// <summary>
    /// Leitura do equipamento 4 com horario fixo, no passado e de 2 em 2 minutos: o servico recusa
    /// leitura no futuro, e a ordem das leituras nao pode depender do relogio da maquina de teste.
    /// </summary>
    private Task<LeituraRegistradaResponse> Enviar(int indice, decimal temperatura)
        => _servico.RegistrarAsync(new LeituraRequest(
            EquipamentoId: 4,
            Temperatura: temperatura,
            Umidade: 75m,
            PortaAberta: false,
            RegistradaEm: Sequencias.Inicio + Sequencias.Intervalo * indice,
            Origem: OrigemLeitura.SIMULADO));

    /// <summary>
    /// Caso 5 pelo servico, com a sequencia do plano (-16, -16, -20, -18, -18, -18): todas as
    /// leituras sao gravadas (201 na API), nenhuma devolve alerta e nenhum alerta chega ao repositorio.
    /// </summary>
    [Fact]
    [Trait("Caso", "5")]
    public async Task Caso5_SequenciaDoPlano_GravaTodasAsLeiturasSemAbrirAlerta()
    {
        decimal[] sequencia = [-16.0m, -16.0m, -20.0m, -18.0m, -18.0m, -18.0m];

        for (var i = 0; i < sequencia.Length; i++)
        {
            var resposta = await Enviar(i, sequencia[i]);

            Assert.Empty(resposta.AlertasAbertos);
        }

        Assert.Equal(sequencia.Length, _leituras.Itens.Count);
        Assert.Empty(_alertas.Itens);
    }

    /// <summary>
    /// Caso 6, passos 1 a 4: a 3a leitura fora da faixa abre <c>TEMP_ALTA</c>/<c>ALTA</c>/<c>ABERTO</c>
    /// e a resposta ja traz o alerta (e o que deixa o sensor e a demo verem o alarme na mesma
    /// chamada); a 4a leitura nao cria outro, porque ja ha um alerta aberto do mesmo tipo.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public async Task Caso6_TerceiraLeituraAbreUmAlerta_EAQuartaNaoDuplica()
    {
        Assert.Empty((await Enviar(0, -16.0m)).AlertasAbertos);
        Assert.Empty((await Enviar(1, -16.0m)).AlertasAbertos);

        var terceira = await Enviar(2, -16.0m);
        var alerta = Assert.Single(terceira.AlertasAbertos);
        Assert.Equal(TipoAlerta.TEMP_ALTA, alerta.Tipo);
        Assert.Equal(SeveridadeAlerta.ALTA, alerta.Severidade);
        Assert.Equal(StatusAlerta.ABERTO, alerta.Status);
        Assert.Equal(4, alerta.EquipamentoId);
        Assert.Equal(terceira.Leitura.Id, alerta.LeituraId); // o alerta aponta para a leitura que o abriu

        Assert.Empty((await Enviar(3, -16.0m)).AlertasAbertos);
        Assert.Empty((await Enviar(4, -14.0m)).AlertasAbertos); // mesmo mais grave, segue sem duplicar

        var gravado = Assert.Single(_alertas.Itens);
        Assert.Equal((TipoAlerta.TEMP_ALTA, SeveridadeAlerta.ALTA, StatusAlerta.ABERTO), (gravado.Tipo, gravado.Severidade, gravado.Status));
    }

    /// <summary>
    /// O passo 5 do caso 6: um equipamento que ja tem alerta aberto do tipo (como o equipamento 1
    /// do seed) recebe mais uma leitura fora da faixa e nao ganha alerta repetido.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public async Task Caso6_EquipamentoComAlertaAbertoDoMesmoTipo_NaoGanhaAlertaRepetido()
    {
        _alertas.Itens.Add(new Alerta
        {
            Id = 100,
            EquipamentoId = 4,
            LeituraId = 1,
            Tipo = TipoAlerta.TEMP_ALTA,
            Severidade = SeveridadeAlerta.CRITICA,
            Status = StatusAlerta.ABERTO,
            CriadoEm = Sequencias.Inicio
        });

        await Enviar(0, -16.0m);
        await Enviar(1, -16.0m);
        var terceira = await Enviar(2, -16.0m);

        Assert.Empty(terceira.AlertasAbertos);
        Assert.Single(_alertas.Itens);
    }

    /// <summary>
    /// Depois que o voluntario resolve o alerta, a temperatura que segue fora da faixa pode abrir
    /// um novo: "um aberto por vez" nao silencia o equipamento para sempre.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public async Task Caso6_AposResolverOAlerta_UmaNovaSequenciaAbreOutro()
    {
        await Enviar(0, -16.0m);
        await Enviar(1, -16.0m);
        Assert.Single((await Enviar(2, -16.0m)).AlertasAbertos);

        _alertas.Itens[0].Status = StatusAlerta.RESOLVIDO;

        var proxima = await Enviar(3, -16.0m);

        Assert.Equal(TipoAlerta.TEMP_ALTA, Assert.Single(proxima.AlertasAbertos).Tipo);
        Assert.Equal(2, _alertas.Itens.Count);
    }
}
