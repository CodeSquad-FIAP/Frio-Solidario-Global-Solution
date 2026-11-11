namespace FrioSolidario.Application.Common;

/// <summary>
/// Regra de isolamento entre instituicoes: o usuario so enxerga e altera o que e da instituicao
/// gravada no token. Fica num lugar so para equipamentos, alertas e instituicoes aplicarem o mesmo
/// criterio que leituras e relatorio ja usam (403 quando o recurso e de outra instituicao).
/// </summary>
internal static class EscopoDaInstituicao
{
    /// <summary>
    /// Lanca <see cref="AcessoNegadoException"/> quando o recurso pertence a outra instituicao.
    /// <paramref name="instituicaoDoToken"/> nulo significa chamada interna, sem usuario: a Api
    /// sempre passa a instituicao do token (e recusa token sem ela).
    /// </summary>
    public static void Garantir(long instituicaoDoRecurso, long? instituicaoDoToken, string mensagem)
    {
        if (instituicaoDoToken is not null && instituicaoDoToken != instituicaoDoRecurso)
        {
            throw new AcessoNegadoException(mensagem);
        }
    }
}
