using System.Text.Json.Serialization;
using FrioSolidario.Api.Middleware;
using FrioSolidario.Application;
using FrioSolidario.Infrastructure;
using Microsoft.OpenApi;

var builder = WebApplication.CreateBuilder(args);

// Enums como texto no JSON e no Swagger: o app manda "CAMARA", nao 0.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Frio Solidario API",
        Version = "v1",
        Description =
            "Monitoramento da cadeia de frio em bancos de alimentos e comercios doadores. " +
            "A API recebe leituras de temperatura, umidade e porta, compara com a faixa de cada " +
            "equipamento e abre alertas para o app do voluntario. Global Solution 2026/2 - FIAP 3ESOA."
    });
});

builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);

var app = builder.Build();

// Primeiro na pilha: qualquer excecao da Application vira ProblemDetails (404/400/409/500).
app.UseTratamentoDeErro();

// Swagger sempre ligado: a banca precisa abrir a documentacao sem alterar configuracao.
app.UseSwagger();
app.UseSwaggerUI(options =>
{
    options.SwaggerEndpoint("/swagger/v1/swagger.json", "Frio Solidario API v1");
    options.RoutePrefix = "swagger";
});

app.MapControllers();
app.MapGet("/", () => Results.Redirect("/swagger"));

app.Run();

/// <summary>Exposto para os testes de integracao (xUnit) conseguirem subir a API em memoria.</summary>
public partial class Program;
