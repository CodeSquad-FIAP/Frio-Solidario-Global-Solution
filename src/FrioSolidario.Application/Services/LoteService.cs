using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Cadastro e retirada de lotes, sempre na instituicao do token: lote de outra instituicao e 403, o
/// mesmo criterio de equipamento, leitura e alerta. O lote nasce DISPONIVEL e so sai desse estado
/// pela retirada (DISTRIBUIDO ou PERDIDO, com data), como o CHECK ck_lote_retirada_coerente exige.
/// </summary>
public sealed class LoteService(ILoteRepository lotes) : ILoteService
{
    public async Task<IReadOnlyList<LoteResponse>> ListarAsync(
        long instituicaoDoToken,
        StatusLote? status = null,
        CancellationToken cancellationToken = default)
    {
        var lista = await lotes.ListarAsync(instituicaoDoToken, status, cancellationToken);
        return lista.Select(LoteResponse.De).ToList();
    }

    public async Task<LoteResponse> ObterAsync(
        long id,
        long instituicaoDoToken,
        CancellationToken cancellationToken = default)
        => LoteResponse.De(await ObterDoUsuarioAsync(id, instituicaoDoToken, cancellationToken));

    public async Task<LoteResponse> CriarAsync(
        LoteRequest request,
        long instituicaoDoToken,
        CancellationToken cancellationToken = default)
    {
        if (request.Validade < DateOnly.FromDateTime(DateTime.UtcNow))
        {
            throw new RegraDeNegocioException(
                "Validade nao pode estar no passado: lote vencido nao entra no estoque da doacao.");
        }

        var lote = new Lote
        {
            InstituicaoId = instituicaoDoToken,
            Descricao = request.Descricao.Trim(),
            Categoria = request.Categoria.Trim().ToUpperInvariant(),
            Kg = request.Kg,
            Validade = request.Validade,
            Status = StatusLote.DISPONIVEL,
            RetiradoEm = null
        };

        return LoteResponse.De(await lotes.AdicionarAsync(lote, cancellationToken));
    }

    public async Task<LoteResponse> RegistrarRetiradaAsync(
        long id,
        RetiradaDeLoteRequest request,
        long instituicaoDoToken,
        CancellationToken cancellationToken = default)
    {
        if (request.Status == StatusLote.DISPONIVEL)
        {
            throw new RegraDeNegocioException(
                "Retirada aceita DISTRIBUIDO ou PERDIDO; para voltar a DISPONIVEL o lote teria de ser cadastrado de novo.");
        }

        var lote = await ObterDoUsuarioAsync(id, instituicaoDoToken, cancellationToken);

        if (lote.Status != StatusLote.DISPONIVEL)
        {
            throw new ConflitoException($"Lote {id} ja saiu do estoque como {lote.Status}.");
        }

        lote.Status = request.Status;
        lote.RetiradoEm = (request.RetiradoEm ?? DateTimeOffset.UtcNow).ToUniversalTime();
        await lotes.AtualizarAsync(lote, cancellationToken);

        return LoteResponse.De(lote);
    }

    private async Task<Lote> ObterDoUsuarioAsync(long id, long instituicaoDoToken, CancellationToken cancellationToken)
    {
        var lote = await lotes.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Lote {id} nao encontrado.");

        EscopoDaInstituicao.Garantir(lote.InstituicaoId, instituicaoDoToken, $"Lote {id} pertence a outra instituicao.");

        return lote;
    }
}
