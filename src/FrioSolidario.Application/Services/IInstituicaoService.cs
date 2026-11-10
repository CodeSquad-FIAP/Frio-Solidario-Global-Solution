using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>Regras de negocio das instituicoes (bancos de alimentos, mercados e padarias).</summary>
public interface IInstituicaoService
{
    Task<IReadOnlyList<InstituicaoResponse>> ListarAsync(CancellationToken cancellationToken = default);

    Task<InstituicaoResponse> ObterAsync(long id, CancellationToken cancellationToken = default);

    Task<InstituicaoResponse> CriarAsync(InstituicaoRequest request, CancellationToken cancellationToken = default);

    Task<InstituicaoResponse> AtualizarAsync(long id, InstituicaoRequest request, CancellationToken cancellationToken = default);

    Task RemoverAsync(long id, CancellationToken cancellationToken = default);
}
