using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using FrioSolidario.Application.DTOs;
using FrioSolidario.Application.Relatorios;
using FrioSolidario.Application.Repositories;
using FrioSolidario.Application.Security;
using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace FrioSolidario.Tests;

/// <summary>
/// Opções de JSON iguais às da API (enums como texto, camelCase): sem isso o corpo sai com enum
/// numérico — que a API recusa com 400 — e a leitura da resposta quebra ao desserializar o enum.
/// </summary>
internal static class JsonDaApi
{
    public static readonly JsonSerializerOptions Opcoes = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter() }
    };
}

/// <summary>
/// API de verdade em memória (WebApplicationFactory = TestServer): autenticação, autorização,
/// controllers, middleware de erro e Swagger são os de produção; só os repositórios viram memória,
/// para o teste não depender de PostgreSQL. É o que o card do xUnit pede — o caso 5 e o caso 6
/// exercitados por HTTP, não só pela classe de serviço.
/// </summary>
internal sealed class ApiDeTeste : WebApplicationFactory<global::Program>
{
    /// <summary>Câmara fria do seed (instituição 1, faixa 0 a 4 °C) — a do caso 6 do plano.</summary>
    public static Equipamento CamaraDoSeed() => new()
    {
        Id = 1,
        InstituicaoId = 1,
        Nome = "Câmara fria de hortifrúti",
        Tipo = TipoEquipamento.CAMARA,
        TempMin = 0m,
        TempMax = 4m,
        UmidMax = 85m,
        Ativo = true
    };

    public LeiturasEmMemoria Leituras { get; } = new();

    public AlertasEmMemoria Alertas { get; } = new();

    public EquipamentosEmMemoria Equipamentos { get; } = new(CamaraDoSeed(), Sequencias.FreezerDoSeed());

    /// <summary>Um lote da instituicao 2, para provar o isolamento entre instituicoes.</summary>
    public LotesEmMemoria Lotes { get; } = new(new Lote
    {
        Id = 1,
        InstituicaoId = 2,
        Descricao = "Lote da outra instituição",
        Categoria = "FRUTAS",
        Kg = 10m,
        Validade = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(10)),
        Status = StatusLote.DISPONIVEL
    });

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        // Só os repositórios são trocados. A configuração vem do appsettings.json da própria API:
        // sobrescrever Jwt:Key aqui não funcionaria, porque o Program lê a chave ao registrar o
        // JwtBearer (antes destes callbacks) e o token sairia assinado com outra chave — 401 em tudo.
        builder.ConfigureServices(servicos =>
        {
            // As listagens devolvem a ultima leitura de cada equipamento: o fake le os Itens daqui.
            Equipamentos.Leituras = Leituras;

            servicos.RemoveAll<ILeituraRepository>();
            servicos.RemoveAll<IAlertaRepository>();
            servicos.RemoveAll<IEquipamentoRepository>();
            servicos.RemoveAll<ILoteRepository>();

            servicos.AddSingleton<ILeituraRepository>(Leituras);
            servicos.AddSingleton<IAlertaRepository>(Alertas);
            servicos.AddSingleton<IEquipamentoRepository>(Equipamentos);
            servicos.AddSingleton<ILoteRepository>(Lotes);
        });
    }

    /// <summary>Token assinado pela ITokenService da própria API, para o papel e a instituição pedidos.</summary>
    public AuthenticationHeaderValue Token(PapelUsuario papel = PapelUsuario.ADMIN, long instituicaoId = 1)
    {
        using var escopo = Services.CreateScope();
        var tokens = escopo.ServiceProvider.GetRequiredService<ITokenService>();
        var usuario = new Usuario
        {
            Id = 1,
            Nome = "Mariana Costa",
            Email = "mariana.costa@caminhosdobem.example",
            SenhaHash = "irrelevante-para-o-token",
            Papel = papel,
            InstituicaoId = instituicaoId
        };

        return new AuthenticationHeaderValue("Bearer", tokens.Gerar(usuario).Valor);
    }

    /// <summary>Cliente já autenticado, para os casos que só precisam do token.</summary>
    public HttpClient Cliente(PapelUsuario papel = PapelUsuario.ADMIN, long instituicaoId = 1)
    {
        var cliente = CreateClient();
        cliente.DefaultRequestHeaders.Authorization = Token(papel, instituicaoId);
        return cliente;
    }
}

/// <summary>
/// Casos 5, 6 e 7 do plano de testes pela API: o mesmo caminho que o app e o sensor usam, com o
/// pipeline HTTP inteiro. As leituras usam horário fixo no passado porque a API recusa data futura.
/// </summary>
public class ApiTests
{
    private static LeituraRequest Leitura(long equipamentoId, decimal temperatura, int indice) => new(
        EquipamentoId: equipamentoId,
        Temperatura: temperatura,
        Umidade: 75m,
        PortaAberta: false,
        RegistradaEm: Sequencias.Inicio + Sequencias.Intervalo * indice,
        Origem: OrigemLeitura.SIMULADO);

    [Fact]
    [Trait("Caso", "7")]
    public async Task SemToken_LeituraVolta401()
    {
        using var app = new ApiDeTeste();
        var cliente = app.CreateClient();

        var resposta = await cliente.PostAsJsonAsync("/api/leituras", Leitura(4, -16m, 0), JsonDaApi.Opcoes);

        Assert.Equal(HttpStatusCode.Unauthorized, resposta.StatusCode);
        Assert.Empty(app.Leituras.Itens);
    }

    /// <summary>Caso 5: 1 ou 2 leituras fora da faixa não abrem alerta; a terceira fora de ordem não conta.</summary>
    [Fact]
    [Trait("Caso", "5")]
    public async Task Caso5_SequenciaDoPlano_NaoAbreAlerta()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        // -16, -16 (fora), -20 (dentro: zera), -18 exatamente no limite (não é fora).
        var temperaturas = new[] { -16m, -16m, -20m, -18m, -18m, -18m };
        for (var i = 0; i < temperaturas.Length; i++)
        {
            var resposta = await cliente.PostAsJsonAsync("/api/leituras", Leitura(4, temperaturas[i], i), JsonDaApi.Opcoes);
            Assert.Equal(HttpStatusCode.Created, resposta.StatusCode);

            var corpo = await resposta.Content.ReadFromJsonAsync<LeituraRegistradaResponse>(JsonDaApi.Opcoes);
            Assert.NotNull(corpo);
            Assert.Empty(corpo!.AlertasAbertos);
        }

        Assert.Equal(6, app.Leituras.Itens.Count);
        Assert.Empty(app.Alertas.Itens);
    }

    /// <summary>Caso 6: a terceira leitura seguida acima da faixa abre 1 alerta crítico, e a quarta não duplica.</summary>
    [Fact]
    [Trait("Caso", "6")]
    public async Task Caso6_TresLeiturasAcimaDaFaixa_AbremUmAlertaCritico()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(); // ADMIN da instituição 1, dona da câmara (faixa 0 a 4 °C)

        for (var i = 0; i < 2; i++)
        {
            var resposta = await cliente.PostAsJsonAsync("/api/leituras", Leitura(1, 7.8m, i), JsonDaApi.Opcoes);
            var corpo = await resposta.Content.ReadFromJsonAsync<LeituraRegistradaResponse>(JsonDaApi.Opcoes);
            Assert.Empty(corpo!.AlertasAbertos);
        }

        var terceira = await cliente.PostAsJsonAsync("/api/leituras", Leitura(1, 7.8m, 2), JsonDaApi.Opcoes);
        var comAlerta = await terceira.Content.ReadFromJsonAsync<LeituraRegistradaResponse>(JsonDaApi.Opcoes);
        var alerta = Assert.Single(comAlerta!.AlertasAbertos);
        Assert.Equal(TipoAlerta.TEMP_ALTA, alerta.Tipo);
        Assert.Equal(SeveridadeAlerta.CRITICA, alerta.Severidade);
        Assert.Equal(StatusAlerta.ABERTO, alerta.Status);

        var quarta = await cliente.PostAsJsonAsync("/api/leituras", Leitura(1, 7.8m, 3), JsonDaApi.Opcoes);
        var semDuplicar = await quarta.Content.ReadFromJsonAsync<LeituraRegistradaResponse>(JsonDaApi.Opcoes);
        Assert.Empty(semDuplicar!.AlertasAbertos);
        Assert.Single(app.Alertas.Itens);
    }

    /// <summary>
    /// O painel do app precisa da lista ja com a ultima leitura de cada equipamento: sem isso ele
    /// fazia uma requisicao por equipamento (N+1). Prova o dado no corpo da rota da instituicao.
    /// </summary>
    [Fact]
    public async Task EquipamentosDaInstituicao_TrazemAUltimaLeitura()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 1);

        await cliente.PostAsJsonAsync("/api/leituras", Leitura(1, 2.5m, 0), JsonDaApi.Opcoes);
        await cliente.PostAsJsonAsync("/api/leituras", Leitura(1, 3.5m, 1), JsonDaApi.Opcoes);

        var daInstituicao1 = await cliente.GetFromJsonAsync<List<EquipamentoResponse>>(
            "/api/instituicoes/1/equipamentos", JsonDaApi.Opcoes);

        var camara = Assert.Single(daInstituicao1!);
        Assert.NotNull(camara.UltimaLeitura);
        Assert.Equal(3.5m, camara.UltimaLeitura!.Temperatura);
        Assert.Equal(1, camara.UltimaLeitura.EquipamentoId);

        var daInstituicao2 = await app.Cliente(instituicaoId: 2).GetFromJsonAsync<List<EquipamentoResponse>>(
            "/api/instituicoes/2/equipamentos", JsonDaApi.Opcoes);

        var freezer = Assert.Single(daInstituicao2!);
        Assert.Null(freezer.UltimaLeitura);
    }

    /// <summary>
    /// Sem datas o relatório cobre 7 dias incluindo hoje (não 8): o nome do arquivo traz a janela.
    /// </summary>
    [Fact]
    public async Task RelatorioSemDatas_CobreSeteDiasIncluindoHoje()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 1);

        var resposta = await cliente.GetAsync("/api/equipamentos/1/relatorio");

        Assert.Equal(HttpStatusCode.OK, resposta.StatusCode);
        var hoje = DateOnly.FromDateTime(FusoDoProjeto.Local(DateTimeOffset.Now).DateTime);
        var nome = resposta.Content.Headers.ContentDisposition!.FileName!.Trim('"');
        Assert.Equal($"relatorio-equipamento-1-{hoje.AddDays(-6):yyyyMMdd}-{hoje:yyyyMMdd}.pdf", nome);
    }

    /// <summary>Rota aninhada do card do CRUD: `/instituicoes/{id}/equipamentos` respeita a instituição do token.</summary>
    [Fact]
    public async Task InstituicaoEquipamentos_DaPropriaInstituicao_ListaEDeOutra_403()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 1);

        var propria = await cliente.GetAsync("/api/instituicoes/1/equipamentos");
        Assert.Equal(HttpStatusCode.OK, propria.StatusCode);
        var equipamentos = await propria.Content.ReadFromJsonAsync<List<EquipamentoResponse>>(JsonDaApi.Opcoes);
        Assert.Single(equipamentos!);
        Assert.Equal(1, equipamentos![0].Id);

        var outra = await cliente.GetAsync("/api/instituicoes/2/equipamentos");
        Assert.Equal(HttpStatusCode.Forbidden, outra.StatusCode);
    }

    /// <summary>DELETE é lógico: desativa, o histórico continua, e desativar de novo devolve 409.</summary>
    [Fact]
    public async Task Delete_DesativaEquipamento_SemApagarHistorico()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        // Leitura de 5 minutos atrás: a série de 24h filtra pelo relógio, então não serve o instante
        // fixo de 2026-01-15 usado nos casos 5 e 6.
        var leitura = await cliente.PostAsJsonAsync(
            "/api/leituras",
            new LeituraRequest(4, -20m, 75m, false, DateTimeOffset.UtcNow.AddMinutes(-5), OrigemLeitura.SIMULADO),
            JsonDaApi.Opcoes);
        Assert.Equal(HttpStatusCode.Created, leitura.StatusCode);

        var desativar = await cliente.DeleteAsync("/api/equipamentos/4");
        Assert.Equal(HttpStatusCode.NoContent, desativar.StatusCode);

        var depois = await cliente.GetFromJsonAsync<EquipamentoResponse>("/api/equipamentos/4", JsonDaApi.Opcoes);
        Assert.NotNull(depois);
        Assert.False(depois!.Ativo);

        var historico = await cliente.GetFromJsonAsync<List<LeituraResponse>>(
            "/api/equipamentos/4/leituras?periodo=24h", JsonDaApi.Opcoes);
        Assert.NotNull(historico);
        Assert.Single(historico!);

        var deNovo = await cliente.DeleteAsync("/api/equipamentos/4");
        Assert.Equal(HttpStatusCode.Conflict, deNovo.StatusCode);
    }


    /// <summary>
    /// O painel do app precisa só da leitura mais recente de cada equipamento: a rota devolve uma
    /// leitura, mesmo que ela seja mais antiga que a janela de 24h do gráfico.
    /// </summary>
    [Fact]
    public async Task UltimaLeitura_DevolveAMaisRecente_MesmoForaDaJanelaDoGrafico()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        foreach (var (temperatura, quando) in new[]
                 {
                     (-21m, DateTimeOffset.UtcNow.AddDays(-9)),
                     (-19.5m, DateTimeOffset.UtcNow.AddDays(-3)),
                 })
        {
            var gravada = await cliente.PostAsJsonAsync(
                "/api/leituras",
                new LeituraRequest(4, temperatura, 75m, false, quando, OrigemLeitura.SIMULADO),
                JsonDaApi.Opcoes);
            Assert.Equal(HttpStatusCode.Created, gravada.StatusCode);
        }

        var ultima = await cliente.GetFromJsonAsync<LeituraResponse>(
            "/api/equipamentos/4/leituras/ultima", JsonDaApi.Opcoes);

        Assert.NotNull(ultima);
        Assert.Equal(-19.5m, ultima!.Temperatura);
        Assert.Equal(4, ultima.EquipamentoId);
    }

    /// <summary>Equipamento sem leitura nenhuma responde 204, que o app mostra como "sem leitura".</summary>
    [Fact]
    public async Task UltimaLeitura_SemLeitura_Volta204()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        var resposta = await cliente.GetAsync("/api/equipamentos/4/leituras/ultima");

        Assert.Equal(HttpStatusCode.NoContent, resposta.StatusCode);
    }

    /// <summary>A rota segue o mesmo isolamento da série: outra instituição é 403, id inexistente é 404.</summary>
    [Fact]
    public async Task UltimaLeitura_RespeitaInstituicaoEExistencia()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        var deOutra = await cliente.GetAsync("/api/equipamentos/1/leituras/ultima");
        Assert.Equal(HttpStatusCode.Forbidden, deOutra.StatusCode);

        var inexistente = await cliente.GetAsync("/api/equipamentos/9999/leituras/ultima");
        Assert.Equal(HttpStatusCode.NotFound, inexistente.StatusCode);
    }

    /// <summary>
    /// "7 dias por padrão" (README e plano de testes) com o fim inclusivo: hoje e os 6 dias anteriores.
    /// O período sai no nome do arquivo, então o teste não depende do relógio.
    /// </summary>
    [Fact]
    public async Task Relatorio_SemDatas_CobreSeteDiasContandoHoje()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        var resposta = await cliente.GetAsync("/api/equipamentos/4/relatorio");

        Assert.Equal(HttpStatusCode.OK, resposta.StatusCode);
        var nome = resposta.Content.Headers.ContentDisposition?.FileName?.Trim('"');
        Assert.NotNull(nome);

        var partes = Path.GetFileNameWithoutExtension(nome!).Split('-');
        var inicio = DateOnly.ParseExact(partes[^2], "yyyyMMdd");
        var fim = DateOnly.ParseExact(partes[^1], "yyyyMMdd");
        Assert.Equal(7, fim.DayNumber - inicio.DayNumber + 1);
    }

    /// <summary>Lote nasce DISPONIVEL na instituicao do token; a retirada o tira do estoque uma vez so.</summary>
    [Fact]
    public async Task Lotes_CadastraDisponivel_RegistraRetiradaENaoRepete()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 1);

        var novo = new LoteRequest(
            "Caixas de tomate italiano", "HORTIFRUTI", 120.5m, DateOnly.FromDateTime(DateTime.UtcNow.AddDays(5)));

        var resposta = await cliente.PostAsJsonAsync("/api/lotes", novo, JsonDaApi.Opcoes);
        Assert.Equal(HttpStatusCode.Created, resposta.StatusCode);

        var lote = await resposta.Content.ReadFromJsonAsync<LoteResponse>(JsonDaApi.Opcoes);
        Assert.NotNull(lote);
        Assert.Equal(StatusLote.DISPONIVEL, lote!.Status);
        Assert.Equal(1, lote.InstituicaoId);
        Assert.Null(lote.RetiradoEm);

        var retirada = await cliente.PatchAsJsonAsync(
            $"/api/lotes/{lote.Id}/retirada", new RetiradaDeLoteRequest(StatusLote.DISTRIBUIDO), JsonDaApi.Opcoes);
        Assert.Equal(HttpStatusCode.OK, retirada.StatusCode);

        var saiu = await retirada.Content.ReadFromJsonAsync<LoteResponse>(JsonDaApi.Opcoes);
        Assert.Equal(StatusLote.DISTRIBUIDO, saiu!.Status);
        Assert.NotNull(saiu!.RetiradoEm);

        var deNovo = await cliente.PatchAsJsonAsync(
            $"/api/lotes/{lote.Id}/retirada", new RetiradaDeLoteRequest(StatusLote.PERDIDO), JsonDaApi.Opcoes);
        Assert.Equal(HttpStatusCode.Conflict, deNovo.StatusCode);
    }

    /// <summary>VOLUNTARIO não cadastra lote (mesma regra de escrita do CRUD) e lote alheio é 403.</summary>
    [Fact]
    public async Task Lotes_VoluntarioNaoCadastraELoteDeOutraInstituicao_403()
    {
        using var app = new ApiDeTeste();

        var voluntario = app.Cliente(PapelUsuario.VOLUNTARIO, instituicaoId: 1);
        var tentativa = await voluntario.PostAsJsonAsync(
            "/api/lotes",
            new LoteRequest("Doação do voluntário", "FRUTAS", 5m, DateOnly.FromDateTime(DateTime.UtcNow.AddDays(3))),
            JsonDaApi.Opcoes);
        Assert.Equal(HttpStatusCode.Forbidden, tentativa.StatusCode);

        var cliente = app.Cliente(instituicaoId: 1);
        var alheio = await cliente.GetAsync("/api/lotes/1");
        Assert.Equal(HttpStatusCode.Forbidden, alheio.StatusCode);

        var dono = app.Cliente(instituicaoId: 2);
        var proprio = await dono.GetAsync("/api/lotes/1");
        Assert.Equal(HttpStatusCode.OK, proprio.StatusCode);
    }

    /// <summary>O filtro `?ativo=` esconde o equipamento desativado, que é o que o painel do app precisa.</summary>
    [Fact]
    public async Task Equipamentos_FiltroAtivo_EscondeDesativado()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente(instituicaoId: 2);

        var desativar = await cliente.DeleteAsync("/api/equipamentos/4");
        Assert.Equal(HttpStatusCode.NoContent, desativar.StatusCode);

        var todos = await cliente.GetFromJsonAsync<List<EquipamentoResponse>>("/api/equipamentos", JsonDaApi.Opcoes);
        Assert.Single(todos!);

        var ativos = await cliente.GetFromJsonAsync<List<EquipamentoResponse>>(
            "/api/equipamentos?ativo=true", JsonDaApi.Opcoes);
        Assert.Empty(ativos!);

        var inativos = await cliente.GetFromJsonAsync<List<EquipamentoResponse>>(
            "/api/equipamentos?ativo=false", JsonDaApi.Opcoes);
        Assert.Single(inativos!);
    }

    /// <summary>Abre um alerta pela regra (3 leituras acima da faixa da câmara) e devolve o id dele.</summary>
    private static async Task<long> AbrirAlertaAsync(HttpClient cliente)
    {
        LeituraRegistradaResponse? corpo = null;
        for (var i = 0; i < 3; i++)
        {
            var resposta = await cliente.PostAsJsonAsync("/api/leituras", Leitura(1, 7.8m, i), JsonDaApi.Opcoes);
            corpo = await resposta.Content.ReadFromJsonAsync<LeituraRegistradaResponse>(JsonDaApi.Opcoes);
        }

        return Assert.Single(corpo!.AlertasAbertos).Id;
    }

    /// <summary>A ação escolhida no app fica gravada no alerta e aparece no histórico de resolvidos.</summary>
    [Fact]
    public async Task Resolver_ComAcao_GravaEDevolveAAcaoTomada()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente();
        var id = await AbrirAlertaAsync(cliente);

        var resposta = await cliente.PatchAsJsonAsync(
            $"/api/alertas/{id}/resolver", new ResolverAlertaRequest(AcaoTomada.TRANSFERIU_LOTE), JsonDaApi.Opcoes);

        Assert.Equal(HttpStatusCode.OK, resposta.StatusCode);
        var resolvido = await resposta.Content.ReadFromJsonAsync<AlertaResponse>(JsonDaApi.Opcoes);
        Assert.Equal(StatusAlerta.RESOLVIDO, resolvido!.Status);
        Assert.Equal(AcaoTomada.TRANSFERIU_LOTE, resolvido.AcaoTomada);

        var historico = await cliente.GetFromJsonAsync<List<AlertaResponse>>(
            "/api/alertas?status=RESOLVIDO", JsonDaApi.Opcoes);
        Assert.Equal(AcaoTomada.TRANSFERIU_LOTE, Assert.Single(historico!).AcaoTomada);
    }

    /// <summary>Compatibilidade: quem ainda chama o PATCH sem corpo continua resolvendo, sem ação.</summary>
    [Fact]
    public async Task Resolver_SemCorpo_ContinuaFuncionando()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente();
        var id = await AbrirAlertaAsync(cliente);

        var resposta = await cliente.PatchAsync($"/api/alertas/{id}/resolver", content: null);

        Assert.Equal(HttpStatusCode.OK, resposta.StatusCode);
        var resolvido = await resposta.Content.ReadFromJsonAsync<AlertaResponse>(JsonDaApi.Opcoes);
        Assert.Equal(StatusAlerta.RESOLVIDO, resolvido!.Status);
        Assert.Null(resolvido.AcaoTomada);
    }

    /// <summary>Ação fora do vocabulário é 400 e o alerta continua aberto.</summary>
    [Fact]
    public async Task Resolver_ComAcaoForaDoVocabulario_Devolve400()
    {
        using var app = new ApiDeTeste();
        var cliente = app.Cliente();
        var id = await AbrirAlertaAsync(cliente);

        var resposta = await cliente.PatchAsync(
            $"/api/alertas/{id}/resolver",
            new StringContent("{\"acao\":\"QUEIMOU\"}", System.Text.Encoding.UTF8, "application/json"));

        Assert.Equal(HttpStatusCode.BadRequest, resposta.StatusCode);
        Assert.Equal(StatusAlerta.ABERTO, Assert.Single(app.Alertas.Itens).Status);
    }
}
