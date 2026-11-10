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

    private static Task EscreverAsync(HttpContext context, int status, string titulo, string detalhe)
    {
        if (context.Response.HasStarted)
        {
            return Task.CompletedTask;
        }

        context.Response.Clear();
        context.Response.StatusCode = status;

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
