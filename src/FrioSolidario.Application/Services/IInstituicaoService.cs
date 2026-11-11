using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Regras de negocio das instituicoes (bancos de alimentos, mercados e padarias). Alterar e remover
/// recebem a instituicao do token: o ADMIN so mexe na propria instituicao.
/// </summary>
public interface IInstituicaoService
{
    Task<IReadOnlyList<InstituicaoResponse>> ListarAsync(CancellationToken cancellationToken = default);

    Task<InstituicaoResponse> ObterAsync(long id, CancellationToken cancellationToken = default);

    Task<InstituicaoResponse> CriarAsync(InstituicaoRequest request, CancellationToken cancellationToken = default);

    Task<InstituicaoResponse> AtualizarAsync(
        long id,
        InstituicaoRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default);

    Task RemoverAsync(long id, long? instituicaoDoToken = null, CancellationToken cancellationToken = default);
}
