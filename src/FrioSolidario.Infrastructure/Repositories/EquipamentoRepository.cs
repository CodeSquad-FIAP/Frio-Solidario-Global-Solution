using System.Data.Common;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;
using FrioSolidario.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Repositories;

public sealed class EquipamentoRepository(FrioSolidarioDbContext contexto) : IEquipamentoRepository
{
    public async Task<IReadOnlyList<(Equipamento Equipamento, string Instituicao, Leitura? UltimaLeitura)>> ListarAsync(
        long? instituicaoId = null,
        bool? ativo = null,
        CancellationToken cancellationToken = default)
    {
        // A leitura mais recente vem como subconsulta por equipamento: o EF traduz para uma unica
        // consulta SQL (OUTER APPLY), em vez de uma ida ao banco por equipamento no app.
        var consulta =
            from e in contexto.Equipamentos.AsNoTracking()
            join i in contexto.Instituicoes.AsNoTracking() on e.InstituicaoId equals i.Id
            where instituicaoId == null || e.InstituicaoId == instituicaoId
            where ativo == null || e.Ativo == ativo
            orderby i.Nome, e.Nome
            select new
            {
                Equipamento = e,
                Instituicao = i.Nome,
                UltimaLeitura = (Leitura?)contexto.Leituras.AsNoTracking()
                    .Where(l => l.EquipamentoId == e.Id)
                    .OrderByDescending(l => l.RegistradaEm)
                    .ThenByDescending(l => l.Id)
                    .FirstOrDefault(),
            };

        var linhas = await consulta.ToListAsync(cancellationToken);
        return linhas.Select(l => (l.Equipamento, l.Instituicao, (Leitura?)l.UltimaLeitura)).ToList();
    }

    public async Task<(Equipamento Equipamento, string Instituicao, Leitura? UltimaLeitura)?> ObterAsync(
        long id,
        CancellationToken cancellationToken = default)
    {
        var linha = await (
                from e in contexto.Equipamentos.AsNoTracking()
                join i in contexto.Instituicoes.AsNoTracking() on e.InstituicaoId equals i.Id
                where e.Id == id
                select new
                {
                    Equipamento = e,
                    Instituicao = i.Nome,
                    UltimaLeitura = (Leitura?)contexto.Leituras.AsNoTracking()
                        .Where(l => l.EquipamentoId == e.Id)
                        .OrderByDescending(l => l.RegistradaEm)
                        .ThenByDescending(l => l.Id)
                        .FirstOrDefault(),
                })
            .FirstOrDefaultAsync(cancellationToken);

        return linha is null ? null : (linha.Equipamento, linha.Instituicao, linha.UltimaLeitura);
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
