using FrioSolidario.Application.Common;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;

namespace FrioSolidario.Api.Middleware;

/// <summary>
/// Traduz as excecoes da Application em resposta HTTP: 404 para "nao encontrado",
/// 400 para regra de negocio e 409 para conflito com o estado do banco.
/// O corpo sai em ProblemDetails, o mesmo formato que o [ApiController] usa na validacao.
/// </summary>
public sealed class TratamentoDeErroMiddleware(RequestDelegate next, ILogger<TratamentoDeErroMiddleware> logger)
{
    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await next(context);
        }
        catch (KeyNotFoundException ex)
        {
            await EscreverAsync(context, StatusCodes.Status404NotFound, "Recurso nao encontrado", ex.Message);
        }
        catch (CredenciaisInvalidasException ex)
        {
            await EscreverAsync(context, StatusCodes.Status401Unauthorized, "Credenciais invalidas", ex.Message);
        }
        catch (AcessoNegadoException ex)
        {
            await EscreverAsync(context, StatusCodes.Status403Forbidden, "Acesso negado", ex.Message);
        }
        catch (RegraDeNegocioException ex)
        {
            await EscreverAsync(context, StatusCodes.Status400BadRequest, "Regra de negocio violada", ex.Message);
        }
        catch (ConflitoException ex)
        {
            await EscreverAsync(context, StatusCodes.Status409Conflict, "Conflito com os dados atuais", ex.Message);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Falha nao tratada em {Metodo} {Caminho}", context.Request.Method, context.Request.Path);
            await EscreverAsync(
                context,
                StatusCodes.Status500InternalServerError,
                "Erro inesperado",
                "Nao foi possivel concluir a operacao. Consulte o log da API.");
        }
    }

    /// <summary>
    /// Tambem usado pelos eventos do JWT (401/403) e pelo limitador do login (429): toda resposta de
    /// erro da API sai no mesmo formato, venha de excecao, de token recusado ou de excesso de tentativas.
    /// </summary>
    internal static Task EscreverAsync(
        HttpContext context,
        int status,
        string titulo,
        string detalhe,
        Action<HttpResponse>? cabecalhos = null)
    {
        if (context.Response.HasStarted)
        {
            return Task.CompletedTask;
        }

        context.Response.Clear();
        context.Response.StatusCode = status;

        // Depois do Clear, que apagaria cabecalhos como WWW-Authenticate (401) e Retry-After (429).
        cabecalhos?.Invoke(context.Response);

        return context.Response.WriteAsJsonAsync(new ProblemDetails
        {
            Status = status,
            Title = titulo,
            Detail = detalhe,
            Instance = context.Request.Path
        });
    }
}

public static class TratamentoDeErroExtensions
{
    public static IApplicationBuilder UseTratamentoDeErro(this IApplicationBuilder app)
        => app.UseMiddleware<TratamentoDeErroMiddleware>();
}
