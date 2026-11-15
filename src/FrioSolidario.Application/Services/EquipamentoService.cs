using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

/// <summary>
/// CRUD dos equipamentos com isolamento por instituicao: sem filtro a lista ja vem so com os da
/// instituicao do token, e consultar, cadastrar, alterar ou remover equipamento de outra
/// instituicao e 403 -- o mesmo criterio das leituras e do relatorio.
/// </summary>
public sealed class EquipamentoService(
    IEquipamentoRepository equipamentos,
    IInstituicaoRepository instituicoes) : IEquipamentoService
{
    public async Task<IReadOnlyList<EquipamentoResponse>> ListarAsync(
        long? instituicaoId = null,
        long? instituicaoDoToken = null,
        bool? ativo = null,
        CancellationToken cancellationToken = default)
    {
        if (instituicaoId is not null)
        {
            EscopoDaInstituicao.Garantir(
                instituicaoId.Value,
                instituicaoDoToken,
                $"Instituicao {instituicaoId} nao e a instituicao do usuario.");
        }

        var lista = await equipamentos.ListarAsync(instituicaoId ?? instituicaoDoToken, ativo, cancellationToken);
        return lista
            .Select(item => EquipamentoResponse.De(item.Equipamento, item.Instituicao, item.UltimaLeitura))
            .ToList();
    }

    public async Task<EquipamentoResponse> ObterAsync(
        long id,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {id} nao encontrado.");

        GarantirDoUsuario(encontrado.Equipamento.InstituicaoId, id, instituicaoDoToken);

        return EquipamentoResponse.De(encontrado.Equipamento, encontrado.Instituicao, encontrado.UltimaLeitura);
    }

    public async Task<EquipamentoResponse> CriarAsync(
        EquipamentoRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        GarantirInstituicaoDoCadastro(request, instituicaoDoToken);
        ValidarFaixa(request);
        await GarantirInstituicaoAsync(request.InstituicaoId, cancellationToken);

        var equipamento = new Equipamento
        {
            InstituicaoId = request.InstituicaoId,
            Nome = request.Nome.Trim(),
            Tipo = request.Tipo,
            TempMin = request.TempMin,
            TempMax = request.TempMax,
            UmidMax = request.UmidMax,
            Ativo = request.Ativo
        };

        var criado = await equipamentos.AdicionarAsync(equipamento, cancellationToken);
        var instituicao = await instituicoes.ObterAsync(criado.InstituicaoId, cancellationToken);
        return EquipamentoResponse.De(criado, instituicao?.Nome ?? string.Empty);
    }

    public async Task<EquipamentoResponse> AtualizarAsync(
        long id,
        EquipamentoRequest request,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {id} nao encontrado.");

        // Os dois lados: o equipamento precisa ser do usuario e nao pode ser transferido para outra instituicao.
        GarantirDoUsuario(encontrado.Equipamento.InstituicaoId, id, instituicaoDoToken);
        GarantirInstituicaoDoCadastro(request, instituicaoDoToken);
        ValidarFaixa(request);
        await GarantirInstituicaoAsync(request.InstituicaoId, cancellationToken);

        var equipamento = encontrado.Equipamento;
        equipamento.InstituicaoId = request.InstituicaoId;
        equipamento.Nome = request.Nome.Trim();
        equipamento.Tipo = request.Tipo;
        equipamento.TempMin = request.TempMin;
        equipamento.TempMax = request.TempMax;
        equipamento.UmidMax = request.UmidMax;
        equipamento.Ativo = request.Ativo;

        await equipamentos.AtualizarAsync(equipamento, cancellationToken);
        var instituicao = await instituicoes.ObterAsync(equipamento.InstituicaoId, cancellationToken);
        return EquipamentoResponse.De(equipamento, instituicao?.Nome ?? string.Empty);
    }

    /// <summary>
    /// Desativa o equipamento em vez de apagar: leitura e alerta antigos precisam sobreviver para o
    /// log termico da vigilancia sanitaria. Equipamento ja desativado e 409, como alerta ja resolvido.
    /// </summary>
    public async Task RemoverAsync(
        long id,
        long? instituicaoDoToken = null,
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {id} nao encontrado.");

        GarantirDoUsuario(encontrado.Equipamento.InstituicaoId, id, instituicaoDoToken);

        if (!encontrado.Equipamento.Ativo)
        {
            throw new ConflitoException($"Equipamento {id} ja esta desativado.");
        }

        encontrado.Equipamento.Ativo = false;
        await equipamentos.AtualizarAsync(encontrado.Equipamento, cancellationToken);
    }

    private static void GarantirDoUsuario(long instituicaoDoEquipamento, long id, long? instituicaoDoToken)
        => EscopoDaInstituicao.Garantir(
            instituicaoDoEquipamento,
            instituicaoDoToken,
            $"Equipamento {id} pertence a outra instituicao.");

    /// <summary>Cadastrar ou mover equipamento para outra instituicao seria escrever nos dados dela.</summary>
    private static void GarantirInstituicaoDoCadastro(EquipamentoRequest request, long? instituicaoDoToken)
        => EscopoDaInstituicao.Garantir(
            request.InstituicaoId,
            instituicaoDoToken,
            $"Instituicao {request.InstituicaoId} nao e a instituicao do usuario.");

    /// <summary>Mesma regra do CHECK ck_equipamento_faixa: minimo estritamente menor que o maximo.</summary>
    private static void ValidarFaixa(EquipamentoRequest request)
    {
        if (request.TempMin >= request.TempMax)
        {
            throw new RegraDeNegocioException(
                $"Temperatura minima ({request.TempMin}) deve ser menor que a maxima ({request.TempMax}).");
        }
    }

    private async Task GarantirInstituicaoAsync(long instituicaoId, CancellationToken cancellationToken)
    {
        if (await instituicoes.ObterAsync(instituicaoId, cancellationToken) is null)
        {
            throw new RegraDeNegocioException($"Instituicao {instituicaoId} nao existe.");
        }
    }
}
