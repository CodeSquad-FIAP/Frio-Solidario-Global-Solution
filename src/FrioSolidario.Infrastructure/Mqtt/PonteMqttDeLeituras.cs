using FrioSolidario.Application.Common;
using FrioSolidario.Application.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace FrioSolidario.Infrastructure.Mqtt;

/// <summary>
/// Ponte MQTT -> API: assina `{PrefixoTopico}/+/leitura`, converte cada mensagem em
/// <see cref="Application.DTOs.LeituraRequest"/> e chama o <see cref="ILeituraService"/>, que e o
/// mesmo caminho do `POST /api/leituras`. Assim a leitura do sensor entra na regra de faixa e abre
/// alerta igual a uma leitura vinda do app — sem rota nova e sem duplicar a regra.
///
/// Duas escolhas que importam na demo:
/// - uma mensagem ruim (JSON torto, temperatura fora da faixa, equipamento inexistente) so gera log;
///   a assinatura continua de pe, senao o primeiro payload errado derrubaria a ponte;
/// - caiu a conexao, tenta de novo a cada <see cref="EsperaAntesDeReconectar"/>, porque o broker do
///   Roger pode subir depois da API na hora de apresentar.
/// </summary>
public sealed class PonteMqttDeLeituras : BackgroundService
{
    private static readonly TimeSpan EsperaAntesDeReconectar = TimeSpan.FromSeconds(5);

    private readonly OpcoesMqtt _opcoes;
    private readonly IServiceScopeFactory _escopos;
    private readonly ILogger<PonteMqttDeLeituras> _registro;

    public PonteMqttDeLeituras(
        IOptions<OpcoesMqtt> opcoes,
        IServiceScopeFactory escopos,
        ILogger<PonteMqttDeLeituras> registro)
    {
        _opcoes = opcoes.Value;
        _escopos = escopos;
        _registro = registro;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_opcoes.Habilitado)
        {
            _registro.LogInformation(
                "MQTT desligado (Mqtt:Habilitado=false): a API so aceita leitura por POST /api/leituras.");
            return;
        }

        var topico = $"{_opcoes.PrefixoTopico.Trim('/')}/+/leitura";
        var fabrica = new MqttFactory();

        using var cliente = fabrica.CreateMqttClient();
        cliente.ApplicationMessageReceivedAsync += ReceberAsync;

        TaskCompletionSource queda = new(TaskCreationOptions.RunContinuationsAsynchronously);
        cliente.DisconnectedAsync += argumentos =>
        {
            _registro.LogWarning(
                "MQTT: desconectado do broker {Host}:{Porta} ({Motivo}).",
                _opcoes.Host,
                _opcoes.Porta,
                argumentos.Exception?.Message ?? argumentos.Reason.ToString());

            queda.TrySetResult();
            return Task.CompletedTask;
        };

        while (!stoppingToken.IsCancellationRequested)
        {
            queda = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);

            try
            {
                await cliente.ConnectAsync(MontarOpcoesDeConexao(), stoppingToken);
                await cliente.SubscribeAsync(
                    new MqttClientSubscribeOptionsBuilder()
                        .WithTopicFilter(filtro => filtro
                            .WithTopic(topico)
                            // QoS 1: o broker reentrega se a API cair no meio da leitura. A regra de
                            // faixa conta leituras seguidas, entao perder uma por queda de rede
                            // mudaria o resultado do alarme.
                            .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce))
                        .Build(),
                    stoppingToken);

                _registro.LogInformation(
                    "MQTT: assinando {Topico} em {Host}:{Porta} como {ClienteId}.",
                    topico,
                    _opcoes.Host,
                    _opcoes.Porta,
                    _opcoes.ClienteId);

                using var parada = stoppingToken.Register(() => queda.TrySetResult());
                await queda.Task;
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _registro.LogWarning(
                    "MQTT: falha ao conectar em {Host}:{Porta} — {Erro}. Nova tentativa em {Espera}s.",
                    _opcoes.Host,
                    _opcoes.Porta,
                    ex.Message,
                    EsperaAntesDeReconectar.TotalSeconds);
            }

            if (cliente.IsConnected)
            {
                try
                {
                    await cliente.DisconnectAsync();
                }
                catch (Exception ex)
                {
                    _registro.LogDebug(ex, "MQTT: falha ao encerrar a conexao (o loop vai reconectar).");
                }
            }

            if (stoppingToken.IsCancellationRequested)
            {
                break;
            }

            try
            {
                await Task.Delay(EsperaAntesDeReconectar, stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }

        cliente.ApplicationMessageReceivedAsync -= ReceberAsync;
    }

    private MqttClientOptions MontarOpcoesDeConexao()
    {
        var construtor = new MqttClientOptionsBuilder()
            .WithTcpServer(_opcoes.Host, _opcoes.Porta)
            .WithClientId(_opcoes.ClienteId)
            .WithCleanSession()
            .WithTimeout(TimeSpan.FromSeconds(10));

        if (!string.IsNullOrWhiteSpace(_opcoes.Usuario))
        {
            construtor.WithCredentials(_opcoes.Usuario, _opcoes.Senha);
        }

        return construtor.Build();
    }

    private async Task ReceberAsync(MqttApplicationMessageReceivedEventArgs argumentos)
    {
        var topico = argumentos.ApplicationMessage.Topic;
        var payload = argumentos.ApplicationMessage.ConvertPayloadToString() ?? string.Empty;

        if (!LeituraMqttMensagem.TentarLer(topico, payload, _opcoes.PrefixoTopico, out var leitura, out var motivo) ||
            leitura is null)
        {
            _registro.LogWarning("MQTT: mensagem ignorada em {Topico} — {Motivo}.", topico, motivo);
            return;
        }

        // Escopo por mensagem: o ILeituraService e scoped (DbContext), o BackgroundService e singleton.
        using var escopo = _escopos.CreateScope();
        var leituras = escopo.ServiceProvider.GetRequiredService<ILeituraService>();

        try
        {
            var registrada = await leituras.RegistrarAsync(leitura);

            _registro.LogInformation(
                "MQTT: equipamento {EquipamentoId} {Temperatura}C / {Umidade}% porta {Porta} -> leitura {LeituraId}, " +
                "{Alertas} alerta(s) aberto(s).",
                leitura.EquipamentoId,
                leitura.Temperatura,
                leitura.Umidade,
                leitura.PortaAberta ? "aberta" : "fechada",
                registrada.Leitura.Id,
                registrada.AlertasAbertos.Count);
        }
        catch (Exception ex) when (ex is RegraDeNegocioException or AcessoNegadoException or KeyNotFoundException)
        {
            // Recusa prevista da regra (data no futuro, equipamento inexistente): o sensor nao tem
            // canal para receber erro, entao o log e a unica evidencia — e a ponte segue assinando.
            _registro.LogWarning(
                "MQTT: leitura do equipamento {EquipamentoId} recusada — {Motivo}.",
                leitura.EquipamentoId,
                ex.Message);
        }
        catch (Exception ex)
        {
            // Banco fora do ar, por exemplo: tambem nao pode derrubar a assinatura do topico.
            _registro.LogError(
                ex,
                "MQTT: falha ao gravar a leitura do equipamento {EquipamentoId}.",
                leitura.EquipamentoId);
        }
    }
}
