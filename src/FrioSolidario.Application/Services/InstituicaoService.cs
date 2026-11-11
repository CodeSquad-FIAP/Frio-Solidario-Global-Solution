using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>
/// Cadastro de instituicoes. Listar e consultar sao abertos a qualquer usuario logado (nome, CNPJ e
/// endereco sao dados publicos do parceiro); alterar e remover so a propria instituicao do token.
/// </summary>
public sealed class InstituicaoService(IInstituicaoRepository repositorio) : IInstituicaoService
{
    public async Task<IReadOnlyList<InstituicaoResponse>> ListarAsync(CancellationToken cancellationToken = default)
    {
        var instituicoes = await repositorio.ListarAsync(cancellationToken);
        return instituicoes.Select(InstituicaoResponse.De).ToList();
    }

    public async Task<InstituicaoResponse> ObterAsync(long id, CancellationToken cancellationToken = default)
    {
        var instituicao = await repositorio.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Instituicao {id} nao encontrada.");

        return InstituicaoResponse.De(instituicao);
    }

    public async Task<InstituicaoResponse> CriarAsync(
        InstituicaoRequest request,
        CancellationToken cancellationToken = default)
    {
        await GarantirCnpjLivreAsync(request.Cnpj, ignorarId: null, cancellationToken);

        var instituicao = new Instituicao
        {
            Nome = request.Nome.Trim(),
            Cnpj = request.Cnpj,
            Tipo = request.Tipo,
            Cidade = request.Cidade.Trim(),
            Endereco = request.Endereco.Trim()
        };

        var criada = await repositorio.AdicionarAsync(instituicao, cancellationToken);
        return InstituicaoResponse.De(criada);
    }

    public async Task<InstituicaoResponse> AtualizarAsync(
        long id,
        InstituicaoRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var instituicao = await repositorio.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Instituicao {id} nao encontrada.");

        EscopoDaInstituicao.Garantir(id, instituicaoDoToken, $"Instituicao {id} nao e a instituicao do usuario.");
        await GarantirCnpjLivreAsync(request.Cnpj, ignorarId: id, cancellationToken);

        instituicao.Nome = request.Nome.Trim();
        instituicao.Cnpj = request.Cnpj;
        instituicao.Tipo = request.Tipo;
        instituicao.Cidade = request.Cidade.Trim();
        instituicao.Endereco = request.Endereco.Trim();

        await repositorio.AtualizarAsync(instituicao, cancellationToken);
        return InstituicaoResponse.De(instituicao);
    }

    public async Task RemoverAsync(
        long id,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var instituicao = await repositorio.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Instituicao {id} nao encontrada.");

        EscopoDaInstituicao.Garantir(id, instituicaoDoToken, $"Instituicao {id} nao e a instituicao do usuario.");

        await repositorio.RemoverAsync(instituicao, cancellationToken);
    }

    private async Task GarantirCnpjLivreAsync(string cnpj, long? ignorarId, CancellationToken cancellationToken)
    {
        if (await repositorio.ExisteCnpjAsync(cnpj, ignorarId, cancellationToken))
        {
            throw new ConflitoException($"Ja existe instituicao cadastrada com o CNPJ {cnpj}.");
        }
    }
}
