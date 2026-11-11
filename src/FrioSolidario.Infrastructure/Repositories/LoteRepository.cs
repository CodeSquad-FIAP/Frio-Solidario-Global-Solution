using System.Data.Common;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

public sealed class LoteRepository(FrioSolidarioDbContext contexto) : ILoteRepository
{
    public async Task<IReadOnlyList<Lote>> ListarAsync(
        long? instituicaoId = null,
        StatusLote? status = null,
        CancellationToken cancellationToken = default)
        => await contexto.Lotes
            .AsNoTracking()
            .Where(l => instituicaoId == null || l.InstituicaoId == instituicaoId)
            .Where(l => status == null || l.Status == status)
            .OrderBy(l => l.Validade)
            .ThenBy(l => l.Id)
            .ToListAsync(cancellationToken);

    public async Task<Lote?> ObterAsync(long id, CancellationToken cancellationToken = default)
        => await contexto.Lotes.FirstOrDefaultAsync(l => l.Id == id, cancellationToken);

    public async Task<Lote> AdicionarAsync(Lote lote, CancellationToken cancellationToken = default)
    {
        contexto.Lotes.Add(lote);
        await SalvarAsync(cancellationToken);
        return lote;
    }

    public async Task AtualizarAsync(Lote lote, CancellationToken cancellationToken = default)
    {
        contexto.Lotes.Update(lote);
        await SalvarAsync(cancellationToken);
    }

    /// <summary>
    /// Traduz violacao de constraint em erro de negocio: instituto inexistente vira 409, kg fora do
    /// CHECK e retirada incoerente viram 400 com mensagem, em vez de 500.
    /// </summary>
    private async Task SalvarAsync(CancellationToken cancellationToken)
    {
        const string vinculo = "Instituicao do lote nao existe.";
        const string regra = "Lote viola uma regra do banco: kg precisa ser maior que zero e a retirada precisa de data.";

        try
        {
            await contexto.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException ex)
        {
            throw ConflitoDeBanco.Traduzir(ex, regra, vinculo, regra);
        }
        catch (DbException ex)
        {
            throw ConflitoDeBanco.Traduzir(ex, regra, vinculo, regra);
        }
    }
}
