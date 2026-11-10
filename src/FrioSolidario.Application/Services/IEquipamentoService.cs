using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>Regras de negocio dos equipamentos monitorados e da faixa aceita para cada um.</summary>
public interface IEquipamentoService
{
    Task<IReadOnlyList<EquipamentoResponse>> ListarAsync(
        long? instituicaoId = null,
        CancellationToken cancellationToken = default);

    Task<EquipamentoResponse> ObterAsync(long id, CancellationToken cancellationToken = default);

    Task<EquipamentoResponse> CriarAsync(EquipamentoRequest request, CancellationToken cancellationToken = default);

    Task<EquipamentoResponse> AtualizarAsync(long id, EquipamentoRequest request, CancellationToken cancellationToken = default);

    Task RemoverAsync(long id, CancellationToken cancellationToken = default);
}
