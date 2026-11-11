using System.Security.Claims;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json.Serialization;
using System.Text.Unicode;
using System.Threading.RateLimiting;
using FrioSolidario.Api;
using FrioSolidario.Api.Middleware;
using FrioSolidario.Application;
using FrioSolidario.Infrastructure;
using FrioSolidario.Infrastructure.Security;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;

var builder = WebApplication.CreateBuilder(args);

// Sem "Server: Kestrel" na resposta: nao ha motivo para anunciar o servidor a quem varre a rede.
builder.WebHost.ConfigureKestrel(options => options.AddServerHeader = false);

// Enums como texto no JSON e no Swagger: o app manda "CAMARA", nao 0. allowIntegerValues: false
// fecha a porta do numero: {"tipo": 99} viraria um enum fora do vocabulario e so pararia no CHECK
// do banco; assim volta 400 na validacao, antes de chegar ao servico.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter(allowIntegerValues: false));

        // Anti-XSS na saida: <, >, &, ' e " saem como < etc. O padrao do ASP.NET devolve o texto
        // cru, e um nome com <script> gravado direto no banco (seed, carga, script) chegaria intacto a
        // quem montasse HTML com ele. UnicodeRanges.All mantem os acentos legiveis ("Camara" com acento).
        options.JsonSerializerOptions.Encoder = JavaScriptEncoder.Create(UnicodeRanges.All);
    });
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
            // So o algoritmo que a API emite. Aceitar qualquer "alg" do cabecalho e a porta classica
            // para token forjado (alg "none" ou troca de algoritmo).
            ValidAlgorithms = [SecurityAlgorithms.HmacSha256],
            RequireSignedTokens = true,
            RequireExpirationTime = true,
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromSeconds(30),
            NameClaimType = ClaimTypes.Name,
            RoleClaimType = ClaimTypes.Role
        };

        // 401 e 403 no mesmo ProblemDetails das demais falhas (o padrao do JwtBearer e corpo vazio).
        // Token vencido tem mensagem propria: e o sinal para o app voltar ao login.
        options.Events = new JwtBearerEvents
        {
            OnChallenge = contexto =>
            {
                contexto.HandleResponse();
                var expirado = contexto.AuthenticateFailure is SecurityTokenExpiredException;
                return TratamentoDeErroMiddleware.EscreverAsync(
                    contexto.HttpContext,
                    StatusCodes.Status401Unauthorized,
                    "Nao autenticado",
                    expirado
                        ? "Token expirado. Faca login novamente."
                        : "Token ausente ou invalido. Envie Authorization: Bearer <token>.",
                    resposta => resposta.Headers.WWWAuthenticate = JwtBearerDefaults.AuthenticationScheme);
            },
            OnForbidden = contexto => TratamentoDeErroMiddleware.EscreverAsync(
                contexto.HttpContext,
                StatusCodes.Status403Forbidden,
                "Acesso negado",
                "O perfil do usuario nao permite esta operacao.")
        };
    });

// Protegido por padrao: rota nova sem atributo exige token. So o que e marcado [AllowAnonymous]
// (login, health e o redirecionamento para o Swagger) fica publico.
builder.Services.AddAuthorization(options =>
    options.FallbackPolicy = new AuthorizationPolicyBuilder().RequireAuthenticatedUser().Build());

// Limite de tentativas no login, por IP: 10 por minuto sobra para quem erra a senha e corta quem
// tenta adivinhar. O BCrypt ja deixa cada tentativa lenta; isto limita quantas cabem por minuto.
builder.Services.AddRateLimiter(options =>
{
    options.AddPolicy(PoliticasDeLimite.Login, contexto => RateLimitPartition.GetFixedWindowLimiter(
        contexto.Connection.RemoteIpAddress?.ToString() ?? "desconhecido",
        _ => new FixedWindowRateLimiterOptions
        {
            PermitLimit = 10,
            Window = TimeSpan.FromMinutes(1),
            QueueLimit = 0
        }));

    options.OnRejected = (contexto, _) => new ValueTask(TratamentoDeErroMiddleware.EscreverAsync(
        contexto.HttpContext,
        StatusCodes.Status429TooManyRequests,
        "Tentativas demais",
        "Limite de tentativas de login atingido. Espere um minuto e tente de novo.",
        resposta => resposta.Headers.RetryAfter = "60"));
});

var app = builder.Build();

// Cabecalhos de seguranca em toda resposta, inclusive nas de erro (por isso antes do tratamento).
app.UseCabecalhosDeSeguranca();

// Qualquer excecao da Application vira ProblemDetails (401/403/404/400/409/500).
app.UseTratamentoDeErro();

// Swagger sempre ligado: a banca precisa abrir a documentacao sem alterar configuracao. Fica antes
// da autorizacao porque e middleware, nao endpoint: depois dela, a FallbackPolicy tambem o fecharia.
app.UseSwagger();
app.UseSwaggerUI(options =>
{
    options.SwaggerEndpoint("/swagger/v1/swagger.json", "Frio Solidario API v1");
    options.RoutePrefix = "swagger";
});

app.UseAuthentication();
app.UseAuthorization();
app.UseRateLimiter();

app.MapControllers();
app.MapGet("/", () => Results.Redirect("/swagger")).AllowAnonymous();

app.Run();

/// <summary>Exposto para os testes de integracao (xUnit) conseguirem subir a API em memoria.</summary>
public partial class Program;
