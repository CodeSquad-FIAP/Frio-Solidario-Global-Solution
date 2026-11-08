using FrioSolidario.Application.Repositories;

namespace FrioSolidario.Infrastructure.Repositories;

/// <summary>
/// Implementacao provisoria: devolve <c>null</c> porque o banco ainda nao esta conectado.
/// Quando o schema do Andre estiver fechado, este arquivo passa a usar o DbContext real
/// (EF Core) ou Dapper com consulta parametrizada -- nenhum outro ponto do codigo muda.
/// </summary>
public sealed class HealthRepository : IHealthRepository
{
    public Task<int?> ContarEquipamentosAsync(CancellationToken cancellationToken = default)
        => Task.FromResult<int?>(null);
}
