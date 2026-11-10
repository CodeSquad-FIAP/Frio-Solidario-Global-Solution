using FrioSolidario.Application.Common;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Services;

public sealed class EquipamentoService(
    IEquipamentoRepository equipamentos,
    IInstituicaoRepository instituicoes) : IEquipamentoService
{
    public async Task<IReadOnlyList<EquipamentoResponse>> ListarAsync(
        long? instituicaoId = null,
        CancellationToken cancellationToken = default)
    {
        var lista = await equipamentos.ListarAsync(instituicaoId, cancellationToken);
        return lista.Select(item => EquipamentoResponse.De(item.Equipamento, item.Instituicao)).ToList();
    }

    public async Task<EquipamentoResponse> ObterAsync(long id, CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {id} nao encontrado.");

        return EquipamentoResponse.De(encontrado.Equipamento, encontrado.Instituicao);
    }

    public async Task<EquipamentoResponse> CriarAsync(
        EquipamentoRequest request,
        CancellationToken cancellationToken = default)
    {
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
        CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {id} nao encontrado.");

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

    public async Task RemoverAsync(long id, CancellationToken cancellationToken = default)
    {
        var encontrado = await equipamentos.ObterAsync(id, cancellationToken)
            ?? throw new KeyNotFoundException($"Equipamento {id} nao encontrado.");

        await equipamentos.RemoverAsync(encontrado.Equipamento, cancellationToken);
    }

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
