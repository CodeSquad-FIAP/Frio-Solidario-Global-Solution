using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Lotes doados e o que saiu deles. E a fonte dos indicadores de impacto (kg doados nos ultimos 30
/// dias e taxa de perda por categoria), entao a retirada precisa ser registrada com data: lote que
/// sai sem data quebra a conta e o CHECK do banco.
/// </summary>
public interface ILoteService
{
    Task<IReadOnlyList<LoteResponse>> ListarAsync(
        long instituicaoDoToken,
        FrioSolidario.Domain.StatusLote? status = null,
        CancellationToken cancellationToken = default);

    Task<LoteResponse> ObterAsync(
        long id,
        long instituicaoDoToken,
        CancellationToken cancellationToken = default);

    Task<LoteResponse> CriarAsync(
        LoteRequest request,
        long instituicaoDoToken,
        CancellationToken cancellationToken = default);

    Task<LoteResponse> RegistrarRetiradaAsync(
        long id,
        RetiradaDeLoteRequest request,
        long instituicaoDoToken,
        CancellationToken cancellationToken = default);
}
