namespace FrioSolidario.Infrastructure.Mqtt;

/// <summary>
/// Secao "Mqtt" do appsettings. Desligada por padrao: sem <see cref="Habilitado"/> a API nao tenta
/// abrir conexao com broker nenhum, entao `dotnet run`, os testes e a demo so por HTTP seguem iguais.
/// </summary>
public sealed class OpcoesMqtt
{
    public const string Secao = "Mqtt";

    /// <summary>Liga a ponte. Sem isto, nenhum socket e aberto para o broker.</summary>
    public bool Habilitado { get; set; }

    public string Host { get; set; } = "localhost";

    public int Porta { get; set; } = 1883;

    /// <summary>
    /// Prefixo dos topicos. O sensor publica em `{PrefixoTopico}/{idEquipamento}/leitura`;
    /// a ponte assina `{PrefixoTopico}/+/leitura`, um filtro so para todos os equipamentos.
    /// </summary>
    public string PrefixoTopico { get; set; } = "frio/equipamentos";

    public string ClienteId { get; set; } = "frio-solidario-api";

    public string? Usuario { get; set; }

    public string? Senha { get; set; }
}
