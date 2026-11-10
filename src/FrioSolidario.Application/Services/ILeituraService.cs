using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Ingestao das leituras e a serie que o grafico do app consome. O POST e o unico ponto por onde a
/// regra de alerta entra em acao: toda leitura gravada passa por <see cref="RegraDeAlerta"/>.
/// </summary>
public interface ILeituraService
{
    Task<LeituraRegistradaResponse> RegistrarAsync(
        LeituraRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<LeituraResponse>> ListarAsync(
        long equipamentoId,
        string? periodo = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);
}
