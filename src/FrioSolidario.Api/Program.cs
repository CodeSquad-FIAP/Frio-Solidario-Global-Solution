using System.Security.Claims;
using System.Text;
using System.Text.Json.Serialization;
using FrioSolidario.Api.Middleware;
using FrioSolidario.Application;
using FrioSolidario.Infrastructure;
using FrioSolidario.Infrastructure.Security;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
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

    // Botao Authorize do Swagger: cola o token de POST /api/auth/login e as rotas protegidas respondem.
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Informe apenas o token devolvido por POST /api/auth/login."
    });
    options.AddSecurityRequirement(documento => new OpenApiSecurityRequirement
    {
        [new OpenApiSecuritySchemeReference("Bearer", documento)] = new List<string>()
    });
});

builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);

// JWT do app. A chave vem do appsettings e pode ser trocada sem tocar em codigo por Jwt__Key.
var jwt = builder.Configuration.GetSection(JwtOptions.Secao).Get<JwtOptions>()
    ?? throw new InvalidOperationException("Configure a secao Jwt no appsettings.json.");

if (string.IsNullOrWhiteSpace(jwt.Key) || jwt.Key.Length < 32)
{
    throw new InvalidOperationException("Jwt:Key precisa ter pelo menos 32 caracteres (HMAC-SHA256).");
}

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        // Sem o mapeamento automatico de claims: o codigo le "sub" e "instituicao" como foram emitidos.
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwt.Issuer,
            ValidateAudience = true,
            ValidAudience = jwt.Audience,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwt.Key)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromSeconds(30),
            NameClaimType = ClaimTypes.Name,
            RoleClaimType = ClaimTypes.Role
        };
    });
builder.Services.AddAuthorization();

var app = builder.Build();

// Primeiro na pilha: qualquer excecao da Application vira ProblemDetails (401/403/404/400/409/500).
app.UseTratamentoDeErro();

app.UseAuthentication();
app.UseAuthorization();

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
