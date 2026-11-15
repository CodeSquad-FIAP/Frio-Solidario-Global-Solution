using FrioSolidario.Domain.Entities;

namespace FrioSolidario.Application.Repositories;

/// <summary>Porta de saida para os equipamentos. Implementacao com EF Core na Infrastructure.</summary>
public interface IEquipamentoRepository
{
    /// <summary>
    /// Equipamentos com o nome da instituicao e a ultima leitura de cada um, na mesma consulta: o
    /// painel do app precisa das duas coisas por equipamento e nao pode fazer uma requisicao por item.
    /// </summary>
    Task<IReadOnlyList<(Equipamento Equipamento, string Instituicao, Leitura? UltimaLeitura)>> ListarAsync(
        long? instituicaoId = null,
        bool? ativo = null,
        CancellationToken cancellationToken = default);

    /// <summary>Equipamento com o nome da instituicao e a ultima leitura, para o cabecalho da tela de detalhe.</summary>
    Task<(Equipamento Equipamento, string Instituicao, Leitura? UltimaLeitura)?> ObterAsync(
        long id,
        CancellationToken cancellationToken = default);

    Task<bool> ExisteAsync(long id, CancellationToken cancellationToken = default);

    Task<Equipamento> AdicionarAsync(Equipamento equipamento, CancellationToken cancellationToken = default);

    Task AtualizarAsync(Equipamento equipamento, CancellationToken cancellationToken = default);
}
