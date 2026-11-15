using System.Globalization;
using System.Text.Json;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Domain;

namespace FrioSolidario.Infrastructure.Mqtt;

/// <summary>
/// Traduz uma mensagem MQTT em <see cref="LeituraRequest"/>. Fica separado da conexao para poder ser
/// testado sem broker: topico e payload entram, leitura ou motivo da recusa sai.
///
/// Contrato combinado com o Roger (o script dele publica assim):
///   topico  frio/equipamentos/{idEquipamento}/leitura
///   payload {"temperatura": 7.8, "umidade": 55, "portaAberta": false, "registradaEm": "2026-11-12T00:10:00-03:00"}
/// `portaAberta` e `registradaEm` sao opcionais (sem data, o servidor usa a hora do recebimento).
/// Aceita tambem snake_case (`porta_aberta`, `registrada_em`), porque script de sensor em Python
/// costuma sair assim, e numero em texto ("7.8"), porque nem toda biblioteca serializa decimal.
/// </summary>
public static class LeituraMqttMensagem
{
    private static readonly JsonDocumentOptions OpcoesDeLeitura = new()
    {
        AllowTrailingCommas = true,
        CommentHandling = JsonCommentHandling.Skip
    };

    /// <summary>Topico que o sensor do Roger deve usar para o equipamento informado.</summary>
    public static string TopicoDe(long equipamentoId, string prefixoTopico) =>
        $"{prefixoTopico.Trim('/')}/{equipamentoId}/leitura";

    /// <summary>
    /// Le topico e payload. Devolve falso com o motivo em <paramref name="motivo"/> quando a mensagem
    /// nao e uma leitura valida — o chamador so registra no log e segue, sem derrubar a ponte.
    /// </summary>
    public static bool TentarLer(
        string topico,
        string payload,
        string prefixoTopico,
        out LeituraRequest? leitura,
        out string motivo)
    {
        leitura = null;

        if (!TentarLerEquipamentoDoTopico(topico, prefixoTopico, out var equipamentoId, out motivo))
        {
            return false;
        }

        if (string.IsNullOrWhiteSpace(payload))
        {
            motivo = "payload vazio";
            return false;
        }

        JsonDocument documento;
        try
        {
            documento = JsonDocument.Parse(payload, OpcoesDeLeitura);
        }
        catch (JsonException ex)
        {
            motivo = $"payload nao e JSON valido ({ex.Message})";
            return false;
        }

        using (documento)
        {
            var raiz = documento.RootElement;
            if (raiz.ValueKind != JsonValueKind.Object)
            {
                motivo = $"payload deveria ser um objeto JSON, veio {raiz.ValueKind}";
                return false;
            }

            var temperatura = LerNumero(raiz, "temperatura", "temperaturaC", "temperatura_c", "temp");
            if (temperatura is null)
            {
                motivo = "payload sem 'temperatura' numerica";
                return false;
            }

            var umidade = LerNumero(raiz, "umidade", "umidadeRelativa", "umidade_relativa");
            if (umidade is null)
            {
                motivo = "payload sem 'umidade' numerica";
                return false;
            }

            // Mesmos limites dos data annotations do LeituraRequest e dos CHECK do schema: recusar
            // aqui evita gravar lixo de sensor descalibrado e deixa o log com o valor recebido.
            if (temperatura is < -40m or > 80m)
            {
                motivo = $"temperatura {temperatura} fora da faixa fisica (-40 a 80)";
                return false;
            }

            if (umidade is < 0m or > 100m)
            {
                motivo = $"umidade {umidade} fora da faixa fisica (0 a 100)";
                return false;
            }

            var portaAberta = LerBooleano(raiz, "portaAberta", "porta_aberta", "porta") ?? false;

            DateTimeOffset? registradaEm = null;
            var textoData = LerTexto(raiz, "registradaEm", "registrada_em", "timestamp", "dataHora");
            if (textoData is not null)
            {
                if (!DateTimeOffset.TryParse(
                        textoData,
                        CultureInfo.InvariantCulture,
                        DateTimeStyles.RoundtripKind,
                        out var data))
                {
                    motivo = $"registradaEm '{textoData}' nao e uma data ISO 8601";
                    return false;
                }

                registradaEm = data;
            }

            var idNoPayload = LerNumero(raiz, "equipamentoId", "equipamento_id");
            if (idNoPayload is not null && (long)idNoPayload.Value != equipamentoId)
            {
                motivo = $"payload diz equipamento {idNoPayload} mas o topico diz {equipamentoId}; " +
                         "a ponte usa o topico para nao gravar leitura no equipamento errado";
                return false;
            }

            // Origem vem do payload quando o sensor declara (o simulador do Roger manda "SIMULADO");
            // sem declaracao, o dado so pode ter vindo do sensor fisico.
            var origem = OrigemLeitura.SENSOR;
            var textoOrigem = LerTexto(raiz, "origem", "origemLeitura");
            if (textoOrigem is not null)
            {
                if (!Enum.TryParse<OrigemLeitura>(textoOrigem, ignoreCase: true, out var origemLida))
                {
                    motivo = $"origem '{textoOrigem}' nao e SENSOR nem SIMULADO";
                    return false;
                }

                origem = origemLida;
            }

            leitura = new LeituraRequest(
                equipamentoId,
                temperatura.Value,
                umidade.Value,
                portaAberta,
                registradaEm,
                origem);

            motivo = string.Empty;
            return true;
        }
    }

    private static bool TentarLerEquipamentoDoTopico(
        string topico,
        string prefixoTopico,
        out long equipamentoId,
        out string motivo)
    {
        equipamentoId = 0;

        var partes = topico.Split('/', StringSplitOptions.RemoveEmptyEntries);
        var prefixo = prefixoTopico.Trim('/').Split('/', StringSplitOptions.RemoveEmptyEntries);

        // Esperado: {prefixo}/{id}/leitura — dois segmentos depois do prefixo.
        if (partes.Length != prefixo.Length + 2)
        {
            motivo = $"topico '{topico}' nao segue '{prefixoTopico}/{{id}}/leitura'";
            return false;
        }

        for (var i = 0; i < prefixo.Length; i++)
        {
            if (!string.Equals(partes[i], prefixo[i], StringComparison.OrdinalIgnoreCase))
            {
                motivo = $"topico '{topico}' fora do prefixo '{prefixoTopico}'";
                return false;
            }
        }

        if (!string.Equals(partes[^1], "leitura", StringComparison.OrdinalIgnoreCase))
        {
            motivo = $"topico '{topico}' deveria terminar em '/leitura'";
            return false;
        }

        if (!long.TryParse(partes[^2], NumberStyles.None, CultureInfo.InvariantCulture, out equipamentoId) ||
            equipamentoId <= 0)
        {
            motivo = $"topico '{topico}' sem id de equipamento valido";
            return false;
        }

        motivo = string.Empty;
        return true;
    }

    private static decimal? LerNumero(JsonElement raiz, params string[] nomes)
    {
        foreach (var nome in nomes)
        {
            if (!TentarObter(raiz, nome, out var valor))
            {
                continue;
            }

            if (valor.ValueKind == JsonValueKind.Number && valor.TryGetDecimal(out var numero))
            {
                return numero;
            }

            if (valor.ValueKind == JsonValueKind.String &&
                TentarDecimal(valor.GetString(), out numero))
            {
                return numero;
            }
        }

        return null;
    }

    /// <summary>
    /// Numero que veio como texto: tenta o formato ISO primeiro e, se falhar, a virgula decimal
    /// ("-18,5"), comum em script de sensor escrito no Brasil. Sem isso, uma leitura valida viraria
    /// "payload sem temperatura".
    /// </summary>
    private static bool TentarDecimal(string? texto, out decimal numero)
    {
        numero = 0;

        if (string.IsNullOrWhiteSpace(texto))
        {
            return false;
        }

        if (decimal.TryParse(texto, NumberStyles.Float, CultureInfo.InvariantCulture, out numero))
        {
            return true;
        }

        return texto.Contains(',', StringComparison.Ordinal) &&
               decimal.TryParse(
                   texto.Replace(",", ".", StringComparison.Ordinal),
                   NumberStyles.Float,
                   CultureInfo.InvariantCulture,
                   out numero);
    }

    private static bool? LerBooleano(JsonElement raiz, params string[] nomes)
    {
        foreach (var nome in nomes)
        {
            if (!TentarObter(raiz, nome, out var valor))
            {
                continue;
            }

            switch (valor.ValueKind)
            {
                case JsonValueKind.True:
                    return true;
                case JsonValueKind.False:
                    return false;
                case JsonValueKind.Number when valor.TryGetInt32(out var numero):
                    return numero != 0;
                case JsonValueKind.String when bool.TryParse(valor.GetString(), out var booleano):
                    return booleano;
            }
        }

        return null;
    }

    private static string? LerTexto(JsonElement raiz, params string[] nomes)
    {
        foreach (var nome in nomes)
        {
            if (TentarObter(raiz, nome, out var valor) && valor.ValueKind == JsonValueKind.String)
            {
                return valor.GetString();
            }
        }

        return null;
    }

    /// <summary>Busca a propriedade sem diferenciar maiuscula de minuscula (Temp vs temp).</summary>
    private static bool TentarObter(JsonElement raiz, string nome, out JsonElement valor)
    {
        foreach (var propriedade in raiz.EnumerateObject())
        {
            if (string.Equals(propriedade.Name, nome, StringComparison.OrdinalIgnoreCase))
            {
                valor = propriedade.Value;
                return true;
            }
        }

        valor = default;
        return false;
    }
}
