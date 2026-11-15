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

    /// <summary>
    /// A leitura mais recente do equipamento, seja de quando for, ou <c>null</c> se ele nunca enviou
    /// nenhuma. Existe para o painel do app, que so mostra o ultimo valor: sem ela o app baixava a
    /// serie inteira de 24h de cada equipamento a cada atualizacao.
    /// </summary>
    Task<LeituraResponse?> UltimaAsync(
        long equipamentoId,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);
}
