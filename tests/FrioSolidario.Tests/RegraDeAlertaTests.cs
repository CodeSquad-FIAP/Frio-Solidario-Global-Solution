using FrioSolidario.Application.Services;
using FrioSolidario.Domain;

namespace FrioSolidario.Tests;

/// <summary>
/// Casos 5 e 6 do plano de testes (docs/plano-de-testes.md) sobre <see cref="RegraDeAlerta"/>, a
/// regra do produto isolada: sem banco, sem HTTP e sem relogio. O que o plano executa a mao pela
/// API, aqui roda em milissegundos e em todo build. Use <c>--filter Caso=5</c> ou <c>Caso=6</c>
/// para rodar so um deles.
/// </summary>
public class RegraDeAlertaTests
{
    // ======================================================================================
    // Caso 5 - menos de 3 leituras fora da faixa NAO abrem alerta
    // ======================================================================================

    /// <summary>
    /// Porta abrindo por um minuto derruba a leitura de um sensor so. Se uma ou duas leituras
    /// bastassem, a fila do voluntario encheria de alarme falso; por isso a regra exige 3.
    /// </summary>
    [Theory]
    [Trait("Caso", "5")]
    [InlineData(-16.0)]
    [InlineData(-16.0, -16.0)]
    public void Caso5_UmaOuDuasLeiturasAcimaDoMaximo_NaoAbremAlerta(params double[] temperaturas)
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), Decimais(temperaturas));

        Assert.Empty(propostos);
    }

    /// <summary>Espelho do anterior para o limite inferior: 1 ou 2 leituras abaixo de temp_min.</summary>
    [Theory]
    [Trait("Caso", "5")]
    [InlineData(-27.0)]
    [InlineData(-27.0, -27.0)]
    public void Caso5_UmaOuDuasLeiturasAbaixoDoMinimo_NaoAbremAlerta(params double[] temperaturas)
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), Decimais(temperaturas));

        Assert.Empty(propostos);
    }

    /// <summary>
    /// A sequencia exata do caso 5 do plano: -16, -16, -20, -18, -18, -18. Cada leitura e avaliada
    /// com as anteriores, como a API faz a cada POST, e nenhuma pode abrir alerta. A leitura de -20
    /// (dentro da faixa) zera a contagem, e -18 e exatamente o limite, que nao conta como "acima".
    /// </summary>
    [Fact]
    [Trait("Caso", "5")]
    public void Caso5_SequenciaDoPlano_NenhumaLeituraAbreAlerta()
    {
        var equipamento = Sequencias.FreezerDoSeed();
        decimal[] sequencia = [-16.0m, -16.0m, -20.0m, -18.0m, -18.0m, -18.0m];

        for (var tamanho = 1; tamanho <= sequencia.Length; tamanho++)
        {
            var propostos = Sequencias.AvaliarUltima(equipamento, sequencia[..tamanho]);

            Assert.True(propostos.Count == 0, $"A leitura {tamanho} ({sequencia[tamanho - 1]} C) abriu alerta.");
        }
    }

    /// <summary>Uma leitura dentro da faixa no meio da sequencia a interrompe: -16, -16, -20, -16, -16 nao abre alerta.</summary>
    [Fact]
    [Trait("Caso", "5")]
    public void Caso5_LeituraDentroDaFaixaNoMeio_InterrompeASequencia()
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), -16.0m, -16.0m, -20.0m, -16.0m, -16.0m);

        Assert.Empty(propostos);
    }

    /// <summary>
    /// Os limites da faixa sao "dentro": temp_max (-18) e temp_min (-25) nao contam como fora, mesmo
    /// com 3 leituras seguidas. "Acima do maximo" e estritamente maior, e "abaixo do minimo", menor.
    /// </summary>
    [Theory]
    [Trait("Caso", "5")]
    [InlineData(-18.0)]
    [InlineData(-25.0)]
    public void Caso5_TresLeiturasExatamenteNoLimiteDaFaixa_NaoAbremAlerta(double limite)
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), (decimal)limite, (decimal)limite, (decimal)limite);

        Assert.Empty(propostos);
    }

    /// <summary>Equipamento funcionando normal (leituras dentro da faixa) nunca gera alerta.</summary>
    [Fact]
    [Trait("Caso", "5")]
    public void Caso5_LeiturasDentroDaFaixa_NaoAbremAlerta()
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), -20.2m, -20.0m, -20.5m, -21.0m, -20.1m);

        Assert.Empty(propostos);
    }

    // ======================================================================================
    // Caso 6 - 3 leituras seguidas fora da faixa abrem 1 alerta, com severidade por desvio
    // ======================================================================================

    /// <summary>
    /// O passo 3 do caso 6: tres leituras a -16 C num equipamento com temp_max -18 abrem
    /// <c>TEMP_ALTA</c>, e o desvio de 2,0 C da severidade <c>ALTA</c>.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public void Caso6_TresLeiturasAcimaDoMaximo_AbremTempAltaComSeveridadeAlta()
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), -16.0m, -16.0m, -16.0m);

        var alerta = Assert.Single(propostos);
        Assert.Equal(TipoAlerta.TEMP_ALTA, alerta.Tipo);
        Assert.Equal(SeveridadeAlerta.ALTA, alerta.Severidade);
    }

    /// <summary>
    /// A severidade cresce com o desvio da ultima leitura em relacao a temp_max (-18): ate 1,5 C
    /// de desvio e MEDIA, de 1,5 a 3 C e ALTA, a partir de 3 C e CRITICA. Os limites 1,5 e 3,0
    /// pertencem a faixa de cima (>=), por isso -16,5 ja e ALTA e -15,0 ja e CRITICA.
    /// </summary>
    [Theory]
    [Trait("Caso", "6")]
    [InlineData(-17.5, SeveridadeAlerta.MEDIA)]   // desvio 0,5
    [InlineData(-17.0, SeveridadeAlerta.MEDIA)]   // desvio 1,0 (plano)
    [InlineData(-16.6, SeveridadeAlerta.MEDIA)]   // desvio 1,4, logo abaixo do limite
    [InlineData(-16.5, SeveridadeAlerta.ALTA)]    // desvio 1,5 (limite, plano)
    [InlineData(-16.0, SeveridadeAlerta.ALTA)]    // desvio 2,0
    [InlineData(-15.1, SeveridadeAlerta.ALTA)]    // desvio 2,9, logo abaixo do limite
    [InlineData(-15.0, SeveridadeAlerta.CRITICA)] // desvio 3,0 (limite)
    [InlineData(-14.0, SeveridadeAlerta.CRITICA)] // desvio 4,0 (plano)
    public void Caso6_SeveridadeDeTempAltaCresceComODesvio(double temperatura, SeveridadeAlerta esperada)
    {
        var t = (decimal)temperatura;

        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), t, t, t);

        var alerta = Assert.Single(propostos);
        Assert.Equal(TipoAlerta.TEMP_ALTA, alerta.Tipo);
        Assert.Equal(esperada, alerta.Severidade);
    }

    /// <summary>Mesmo criterio abaixo de temp_min (-25): 1 C de desvio e MEDIA, 2 C e ALTA, 3 C e CRITICA.</summary>
    [Theory]
    [Trait("Caso", "6")]
    [InlineData(-26.0, SeveridadeAlerta.MEDIA)]   // desvio 1,0
    [InlineData(-27.0, SeveridadeAlerta.ALTA)]    // desvio 2,0 (como o alerta 2 do seed)
    [InlineData(-28.0, SeveridadeAlerta.CRITICA)] // desvio 3,0
    public void Caso6_TresLeiturasAbaixoDoMinimo_AbremTempBaixaComSeveridadePorDesvio(double temperatura, SeveridadeAlerta esperada)
    {
        var t = (decimal)temperatura;

        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), t, t, t);

        var alerta = Assert.Single(propostos);
        Assert.Equal(TipoAlerta.TEMP_BAIXA, alerta.Tipo);
        Assert.Equal(esperada, alerta.Severidade);
    }

    /// <summary>
    /// A severidade olha a ultima leitura, nao a primeira da sequencia: -16, -16 e depois -14 da
    /// desvio 4,0 e vira CRITICA, mesmo que as duas primeiras valessem so ALTA.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public void Caso6_SeveridadeUsaODesvioDaUltimaLeitura()
    {
        var propostos = Sequencias.AvaliarUltima(Sequencias.FreezerDoSeed(), -16.0m, -16.0m, -14.0m);

        var alerta = Assert.Single(propostos);
        Assert.Equal(SeveridadeAlerta.CRITICA, alerta.Severidade);
    }

    /// <summary>
    /// A 3a leitura fora da faixa e a que abre o alerta, mas a regra e "3 seguidas": depois de uma
    /// leitura dentro da faixa, sao necessarias mais 3 (o caso 6 parte desse estado, deixado pelo caso 5).
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public void Caso6_AposLeituraDentroDaFaixa_ExigeTresNovasLeiturasForaDaFaixa()
    {
        var equipamento = Sequencias.FreezerDoSeed();

        Assert.Empty(Sequencias.AvaliarUltima(equipamento, -18.0m, -16.0m));
        Assert.Empty(Sequencias.AvaliarUltima(equipamento, -18.0m, -16.0m, -16.0m));
        Assert.Single(Sequencias.AvaliarUltima(equipamento, -18.0m, -16.0m, -16.0m, -16.0m));
    }

    /// <summary>
    /// A regra compara com a faixa do equipamento, nunca com um valor fixo: as mesmas tres leituras
    /// a -16 C abrem alerta no freezer (max -18) e nao abrem numa camara de faixa mais folgada
    /// (max -10). Mudar a faixa no cadastro muda o comportamento sem tocar em codigo.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public void Caso6_AMesmaSequencia_DependeDaFaixaDoEquipamento()
    {
        var freezer = Sequencias.FreezerDoSeed();
        var camaraFolgada = Sequencias.FreezerDoSeed();
        camaraFolgada.TempMax = -10m;

        Assert.Single(Sequencias.AvaliarUltima(freezer, -16.0m, -16.0m, -16.0m));
        Assert.Empty(Sequencias.AvaliarUltima(camaraFolgada, -16.0m, -16.0m, -16.0m));
    }

    /// <summary>
    /// A API le as ultimas leituras do banco da mais nova para a mais antiga; a regra nao pode
    /// depender dessa ordem de entrada, nem se confundir com uma leitura de data futura.
    /// </summary>
    [Fact]
    [Trait("Caso", "6")]
    public void Caso6_NaoDependeDaOrdemNemConsideraLeituraPosterior()
    {
        var equipamento = Sequencias.FreezerDoSeed();
        var inicio = Sequencias.Inicio;
        var nova = Sequencias.Leitura(3, -16.0m, inicio + Sequencias.Intervalo * 2);
        var anteriores = new[]
        {
            Sequencias.Leitura(2, -16.0m, inicio + Sequencias.Intervalo),
            Sequencias.Leitura(9, -20.0m, inicio + Sequencias.Intervalo * 5), // chegou depois da nova: nao conta
            Sequencias.Leitura(1, -16.0m, inicio)
        };

        var propostos = RegraDeAlerta.Avaliar(equipamento, nova, anteriores);

        Assert.Equal(TipoAlerta.TEMP_ALTA, Assert.Single(propostos).Tipo);
    }

    private static decimal[] Decimais(double[] valores) => valores.Select(v => (decimal)v).ToArray();
}
