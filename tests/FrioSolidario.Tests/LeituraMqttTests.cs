using System.Globalization;
using FrioSolidario.Domain;
using FrioSolidario.Infrastructure.Mqtt;

namespace FrioSolidario.Tests;

/// <summary>
/// A ponte MQTT sem broker: topico e payload entram, <c>LeituraRequest</c> ou motivo da recusa sai.
/// Cobre o contrato combinado com o Roger e o que precisa ser recusado antes de chegar ao banco —
/// mensagem de sensor errado nao pode virar leitura nem derrubar a assinatura do topico.
/// </summary>
public class LeituraMqttTests
{
    private const string Prefixo = "frio/equipamentos";

    [Fact]
    public void TopicoDe_SegueOPadraoCombinado()
    {
        Assert.Equal("frio/equipamentos/7/leitura", LeituraMqttMensagem.TopicoDe(7, Prefixo));
        Assert.Equal("frio/equipamentos/7/leitura", LeituraMqttMensagem.TopicoDe(7, "frio/equipamentos/"));
    }

    [Fact]
    public void PayloadCompleto_ViraLeituraDoSensor()
    {
        const string payload = """
            {"temperatura": 7.8, "umidade": 55, "portaAberta": true,
             "registradaEm": "2026-11-12T00:10:00-03:00"}
            """;

        var ok = LeituraMqttMensagem.TentarLer(
            "frio/equipamentos/4/leitura", payload, Prefixo, out var leitura, out var motivo);

        Assert.True(ok, motivo);
        Assert.NotNull(leitura);
        Assert.Equal(4, leitura!.EquipamentoId);
        Assert.Equal(7.8m, leitura.Temperatura);
        Assert.Equal(55m, leitura.Umidade);
        Assert.True(leitura.PortaAberta);
        Assert.Equal(OrigemLeitura.SENSOR, leitura.Origem);
        Assert.Equal(
            new DateTimeOffset(2026, 11, 12, 0, 10, 0, TimeSpan.FromHours(-3)),
            leitura.RegistradaEm);
    }

    /// <summary>
    /// O simulador do Roger declara <c>"origem": "SIMULADO"</c> no payload. Se a ponte ignorasse isso,
    /// o banco marcaria toda leitura de ensaio como se fosse do sensor fisico e a demo perderia a
    /// distincao que o edital pede (prototipo vs. dado real).
    /// </summary>
    [Fact]
    public void OrigemDeclaradaNoPayload_EhRespeitada()
    {
        const string payload = """
            {"temperatura": 7.8, "umidade": 55, "portaAberta": false, "origem": "SIMULADO"}
            """;

        var ok = LeituraMqttMensagem.TentarLer(
            "frio/equipamentos/4/leitura", payload, Prefixo, out var leitura, out var motivo);

        Assert.True(ok, motivo);
        Assert.Equal(OrigemLeitura.SIMULADO, leitura!.Origem);
    }

    /// <summary>
    /// Script de sensor em Python costuma sair em snake_case e com numero em texto. Recusar por isso
    /// seria recusar o proprio Roger na demo.
    /// </summary>
    [Fact]
    public void PayloadEmSnakeCaseComNumeroEmTexto_TambemVale()
    {
        const string payload = """{"temperatura": "-18.5", "umidade": "62", "porta_aberta": "false"}""";

        var ok = LeituraMqttMensagem.TentarLer(
            "frio/equipamentos/2/leitura", payload, Prefixo, out var leitura, out var motivo);

        Assert.True(ok, motivo);
        Assert.Equal(-18.5m, leitura!.Temperatura);
        Assert.Equal(62m, leitura.Umidade);
        Assert.False(leitura.PortaAberta);
        Assert.Null(leitura.RegistradaEm);
    }

    /// <summary>Virgula decimal em texto tambem passa: sensor brasileiro publica "-18,5".</summary>
    [Fact]
    public void NumeroEmTextoComVirgula_TambemVale()
    {
        const string payload = """{"temperatura": "-18,5", "umidade": "62,5"}""";

        var ok = LeituraMqttMensagem.TentarLer(
            "frio/equipamentos/2/leitura", payload, Prefixo, out var leitura, out var motivo);

        Assert.True(ok, motivo);
        Assert.Equal(-18.5m, leitura!.Temperatura);
        Assert.Equal(62.5m, leitura.Umidade);
    }

    [Fact]
    public void SemPortaNemData_AssumeFechadaEAgoraDoServidor()
    {
        var ok = LeituraMqttMensagem.TentarLer(
            "frio/equipamentos/1/leitura",
            """{"temperatura": 3.1, "umidade": 48}""",
            Prefixo,
            out var leitura,
            out var motivo);

        Assert.True(ok, motivo);
        Assert.False(leitura!.PortaAberta);
        Assert.Null(leitura.RegistradaEm);
    }

    [Theory]
    [InlineData("""{"temperatura": 100, "umidade": 50}""", "faixa")]
    [InlineData("""{"temperatura": -41, "umidade": 50}""", "faixa")]
    [InlineData("""{"temperatura": 5, "umidade": 101}""", "faixa")]
    [InlineData("""{"temperatura": 5}""", "umidade")]
    [InlineData("""{"umidade": 50}""", "temperatura")]
    [InlineData("""{"temperatura": 5, "umidade": 50, "registradaEm": "ontem"}""", "data")]
    [InlineData("""{"temperatura": 5, "umidade": 50, "equipamentoId": 9}""", "equipamento")]
    [InlineData("""{"temperatura": 5, "umidade": 50, "origem": "CHUTE"}""", "origem")]
    [InlineData("""{"temperatura": 5, "umidade": """, "JSON")]
    [InlineData("", "vazio")]
    public void MensagemInvalida_EhRecusadaComMotivo(string payload, string trechoDoMotivo)
    {
        var ok = LeituraMqttMensagem.TentarLer(
            "frio/equipamentos/1/leitura", payload, Prefixo, out var leitura, out var motivo);

        Assert.False(ok);
        Assert.Null(leitura);
        Assert.Contains(trechoDoMotivo, motivo, StringComparison.OrdinalIgnoreCase);
    }

    [Theory]
    [InlineData("casa/equipamentos/1/leitura")]
    [InlineData("frio/equipamentos/leitura")]
    [InlineData("frio/equipamentos/abc/leitura")]
    [InlineData("frio/equipamentos/0/leitura")]
    [InlineData("frio/equipamentos/1/temperatura")]
    [InlineData("frio/1/leitura")]
    public void TopicoForaDoPadrao_EhRecusado(string topico)
    {
        var ok = LeituraMqttMensagem.TentarLer(
            topico, """{"temperatura": 5, "umidade": 50}""", Prefixo, out var leitura, out var motivo);

        Assert.False(ok);
        Assert.Null(leitura);
        Assert.False(string.IsNullOrWhiteSpace(motivo));
    }
}
