namespace FrioSolidario.Application.DTOs;

/// <summary>Resposta do health check: prova que a API está no ar e em qual camada ela está respondendo.</summary>
public sealed record HealthResponse(
    string Servico,
    string Status,
    string Versao,
    string Ambiente,
    DateTimeOffset VerificadoEm,
    string OrigemDosDados);
