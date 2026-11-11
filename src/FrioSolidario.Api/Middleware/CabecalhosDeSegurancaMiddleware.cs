namespace FrioSolidario.Api.Middleware;

/// <summary>
/// Cabecalhos de seguranca em toda resposta. A API so devolve JSON e PDF, entao nenhum navegador
/// deveria interpretar uma resposta dela como pagina: <c>nosniff</c> impede que um JSON com texto
/// malicioso seja "adivinhado" como HTML, e a CSP <c>default-src 'none'</c> faz com que, mesmo se
/// isso acontecer, nenhum script rode. O Swagger UI e uma pagina de verdade (carrega script e
/// estilo proprios), por isso fica fora da CSP, mas recebe os demais cabecalhos.
/// </summary>
public sealed class CabecalhosDeSegurancaMiddleware(RequestDelegate next)
{
    public Task InvokeAsync(HttpContext context)
    {
        // OnStarting: o TratamentoDeErroMiddleware limpa a resposta (Response.Clear) ao trocar por
        // ProblemDetails, o que apagaria cabecalhos gravados antes. Aqui eles entram por ultimo.
        context.Response.OnStarting(() =>
        {
            var cabecalhos = context.Response.Headers;
            cabecalhos.XContentTypeOptions = "nosniff";
            cabecalhos.XFrameOptions = "DENY";
            cabecalhos["Referrer-Policy"] = "no-referrer";

            if (!context.Request.Path.StartsWithSegments("/swagger"))
            {
                cabecalhos.ContentSecurityPolicy = "default-src 'none'; frame-ancestors 'none'";
            }

            // Token, dados da instituicao e relatorio nao devem ficar em cache de proxy nem do aparelho.
            if (context.Request.Path.StartsWithSegments("/api"))
            {
                cabecalhos.CacheControl = "no-store";
            }

            return Task.CompletedTask;
        });

        return next(context);
    }
}

public static class CabecalhosDeSegurancaExtensions
{
    public static IApplicationBuilder UseCabecalhosDeSeguranca(this IApplicationBuilder app)
        => app.UseMiddleware<CabecalhosDeSegurancaMiddleware>();
}
