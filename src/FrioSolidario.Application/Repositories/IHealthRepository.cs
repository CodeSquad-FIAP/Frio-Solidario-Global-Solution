namespace FrioSolidario.Application.Repositories;

/// <summary>
/// Porta de saida da camada de servico. A implementacao vive na Infrastructure,
/// entao a Application nao conhece banco, provider nem SQL.
/// </summary>
public interface IHealthRepository
{
    /// <summary>
    /// Quantidade de equipamentos cadastrados, ou <c>null</c> enquanto o schema
    /// (dono: Andre) nao estiver conectado a API.
    /// </summary>
    Task<int?> ContarEquipamentosAsync(CancellationToken cancellationToken = default);
}
