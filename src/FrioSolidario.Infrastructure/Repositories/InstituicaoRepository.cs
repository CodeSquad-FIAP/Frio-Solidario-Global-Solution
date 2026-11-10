using System.Data.Common;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

public sealed class InstituicaoRepository(FrioSolidarioDbContext contexto) : IInstituicaoRepository
{
    public async Task<IReadOnlyList<Instituicao>> ListarAsync(CancellationToken cancellationToken = default)
        => await contexto.Instituicoes
            .AsNoTracking()
            .OrderBy(i => i.Nome)
            .ToListAsync(cancellationToken);

    public Task<Instituicao?> ObterAsync(long id, CancellationToken cancellationToken = default)
        => contexto.Instituicoes.FirstOrDefaultAsync(i => i.Id == id, cancellationToken);

    public Task<bool> ExisteCnpjAsync(string cnpj, long? ignorarId = null, CancellationToken cancellationToken = default)
        => contexto.Instituicoes
            .AsNoTracking()
            .AnyAsync(i => i.Cnpj == cnpj && (ignorarId == null || i.Id != ignorarId), cancellationToken);

    public async Task<Instituicao> AdicionarAsync(Instituicao instituicao, CancellationToken cancellationToken = default)
    {
        contexto.Instituicoes.Add(instituicao);
        await SalvarAsync(cancellationToken);
        return instituicao;
    }

    public async Task AtualizarAsync(Instituicao instituicao, CancellationToken cancellationToken = default)
    {
        contexto.Instituicoes.Update(instituicao);
        await SalvarAsync(cancellationToken);
    }

    public async Task RemoverAsync(Instituicao instituicao, CancellationToken cancellationToken = default)
    {
        contexto.Instituicoes.Remove(instituicao);
        await SalvarAsync(cancellationToken);
    }

    /// <summary>
    /// Grava traduzindo violacao de constraint em erro de negocio: CNPJ repetido e remocao com
    /// vinculo viram <see cref="Application.Common.ConflitoException"/> (HTTP 409) em vez de 500.
    /// </summary>
    private async Task SalvarAsync(CancellationToken cancellationToken)
    {
        const string duplicado = "Ja existe instituicao com este CNPJ.";
        const string vinculo = "Instituicao possui equipamentos, usuarios ou lotes vinculados e nao pode ser removida.";
        const string invalido = "Dados da instituicao violam uma regra do banco (CNPJ com 14 digitos ou tipo valido).";

        try
        {
            await contexto.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException ex)
        {
            throw ConflitoDeBanco.Traduzir(ex, duplicado, vinculo, invalido);
        }
        catch (DbException ex)
        {
            throw ConflitoDeBanco.Traduzir(ex, duplicado, vinculo, invalido);
        }
    }
}
