namespace FrioSolidario.Application.Repositories;

/// <summary>
/// Porta de saida da camada de servico. A implementacao vive na Infrastructure,
/// entao a Application nao conhece banco, provider nem SQL.
/// </summary>
public interface IHealthRepository
{
    /// <summary>
    /// Quantidade de equipamentos cadastrados no schema do banco, ou <c>null</c> quando o
    /// banco nao responde (fora do ar ou schema nao carregado).
    /// </summary>
    Task<int?> ContarEquipamentosAsync(CancellationToken cancellationToken = default);
}
