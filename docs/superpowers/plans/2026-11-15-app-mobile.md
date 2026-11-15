# App mobile Frio Solidário Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o app Android (5 telas + PDF) integrado à API real, cobrindo os 7 cards "App mobile" do board.

**Architecture:** Projeto Gradle em `mobile/`, Activity única com Navigation Compose, um ViewModel por tela sobre repositórios Retrofit, injeção manual por `AppContainer`. Todo acesso à rede passa por `chamar { }`, que converte exceção e status HTTP em `ErroApp`; 401 limpa a sessão e a navegação volta ao login. A API ganha um corpo opcional no PATCH de resolução para gravar a ação tomada.

**Tech Stack:** Kotlin 2.2, Jetpack Compose (BOM 2025.12), Material 3, Navigation Compose, Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization, DataStore Preferences, JUnit 4 + MockWebServer + coroutines-test. Backend: .NET 10, EF Core, xUnit.

**Spec:** `docs/superpowers/specs/2026-11-15-app-mobile-design.md`

## Global Constraints

- Pacote `br.com.fiap.friosolidario`; minSdk 24; compileSdk/targetSdk 36.
- Nenhum dado fixo de domínio no app: equipamentos, leituras, alertas e lotes vêm só da API.
- URL da API em um único ponto: `core/ApiConfig.kt` (valor em `frio.apiUrl`, `mobile/gradle.properties`, padrão `http://10.0.2.2:5199/`).
- Timeouts de rede: 10 s (connect, read, write).
- Interface em português; datas da API (UTC) exibidas no fuso do aparelho.
- A mudança na API é compatível: `PATCH /api/alertas/{id}/resolver` sem corpo continua devolvendo 200.
- Branch `feat/app-mobile`; commit por alteração relevante; `git push` ao fim de cada task; sem merge na `main`.
- Mensagens de commit no padrão do repositório (`feat(app): ...`, `feat(api): ...`, `docs(...): ...`).

## Review Focus

1. Token vencido no meio do uso (401 em tela interna): o app volta ao login com aviso, sem crash e sem laço de requisições. Teste em Task 2 (`chamar` com 401) e verificação manual na Task 8.
2. Equipamento sem nenhuma leitura (recém-cadastrado): painel mostra cinza "sem leitura", detalhe mostra estado vazio em vez de gráfico quebrado. Teste em Task 3 (`semaforo(null)`), tela na Task 4.
3. Série com uma única leitura ou com todas as temperaturas iguais: o gráfico não divide por zero. Teste em Task 4 (`EscalaDoGrafico`).
4. Número digitado com vírgula (`-18,5`) nos formulários: aceito como decimal. Teste em Task 6 (`paraDecimal`).
5. Corpo de erro que não é ProblemDetails (HTML de proxy, corpo vazio): mensagem genérica, sem exceção de parse. Teste em Task 2.

---

### Task 0: Ambiente local (nada é commitado)

- [ ] Instalar .NET SDK 10 em `~/.dotnet` com `dotnet-install.sh --channel 10.0`; `~/.dotnet/dotnet --version` imprime `10.`.
- [ ] Subir um PostgreSQL de usuário (sem sudo) em porta local, criar o banco e carregar `db/schema.sql` e `db/seed.sql` conforme a seção "Como carregar o banco" do README.
- [ ] `dotnet test` na raiz: 57 aprovados (linha de base).
- [ ] API rodando em `http://0.0.0.0:5199`; `curl /api/health` mostra `"origemDosDados":"6 equipamentos no banco"`.

### Task 1: API grava a ação tomada na resolução do alerta

**Files:**
- Modify: `src/FrioSolidario.Domain/Enums.cs`, `src/FrioSolidario.Domain/Entities/Alerta.cs`
- Modify: `src/FrioSolidario.Application/DTOs/AlertaDtos.cs`, `Services/IAlertaService.cs`, `Services/AlertaService.cs`
- Modify: `src/FrioSolidario.Api/Controllers/AlertasController.cs`
- Modify: `src/FrioSolidario.Infrastructure/Persistence/FrioSolidarioDbContext.cs`
- Modify: `db/schema.sql`, `README.md` (contrato do app, item 5), `docs/der.md` se listar as colunas de `alerta`
- Test: `tests/FrioSolidario.Tests/ApiTests.cs`

**Interfaces:**
- Produces: `enum AcaoTomada { AJUSTOU, TRANSFERIU_LOTE, DESCARTOU }`; `record ResolverAlertaRequest(AcaoTomada? Acao)`; `AlertaResponse.AcaoTomada` (`AcaoTomada?`, JSON `acaoTomada`); coluna `alerta.acao_tomada VARCHAR(16) NULL` com `CHECK` nos três valores e `CHECK` de que só existe com `status = 'RESOLVIDO'`.
- `IAlertaService.ResolverAsync(long id, long resolvidoPorId, long? instituicaoDoToken = null, AcaoTomada? acao = null, CancellationToken ct = default)`.

- [ ] **Step 1:** Testes que falham em `ApiTests.cs`: `Resolver_ComAcao_GravaEDevolveAAcaoTomada` (PATCH com `{"acao":"TRANSFERIU_LOTE"}` → 200, `acaoTomada == "TRANSFERIU_LOTE"`, e `GET /api/alertas?status=RESOLVIDO` traz o mesmo valor); `Resolver_SemCorpo_ContinuaFuncionando` (200, `acaoTomada == null`); `Resolver_ComAcaoForaDoVocabulario_Devolve400` (`{"acao":"QUEIMOU"}`).
- [ ] **Step 2:** `dotnet test --filter Resolver_` falha.
- [ ] **Step 3:** Implementar. No controller o corpo é opcional: `[FromBody] ResolverAlertaRequest? request = null` com `EmptyBodyBehavior.Allow`.
- [ ] **Step 4:** `dotnet test`: 60 aprovados.
- [ ] **Step 5:** Aplicar a coluna no banco local, conferir com `curl` contra a API real, atualizar README. Commit `feat(api): acao tomada opcional na resolucao do alerta` e push.

### Task 2: Projeto do app, núcleo de rede/sessão/erro e tela de login (card 1)

**Files (todos em `mobile/`):**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, wrapper do Gradle, `.gitignore`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `res/` (strings, tema, ícone, `xml/network_security_config.xml`, `xml/file_paths.xml`)
- Create em `app/src/main/java/br/com/fiap/friosolidario/`:
  - `core/ApiConfig.kt` — `object ApiConfig { val BASE_URL: String; const val TIMEOUT_SEGUNDOS = 10L }`
  - `core/ErroApp.kt`, `core/Chamada.kt`, `core/Sessao.kt`, `core/SessaoStore.kt`, `core/Rede.kt`, `core/AppContainer.kt`, `core/Formatos.kt`
  - `data/Dtos.kt`, `data/FrioApi.kt`, `data/AuthRepository.kt`
  - `ui/tema/Tema.kt`, `ui/comum/Estados.kt`, `ui/login/LoginViewModel.kt`, `ui/login/LoginScreen.kt`, `ui/Navegacao.kt`, `MainActivity.kt`, `FrioApp.kt`
- Test: `app/src/test/java/br/com/fiap/friosolidario/core/ChamadaTest.kt`, `data/AuthRepositoryTest.kt`, `ui/login/LoginValidacaoTest.kt`

**Interfaces (Produces):**
- `sealed interface ErroApp { val mensagem: String }` com `SemConexao`, `SessaoExpirada`, `CredenciaisInvalidas`, `SemPermissao`, `Validacao(mensagem)`, `NaoEncontrado`, `Conflito(mensagem)`, `MuitasTentativas`, `Servidor`, `Inesperado`.
- `sealed interface Resultado<out T> { data class Ok<T>(val valor: T); data class Falha(val erro: ErroApp) }`
- `suspend fun <T> chamar(aoExpirar: suspend () -> Unit = {}, bloco: suspend () -> T): Resultado<T>` — `IOException` (timeout, host inacessível) → `SemConexao`; `HttpException` por status: 400 → `Validacao(detail ou primeira mensagem de errors)`, 401 → `SessaoExpirada` (chama `aoExpirar`), 403 → `SemPermissao`, 404 → `NaoEncontrado`, 409 → `Conflito(detail)`, 429 → `MuitasTentativas`, 5xx → `Servidor`; qualquer outra → `Inesperado`.
- `data class Sessao(token, expiraEm: Instant, nome, papel: Papel, instituicaoId: Long, instituicao)`; `enum class Papel { ADMIN, VOLUNTARIO }`; `val Sessao.ehAdmin`.
- `class SessaoStore(context)`: `val sessao: Flow<Sessao?>`, `suspend fun salvar(Sessao)`, `suspend fun limpar()`, `fun tokenAtual(): String?` (cache em memória para o interceptor).
- `interface FrioApi` (Retrofit) com todas as rotas do contrato: `login`, `equipamentos(ativo)`, `equipamento(id)`, `criarEquipamento`, `leituras(id, periodo)`, `alertas(status)`, `resolverAlerta(id, body)`, `lotes(status)`, `criarLote`, `retirarLote`, `relatorio(id, de, ate): ResponseBody` (`@Streaming`).
- `class AuthRepository`: `suspend fun entrar(email, senha): Resultado<Sessao>` (401 aqui vira `CredenciaisInvalidas`), `suspend fun sair()`.
- `ui/comum/Estados.kt`: `@Composable Carregando()`, `ErroComTentarDeNovo(erro: ErroApp, aoTentar: () -> Unit)`, `ListaVazia(texto: String)`, `sealed interface EstadoTela<out T> { Carregando; Erro(erro); Conteudo(valor) }`.
- `fun validarLogin(email: String, senha: String): String?` — vazio → "Informe o e-mail e a senha."; e-mail sem `@` → "E-mail inválido."; senha < 6 → "A senha tem pelo menos 6 caracteres.".

- [ ] **Step 1:** Scaffold do Gradle (versões do catálogo: AGP e Kotlin iguais aos do app da Vinheria, que já compilam nesta máquina) e `./gradlew help` passa.
- [ ] **Step 2:** Testes que falham: `ChamadaTest` (um caso por linha da tabela de status acima, mais: corpo HTML em 500 → `Servidor`; 400 com `errors` → primeira mensagem; 400 com corpo vazio → mensagem genérica; `SocketTimeoutException` → `SemConexao`; 401 chama `aoExpirar` uma vez), `AuthRepositoryTest` com MockWebServer (200 salva a sessão; 401 → `CredenciaisInvalidas` e não limpa nada; servidor desligado → `SemConexao`), `LoginValidacaoTest`.
- [ ] **Step 3:** Implementar núcleo e repositório; `./gradlew testDebugUnitTest` passa.
- [ ] **Step 4:** Tela de login: e-mail, senha (mostrar/ocultar), botão "Entrar" desabilitado e com indicador durante a requisição, mensagem distinta para campo vazio, 401, 429 e sem conexão (esta com "Tentar de novo"). `Navegacao.kt` decide a rota inicial pela sessão salva e volta ao login quando a sessão vira `null`.
- [ ] **Step 5:** `./gradlew assembleDebug`, instalar no aparelho, conferir os três "pronto quando" do card. Commits separados (scaffold+núcleo; login) e push.

### Task 3: Painel de equipamentos com semáforo (card 2)

**Files:** Create `dominio/Semaforo.kt`, `data/EquipamentoRepository.kt`, `ui/painel/PainelViewModel.kt`, `ui/painel/PainelScreen.kt`. Test: `dominio/SemaforoTest.kt`, `data/EquipamentoRepositoryTest.kt`.

**Interfaces (Produces):**
- `enum class Semaforo { VERDE, AMARELO, VERMELHO, SEM_LEITURA }`
- `fun semaforo(equipamento: EquipamentoDto, ultima: LeituraDto?): Semaforo` — regra da tabela "Semáforo" do spec.
- `data class EquipamentoNoPainel(equipamento, ultimaLeitura: LeituraDto?, semaforo)`
- `EquipamentoRepository.painel(): Resultado<List<EquipamentoNoPainel>>` — `GET /equipamentos?ativo=true`, depois em paralelo a série de 24h de cada um (7d se vazia); falha na série de um equipamento não derruba a lista (fica `SEM_LEITURA`), mas 401 propaga.
- `EquipamentoRepository.equipamento(id)`, `leituras(id, periodo: Periodo)`, `criar(...)`; `enum class Periodo(val api: String) { H24("24h"), D7("7d") }`.

- [ ] **Step 1:** `SemaforoTest`: faixa 2..8 → 5,0 verde; 8,1 vermelho; 1,9 vermelho; 8,0 amarelo (limite ainda é dentro); 7,5 amarelo (margem 0,6); 7,3 verde; umidade acima de `umidMax` amarelo; porta aberta amarelo; `null` → `SEM_LEITURA`; faixa estreita -19..-18 usa margem mínima 0,5. `EquipamentoRepositoryTest`: última leitura é a última da série; 24h vazia consulta 7d; erro 500 em uma série mantém os outros.
- [ ] **Step 2:** Testes falham; implementar; testes passam.
- [ ] **Step 3:** Tela: cartão por equipamento (cor + rótulo textual do estado, nome, tipo, temperatura, faixa, horário do dado como "há 4 min · 14:32"), resumo no topo ("2 fora da faixa" / "Tudo dentro da faixa"), pull-to-refresh, estado vazio "Nenhum equipamento cadastrado", grade adaptativa em tablet. Barra superior: alertas, lotes, sair.
- [ ] **Step 4:** Conferir no aparelho os "pronto quando". Commit e push.

### Task 4: Detalhe do equipamento com gráfico e faixa (card 3)

**Files:** Create `dominio/EscalaDoGrafico.kt`, `ui/detalhe/GraficoTemperatura.kt`, `ui/detalhe/DetalheViewModel.kt`, `ui/detalhe/DetalheScreen.kt`. Test: `dominio/EscalaDoGraficoTest.kt`.

**Interfaces (Produces):**
- `data class EscalaDoGrafico(val minY: Double, val maxY: Double, val inicio: Instant, val fim: Instant)` com `fun x(t: Instant, largura: Float): Float`, `fun y(v: Double, altura: Float): Float`.
- `fun escalaPara(leituras: List<LeituraDto>, tempMin: Double, tempMax: Double): EscalaDoGrafico` — o eixo Y sempre inclui os dois limites e todos os pontos, com folga de 10%; série de um ponto ou valores iguais não gera intervalo zero.
- `fun foraDaFaixa(l: LeituraDto, e: EquipamentoDto): Boolean`.

- [ ] **Step 1:** Testes da escala (um ponto; todos iguais; ponto acima do limite entra no eixo; `y(maxY) == 0`, `y(minY) == altura`). Falham; implementar; passam.
- [ ] **Step 2:** Tela: cabeçalho com estado atual, seletor 24h/7d (troca só a série, o cabeçalho fica), gráfico em Canvas (linha, limites tracejados com rótulo, pontos fora da faixa em vermelho, rótulos de eixo), tabela das últimas 20 leituras com as fora da faixa marcadas, estado vazio "Sem leituras no período".
- [ ] **Step 3:** Conferir no aparelho. Commit e push.

### Task 5: Lista de alertas com resolução (card 4)

**Files:** Create `data/AlertaRepository.kt`, `ui/alertas/AlertasViewModel.kt`, `ui/alertas/AlertasScreen.kt`. Test: `data/AlertaRepositoryTest.kt`.

**Interfaces:** `AlertaRepository.abertos()`, `historico()` (`status=RESOLVIDO`, mais recente primeiro), `resolver(id, acao: AcaoTomada)`; `enum class AcaoTomada { AJUSTOU, TRANSFERIU_LOTE, DESCARTOU }` com rótulos "Ajustei o equipamento", "Transferi o lote", "Descartei o lote".

- [ ] **Step 1:** Teste: `resolver` envia `{"acao":"TRANSFERIU_LOTE"}` no corpo do PATCH; 409 → `Conflito`. Falha; implementar; passa.
- [ ] **Step 2:** Tela: abas Abertos/Histórico, item com tipo, severidade (cor + texto), equipamento e tempo decorrido; diálogo de resolução com as três ações; o item sai da fila e aparece no histórico com a ação; atualização automática a cada 10 s enquanto a tela está visível; contador de alertas abertos no ícone do painel.
- [ ] **Step 3:** Conferir no aparelho com leituras fora da faixa publicadas por `curl`. Commit e push.

### Task 6: Cadastro de equipamento + lote doado (card 5)

**Files:** Create `dominio/Validacoes.kt`, `data/LoteRepository.kt`, `ui/cadastro/EquipamentoFormScreen.kt` + ViewModel, `ui/lotes/LotesScreen.kt`, `ui/lotes/LoteFormScreen.kt` + ViewModels. Test: `dominio/ValidacoesTest.kt`.

**Interfaces:**
- `fun paraDecimal(texto: String): Double?` — aceita vírgula ou ponto, sinal e espaços nas pontas.
- `fun validarEquipamento(nome, tempMin, tempMax, umidMax: String): Map<Campo, String>` — nome 3..150; temperaturas entre -40 e 80; `tempMin < tempMax` ("A mínima precisa ser menor que a máxima."); umidade > 0 e ≤ 100.
- `fun validarLote(descricao, categoria, kg, validade: LocalDate?): Map<Campo, String>` — descrição 3..200, categoria 3..80, kg > 0, validade obrigatória.

- [ ] **Step 1:** Testes das validações (inclui `"-18,5"` → -18.5, `"abc"` → null, min == max inválido). Falham; implementar; passam.
- [ ] **Step 2:** Formulários com erro por campo, botão desabilitado durante o envio, mensagem do 400/403 da API exibida sem perder o que foi digitado; ao salvar, volta e a lista de origem recarrega. Ações de cadastro visíveis só para ADMIN. Lista de lotes com saída `DISTRIBUIDO`/`PERDIDO`.
- [ ] **Step 3:** Conferir no aparelho (inclusive forçando um 400 da API). Commit e push.

### Task 7: Exportar/imprimir o relatório em PDF (card 6)

**Files:** Create `data/RelatorioRepository.kt`, `ui/detalhe/RelatorioPdf.kt` (diálogo de período + compartilhar/imprimir). Test: `data/RelatorioRepositoryTest.kt`.

**Interfaces:** `RelatorioRepository.baixar(equipamentoId, de: LocalDate, ate: LocalDate): Resultado<File>` — grava em `cacheDir/relatorios/`, nome do `Content-Disposition` (ou `relatorio-<id>-<de>-<ate>.pdf`); `fun validarPeriodo(de, ate): String?` — `de <= ate`, no máximo 92 dias.

- [ ] **Step 1:** Teste com MockWebServer: bytes gravados iguais aos enviados; query `de`/`ate` em `yyyy-MM-dd`; 400 → `Validacao`; período de 93 dias recusado sem chamar a rede. Falha; implementar; passa.
- [ ] **Step 2:** Botão "Relatório PDF" no detalhe; diálogo com 7 dias (padrão), 30 dias ou datas; depois do download, "Compartilhar" (ACTION_SEND via FileProvider) e "Imprimir" (PrintManager); erro de rede com mensagem e tentar de novo.
- [ ] **Step 3:** Conferir no aparelho que o PDF abre. Commit e push.

### Task 8: Integração real, erro de conexão e fechamento (card 7)

- [ ] Varredura: `grep` por literais de domínio no app (nomes de equipamento, temperaturas, e-mails) fora de testes; nenhum resultado.
- [ ] Roteiro no aparelho com evidência em `docs/evidencias/app/` (capturas por `adb exec-out screencap`): API desligada em cada uma das 5 telas; token expirado; instituição sem equipamentos; fonte do sistema no máximo em tela pequena; largura de tablet (`adb shell wm size`).
- [ ] README: seção "App mobile" (como compilar, onde trocar a URL, usuários de demo), "Estado atual" e "O que ainda não roda" atualizados, linha do Kevin na tabela de uso de IA.
- [ ] `./gradlew testDebugUnitTest assembleDebug` e `dotnet test` verdes. Commit e push.
