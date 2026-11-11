using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Regras de negocio dos equipamentos monitorados e da faixa aceita para cada um. Toda operacao
/// recebe a instituicao do token: o usuario so ve e altera equipamento da propria instituicao.
/// </summary>
public interface IEquipamentoService
{
    Task<IReadOnlyList<EquipamentoResponse>> ListarAsync(
        long? instituicaoId = null,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task<EquipamentoResponse> ObterAsync(
        long id,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task<EquipamentoResponse> CriarAsync(
        EquipamentoRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task<EquipamentoResponse> AtualizarAsync(
        long id,
        EquipamentoRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task RemoverAsync(long id, long? instituicaoDoToken = null, CancellationToken cancellationToken = default);
}
