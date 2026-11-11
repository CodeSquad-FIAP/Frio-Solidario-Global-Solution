using System.Data.Common;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

public sealed class EquipamentoRepository(FrioSolidarioDbContext contexto) : IEquipamentoRepository
{
    public async Task<IReadOnlyList<(Equipamento Equipamento, string Instituicao)>> ListarAsync(
        long? instituicaoId = null,
        bool? ativo = null,
        CancellationToken cancellationToken = default)
    {
        var consulta =
            from e in contexto.Equipamentos.AsNoTracking()
            join i in contexto.Instituicoes.AsNoTracking() on e.InstituicaoId equals i.Id
            where instituicaoId == null || e.InstituicaoId == instituicaoId
            where ativo == null || e.Ativo == ativo
            orderby i.Nome, e.Nome
            select new { Equipamento = e, Instituicao = i.Nome };

        var linhas = await consulta.ToListAsync(cancellationToken);
        return linhas.Select(l => (l.Equipamento, l.Instituicao)).ToList();
    }

    public async Task<(Equipamento Equipamento, string Instituicao)?> ObterAsync(
        long id,
        CancellationToken cancellationToken = default)
    {
        var linha = await (
                from e in contexto.Equipamentos.AsNoTracking()
                join i in contexto.Instituicoes.AsNoTracking() on e.InstituicaoId equals i.Id
                where e.Id == id
                select new { Equipamento = e, Instituicao = i.Nome })
            .FirstOrDefaultAsync(cancellationToken);

        return linha is null ? null : (linha.Equipamento, linha.Instituicao);
    }

    public Task<bool> ExisteAsync(long id, CancellationToken cancellationToken = default)
        => contexto.Equipamentos.AsNoTracking().AnyAsync(e => e.Id == id, cancellationToken);

    public async Task<Equipamento> AdicionarAsync(Equipamento equipamento, CancellationToken cancellationToken = default)
    {
        contexto.Equipamentos.Add(equipamento);
        await SalvarAsync(cancellationToken);
        return equipamento;
    }

    public async Task AtualizarAsync(Equipamento equipamento, CancellationToken cancellationToken = default)
    {
        contexto.Equipamentos.Update(equipamento);
        await SalvarAsync(cancellationToken);
    }

    /// <summary>
    /// Grava traduzindo violacao de constraint em erro de negocio: dados repetidos ou faixa invalida
    /// viram <see cref="Application.Common.ConflitoException"/> (HTTP 409), nao 500.
    /// </summary>
    private async Task SalvarAsync(CancellationToken cancellationToken)
    {
        const string duplicado = "Ja existe equipamento com estes dados.";
        const string vinculo = "Equipamento possui leituras ou alertas vinculados e nao pode ser removido.";
        const string invalido = "Dados do equipamento violam uma regra do banco (faixa de temperatura, umidade ou tipo).";

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
