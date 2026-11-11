using System.ComponentModel.DataAnnotations;

namespace FrioSolidario.Application.Validation;

/// <summary>
/// Recusa texto com <c>&lt;</c>, <c>&gt;</c> ou caracteres de controle. E defesa em profundidade
/// contra XSS armazenado: a API devolve JSON escapado e o PDF desenha texto puro, mas o nome de uma
/// instituicao ou equipamento aparece no app, no Swagger e no relatorio impresso. Nenhum nome ou
/// endereco legitimo precisa de marcacao, entao barrar na entrada nao custa nada e impede que um
/// script fique guardado no banco esperando uma tela que nao escape a saida.
/// </summary>
[AttributeUsage(AttributeTargets.Property | AttributeTargets.Parameter | AttributeTargets.Field)]
public sealed class SemMarcacaoHtmlAttribute : ValidationAttribute
{
    public SemMarcacaoHtmlAttribute()
        : base("{0} nao pode conter os caracteres < ou > nem caracteres de controle.")
    {
    }

    public override bool IsValid(object? value) =>
        value is not string texto || !texto.Any(caractere => caractere is '<' or '>' || char.IsControl(caractere));
}
