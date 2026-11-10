namespace FrioSolidario.Domain.Entities;

/// <summary>Local que doa ou recebe os lotes (banco de alimentos, mercado, padaria).</summary>
public sealed class Instituicao
{
    public long Id { get; set; }

    public string Nome { get; set; } = string.Empty;

    /// <summary>Somente os 14 digitos, sem pontuacao (o CHECK do banco recusa CNPJ formatado).</summary>
    public string Cnpj { get; set; } = string.Empty;

    public TipoInstituicao Tipo { get; set; }

    public string Cidade { get; set; } = string.Empty;

    public string Endereco { get; set; } = string.Empty;
}
