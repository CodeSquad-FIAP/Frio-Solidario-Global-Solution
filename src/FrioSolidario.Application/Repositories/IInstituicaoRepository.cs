using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para as instituicoes. Implementacao com EF Core na Infrastructure.</summary>
public interface IInstituicaoRepository
{
    Task<IReadOnlyList<Instituicao>> ListarAsync(CancellationToken cancellationToken = default);

    Task<Instituicao?> ObterAsync(long id, CancellationToken cancellationToken = default);

    Task<bool> ExisteCnpjAsync(string cnpj, long? ignorarId = null, CancellationToken cancellationToken = default);

    Task<Instituicao> AdicionarAsync(Instituicao instituicao, CancellationToken cancellationToken = default);

    Task AtualizarAsync(Instituicao instituicao, CancellationToken cancellationToken = default);

    Task RemoverAsync(Instituicao instituicao, CancellationToken cancellationToken = default);
}
