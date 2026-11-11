# Plano de testes — Frio Solidário

Global Solution 2026/2 — FIAP 3ESOA. Responsável: Arthur (frente Segurança + Testes).
Este plano tem **9 casos**. Cada caso traz pré-condição, passos, resultado esperado e a evidência a
coletar, e cada um aponta para o trecho do README ou da regra de alerta que ele verifica. A execução
(com evidência) é o card seguinte (15/11); o caso 5 e o caso 6 também viram testes xUnit (15→16/11).

## 1. Escopo e critério

O que se testa é o que o produto promete: o sensor envia leituras, a API compara com a faixa do
equipamento e abre alerta para o voluntário no app, com acesso controlado por login e perfil. Os 8
casos cobrem as cinco promessas do projeto:

| Promessa | Casos |
|---|---|
| Só entra quem tem login, e a senha não fica exposta (BCrypt, JWT) | 1, 7 |
| Cada perfil faz só o que lhe cabe (ADMIN × VOLUNTARIO) e cada instituição vê só o seu | 2, 7 |
| Cadastro e manutenção dos equipamentos e suas faixas | 3, 4 |
| Regra de faixa: 3 leituras seguidas fora da faixa abrem alerta, 1 ou 2 não | 5, 6 |
| O voluntário recebe a fila de alertas, resolve no local e fica registrado quem resolveu | 8 |
| O período atendido vira um log térmico em PDF, no horário de Brasília, para a fiscalização | 9 |

Um caso **passa** quando todos os resultados esperados dos seus passos acontecem. **Falha** quando um
deles não acontece, e a falha é registrada com o que a API devolveu de fato.

## 2. Ambiente

| Item | Valor |
|---|---|
| API | `dotnet run --project src/FrioSolidario.Api`, em `http://localhost:5199` (Swagger em `/swagger`) |
| Banco | PostgreSQL 17 com `db/schema.sql` + `db/seed.sql` recém-carregados |
| Ferramentas | Swagger (botão **Authorize**), `curl`, `psql` para conferir o banco; app do Kevin nos casos 3 e 8 |
| Código | com os cards de segurança aplicados (BCrypt, perfis, JWT, isolamento por instituição) |

**Recarregue o banco antes de cada rodada completa.** As leituras do seed usam `CURRENT_TIMESTAMP`,
então a série das últimas 24 h (caso 4) só traz as 5 leituras de cada equipamento se o seed foi
carregado há menos de um dia. Os casos dependem do estado do seed e alteram o banco (o caso 3 cria um
equipamento, o 6 abre um alerta, o 8 resolve outro).

No `curl` do Windows, corpos com acento (como "proteínas") chegam quebrados e a API responde 400.
Use o Swagger ou escreva o acento escapado (`proteínas`).

### Dados de teste (do seed)

Senha de todos os usuários: **`Frio@2026`** (demonstrativa, documentada no README).

| Usuário | Papel | Instituição |
|---|---|---|
| mariana.costa@caminhosdobem.example | ADMIN | 1 — Banco de Alimentos Caminhos do Bem |
| ana.rocha@caminhosdobem.example | VOLUNTARIO | 1 |
| rafael.nunes@bairrosolidario.example | ADMIN | 2 — Mercado Bairro Solidário |
| bruna.oliveira@bairrosolidario.example | VOLUNTARIO | 2 |

| Equipamento | Instituição | Faixa (°C) | Umidade máx. | Situação no seed |
|---|---|---|---|---|
| 1 Câmara fria de hortifrúti | 1 | 0 a 4 | 85 % | alerta 1 aberto: `TEMP_ALTA`, `CRITICA` |
| 2 Freezer de proteínas | 1 | -25 a -18 | 90 % | alerta 2 aberto: `TEMP_BAIXA`, `ALTA` |
| 3 Balcão refrigerado de doações | 2 | 0 a 5 | 80 % | alerta 3 aberto: `PORTA_ABERTA`, `MEDIA` |
| 4 Freezer de congelados | 2 | -25 a -18 | 85 % | funcionamento normal, **sem alerta** |

### Ordem de execução

Execute na ordem **1, 2, 3, 4, 5, 6, 8, 9, 7**. O caso 4 usa o equipamento criado no caso 3, o 6 parte da
sequência deixada pelo 5, e o último passo do caso 7 esgota o limite de tentativas de login (o IP fica
bloqueado por 1 minuto), então ele vai por último.

**Cada login conta no limite de 10 por minuto**, não só os do caso 7: emendar casos sem esperar faz um
login legítimo voltar **429** no meio da execução (e sem token, o passo seguinte vira 401 e parece
falha do sistema). Duas saídas: reaproveitar o token já obtido no caso 1, ou esperar 1 minuto entre
blocos de casos. Confira antes de culpar a API: `detail` "Limite de tentativas de login atingido".

## 3. Os 9 casos

Convenções: `{{token-X}}` é o token devolvido pelo login do usuário X (caso 1), enviado como
`Authorization: Bearer <token>`. Mensagens e títulos de erro são os que a API devolve em
`ProblemDetails`.

---

### Caso 1 — Login com senha protegida por BCrypt

| | |
|---|---|
| **Verifica** | README, "Login (demo)": BCrypt no `senha_hash`, mesmo 401 para e-mail inexistente e senha errada, token com papel e instituição |
| **Pré-condição** | Banco com o seed; API no ar |
| **Automação** | Execução manual (Swagger ou `curl`) |

**Passos e resultado esperado**

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | No banco: `SELECT email, left(senha_hash,7), length(senha_hash) FROM usuario;` | 6 linhas, todas com prefixo `$2a$10$` e tamanho 60. Nenhuma senha em texto puro |
| 2 | `POST /api/auth/login` com `mariana.costa@caminhosdobem.example` e `Frio@2026` | **200**. Corpo com `token`, `expiraEm` (cerca de 8 h à frente), `nome` "Mariana Costa", `papel` "ADMIN", `instituicaoId` 1, `instituicao` "Banco de Alimentos Caminhos do Bem" |
| 3 | Mesmo e-mail com senha `errada123` | **401**, `ProblemDetails` com `detail` "E-mail ou senha invalidos." |
| 4 | E-mail inexistente (`ninguem@x.example`) com `Frio@2026` | **401** com **o mesmo** `detail` do passo 3 (a API não revela quais e-mails existem) |
| 5 | E-mail em formato inválido (`nao-e-email`) | **400** de validação, sem consultar a senha |

**Evidência:** resultado do passo 1 no `psql`; resposta dos passos 2 a 5 (Swagger ou `curl`), com o
status visível.

---

### Caso 2 — Perfis: ADMIN escreve, VOLUNTARIO não

| | |
|---|---|
| **Verifica** | README, "Endpoints": escritas do CRUD exigem `ADMIN` (401 sem token, 403 para `VOLUNTARIO`) |
| **Pré-condição** | Tokens de `ana.rocha` (VOLUNTARIO, instituição 1) e `mariana.costa` (ADMIN, instituição 1) obtidos no caso 1 |
| **Automação** | Execução manual |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `POST /api/equipamentos` sem token | **401** |
| 2 | `POST /api/equipamentos` com `{{token-ana}}`, corpo válido (instituição 1) | **403**, `detail` "O perfil do usuario nao permite esta operacao." |
| 3 | `DELETE /api/equipamentos/2` com `{{token-ana}}` | **403**. O equipamento 2 continua existindo |
| 4 | `PUT /api/instituicoes/1` com `{{token-ana}}` | **403** |
| 5 | `GET /api/alertas?status=ABERTO` com `{{token-ana}}` | **200**: o voluntário opera a fila de alertas |
| 6 | `GET /api/equipamentos` com `{{token-ana}}` | **200**: o voluntário consulta os equipamentos |
| 7 | `PUT /api/equipamentos/2` com `{{token-mariana}}` e os mesmos dados que o equipamento já tem (não altera nada) | **200**: o ADMIN da instituição passa pela escrita que o voluntário não passou |

**Evidência:** respostas dos 7 passos com o status; no passo 3, `GET /api/equipamentos/2` depois,
mostrando que o equipamento está intacto.

---

### Caso 3 — Cadastro de equipamento com faixa de temperatura

| | |
|---|---|
| **Verifica** | README, "Endpoints" e `EquipamentoRequest`: temp_min < temp_max, umidade até 100, equipamento nasce ativo e aparece na lista |
| **Pré-condição** | `{{token-mariana}}` (ADMIN, instituição 1). Caso realizado pelo **app** (tela de cadastro, Kevin) e repetido pela API |
| **Automação** | Execução manual |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `POST /api/equipamentos` com `{"instituicaoId":1,"nome":"Freezer de teste","tipo":"FREEZER","tempMin":-22,"tempMax":-15,"umidMax":80}` | **201**, corpo com `id` novo (7), `instituicao` "Banco de Alimentos Caminhos do Bem", `ativo` true. Guarde o `id` |
| 2 | `GET /api/equipamentos?instituicaoId=1` | **200**, a lista traz o equipamento novo junto com os equipamentos 1 e 2. Quem filtra é a instituição **do token**; o `instituicaoId` na query é ignorado hoje (não serve para ver a instituição de outro usuário) |
| 3 | `POST` com `tempMin` 8 e `tempMax` 2 | **400** (`detail` "Temperatura minima (8) deve ser menor que a maxima (2).") |
| 4 | `POST` com `umidMax` 101 | **400** de validação ("Umidade maxima deve ficar entre 0,01 e 100.") |
| 5 | `POST` com `"tipo":"AQUARIO"` | **400** (tipo fora de `CAMARA`, `GELADEIRA`, `FREEZER`) |
| 6 | No banco: `SELECT nome, temp_min, temp_max FROM equipamento WHERE id = 7;` | 1 linha com os valores enviados no passo 1 |

**Evidência:** print da tela de cadastro e da lista no app (ou o Swagger), resposta do passo 1 e do
`psql` do passo 6. Os passos 3 a 5 mostram que o cadastro recusa faixa impossível.

---

### Caso 4 — Manter o equipamento: editar, remover e consultar a série

| | |
|---|---|
| **Verifica** | README, "Endpoints": PUT, DELETE (409 com leituras) e `GET /api/equipamentos/{id}/leituras` |
| **Pré-condição** | Caso 3 executado (equipamento 7 existe); `{{token-mariana}}` |
| **Automação** | Execução manual |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `PUT /api/equipamentos/7` com a faixa trocada para `-25` a `-18` | **200**, corpo com `tempMin` -25 e `tempMax` -18 |
| 2 | `GET /api/equipamentos/7` | **200**, mostra a faixa nova |
| 3 | `GET /api/equipamentos/1/leituras?periodo=24h` | **200**, **5 leituras** em ordem cronológica (a mais antiga primeiro), cada uma com `temperatura`, `umidade`, `portaAberta`, `registradaEm` e `origem` |
| 4 | `GET /api/equipamentos/1/leituras?periodo=1h` | **400** ("periodo aceita apenas 24h ou 7d.") |
| 5 | `DELETE /api/equipamentos/1` (tem leituras e alerta) | **409**. O equipamento 1 continua na lista |
| 6 | `DELETE /api/equipamentos/7` (sem leituras) | **204** |
| 7 | `GET /api/equipamentos/7` | **404** |

**Evidência:** respostas dos passos 3, 5, 6 e 7; no passo 3, o painel do app com o gráfico, se
disponível.

---

### Caso 5 — Regra de faixa: 1 ou 2 leituras fora da faixa **não** abrem alerta

| | |
|---|---|
| **Verifica** | README, "Regra de alerta": o alerta exige 3 leituras seguidas fora da faixa (evita alarme falso); `RegraDeAlerta.cs` compara com `temp_max` de forma estrita |
| **Pré-condição** | Equipamento 4 (faixa -25 a -18 °C, umidade máx. 85 %, instituição 2) sem alerta aberto; `{{token-bruna}}` (qualquer perfil da instituição 2 pode gravar leitura) |
| **Automação** | **xUnit** (regra pura em `RegraDeAlerta.Avaliar`) |

Envie as leituras abaixo, nesta ordem, com `POST /api/leituras`
(`{"equipamentoId":4,"temperatura":T,"umidade":75.0,"portaAberta":false,"origem":"SIMULADO"}`):

| # | Temperatura | Resultado esperado |
|---|---|---|
| 1 | -16.0 (acima de -18) | **201**, `alertasAbertos` vazio (1ª fora da faixa) |
| 2 | -16.0 | **201**, `alertasAbertos` vazio (2ª seguida: ainda não basta) |
| 3 | -20.0 (dentro da faixa) | **201**, `alertasAbertos` vazio; **a sequência volta ao zero** |
| 4 | -18.0 (exatamente no limite) | **201**, vazio: o limite não conta como fora da faixa |
| 5 | -18.0 | **201**, vazio |
| 6 | -18.0 | **201**, vazio |

No banco: `SELECT count(*) FROM alerta WHERE equipamento_id = 4;` deve dar **0**.

**Evidência:** as 6 respostas (cada `alertasAbertos: []`) e a contagem zero no `psql`.

---

### Caso 6 — Regra de faixa: 3 leituras seguidas fora da faixa abrem 1 alerta, sem duplicar

| | |
|---|---|
| **Verifica** | README, "Regra de alerta": `TEMP_ALTA` com 3 leituras acima de `temp_max`; severidade por desvio (≥ 1,5 °C = `ALTA`, ≥ 3 °C = `CRITICA`); um alerta aberto por tipo e equipamento |
| **Pré-condição** | Caso 5 executado (a última leitura do equipamento 4 foi -18.0, dentro da faixa) |
| **Automação** | **xUnit** (regra de severidade e de não duplicação) |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `POST /api/leituras` com temperatura **-16.0** (equipamento 4) | **201**, `alertasAbertos` vazio (1ª seguida) |
| 2 | Idem, **-16.0** | **201**, vazio (2ª seguida) |
| 3 | Idem, **-16.0** | **201**, `alertasAbertos` com **1 alerta**: `tipo` `TEMP_ALTA`, `severidade` `ALTA` (desvio de 2,0 °C), `status` `ABERTO` |
| 4 | Idem, **-16.0** | **201**, `alertasAbertos` vazio: o alerta aberto não se repete |
| 5 | `POST /api/leituras` no equipamento 1 (instituição 1, use `{{token-ana}}`) com temperatura **7.8**; ele já tem o alerta 1 aberto | **201**, `alertasAbertos` vazio, sem alerta duplicado |
| 6 | No banco: `SELECT tipo, severidade, status FROM alerta WHERE equipamento_id = 4;` | **1 linha**: `TEMP_ALTA`, `ALTA`, `ABERTO` |

Variações de severidade para o xUnit (equipamento com `temp_max` -18): 3 leituras a -17,0 dão `MEDIA`
(desvio 1,0), a -16,5 dão `ALTA` (desvio 1,5, o limite) e a -14,0 dão `CRITICA` (desvio 4,0).

**Evidência:** respostas dos passos 3 e 4 e o `psql` do passo 6. No app, o alerta aparece na fila (caso 8).

---

### Caso 7 — Segurança: token, SQL Injection, XSS e isolamento entre instituições

| | |
|---|---|
| **Verifica** | README, "Segurança": JWT em todas as rotas, consultas parametrizadas, entrada sem marcação, instituição do token |
| **Pré-condição** | `{{token-mariana}}` (ADMIN, instituição 1) |
| **Automação** | Execução manual |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `GET /api/equipamentos` sem token | **401**, corpo `ProblemDetails` e cabeçalho `WWW-Authenticate: Bearer` |
| 2 | `GET /api/alertas` com o token adulterado (troque o último caractere da assinatura) | **401** |
| 3 | `POST /api/auth/login` com e-mail `' OR 1=1 --` | **400** (não é e-mail válido) |
| 4 | `POST /api/auth/login` com e-mail de Mariana e senha `' OR '1'='1` | **401**, sem token |
| 5 | `GET /api/equipamentos?instituicaoId=1%20OR%201=1` | **400** (o parâmetro é numérico) |
| 6 | `POST /api/equipamentos` com `nome` `<script>alert(1)</script>` | **400**, nada gravado (`SELECT count(*) FROM equipamento` continua igual) |
| 7 | `GET /api/equipamentos/3` (equipamento da instituição 2) | **403** ("pertence a outra instituicao") |
| 8 | `GET /api/equipamentos` | **200**, **só** equipamentos da instituição 1 |
| 9 | Em qualquer resposta de `/api`, ler os cabeçalhos | `X-Content-Type-Options: nosniff`, `Content-Security-Policy: default-src 'none'`, `Cache-Control: no-store`, sem cabeçalho `Server` |
| 10 | **Por último:** 11 chamadas a `POST /api/auth/login` dentro do mesmo minuto (corpo vazio basta) | o limite é de 10 por minuto por IP: a **11ª responde 429**, com `Retry-After: 60`. Logins feitos nos passos anteriores no mesmo minuto também contam |

**Evidência:** status e corpo dos passos 1, 3, 4, 5, 6 e 7; cabeçalhos do passo 9; a resposta 429 do
passo 10.

---

### Caso 8 — Voluntário vê a fila de alertas e resolve no local

| | |
|---|---|
| **Verifica** | README, "Contrato para o app": fila ordenada por severidade, `PATCH .../resolver`, 409 ao repetir, quem e quando resolveu |
| **Pré-condição** | Seed com os alertas 1 (`CRITICA`) e 2 (`ALTA`) abertos na instituição 1; `{{token-ana}}`. Caso realizado pelo **app** (lista de alertas, Kevin) e confirmado pela API |
| **Automação** | Execução manual |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `GET /api/alertas?status=ABERTO` com `{{token-ana}}` | **200**, **2 alertas**, nesta ordem: id 1 (`CRITICA`, equipamento "Câmara fria de hortifrúti") e id 2 (`ALTA`, "Freezer de proteínas"). Nenhum alerta das instituições 2 e 3 |
| 2 | `PATCH /api/alertas/2/resolver` (sem corpo) | **200**, `status` "RESOLVIDO", `resolvidoEm` preenchido e `resolvidoPorId` 2 (Ana) |
| 3 | `GET /api/alertas?status=ABERTO` | **200**, só o alerta 1 |
| 4 | `PATCH /api/alertas/2/resolver` de novo | **409** (`detail` "Alerta 2 ja foi resolvido.") |
| 5 | `PATCH /api/alertas/3/resolver` (alerta da instituição 2) | **403**; o alerta 3 continua `ABERTO` |
| 6 | `PATCH /api/alertas/999/resolver` | **404** |
| 7 | No banco: `SELECT status, resolvido_em, resolvido_por_id FROM alerta WHERE id = 2;` | `RESOLVIDO`, data preenchida, `2` |

**Evidência:** print da lista no app antes e depois de resolver (o alerta some da fila de abertos),
respostas dos passos 2, 4 e 5 e o `psql` do passo 7.

---

### Caso 9 — Relatório do período em PDF

| | |
|---|---|
| **Verifica** | README, "Endpoints": `GET /api/equipamentos/{id}/relatorio` devolve o log térmico em PDF, 7 dias por padrão, teto de 92 dias, horários no fuso de Brasília |
| **Pré-condição** | Seed recém-carregado (as leituras do seed usam `CURRENT_TIMESTAMP`); `{{token-mariana}}` (instituição 1) |
| **Automação** | Execução manual (o PDF é conferido a olho) |

| # | Passo | Resultado esperado |
|---|---|---|
| 1 | `GET /api/equipamentos/1/relatorio` | **200**, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename=relatorio-equipamento-1-<de>-<ate>.pdf` e corpo começando por `%PDF-` (cerca de 43 KB) |
| 2 | Abrir o PDF e ler o cabeçalho | "FRIO SOLIDARIO - LOG TERMICO", instituição e equipamento com a faixa (0,0 a 4,0 °C, umidade máx. 85 %), "Periodo (horario de Brasilia): ... 00:00 a ... 23:59" (7 dias) e "Emitido em" |
| 3 | Conferir as linhas do PDF | **5 leituras** em ordem crescente, cada uma com temperatura, umidade, porta, origem e Situação (`Normal` / `Fora da faixa`), e a seção "Alertas no periodo (1)" com `TEMP_ALTA` `CRITICA` `ABERTO` |
| 4 | Comparar o horário do PDF com o banco: `SELECT registrada_em AT TIME ZONE 'America/Sao_Paulo' FROM leitura WHERE equipamento_id = 1 ORDER BY id LIMIT 1;` | o horário impresso é o **de Brasília**, igual ao do `psql` — não o valor UTC (`AT TIME ZONE 'UTC'`, 3 h à frente) |
| 5 | `GET .../relatorio?de=2026-01-01&ate=2026-11-11` | **400** ("Periodo maximo do relatorio e de 92 dias.") |
| 6 | `GET .../relatorio?de=2026-11-10&ate=2026-11-01` | **400** ("Periodo invalido: de (10/11/2026) e posterior a ate (01/11/2026).") |
| 7 | `GET .../relatorio?de=2026-10-01&ate=2026-10-02` (período sem leitura) | **200** com o PDF dizendo "Sem leituras no periodo." — dia parado não é erro: quem imprime precisa do arquivo |
| 8 | `GET /api/equipamentos/999/relatorio` | **404** ("Equipamento 999 nao encontrado.") |
| 9 | `GET /api/equipamentos/1/relatorio` com `{{token-rafael}}` (instituição 2) | **403** ("Equipamento 1 pertence a outra instituicao.") |

**Evidência:** as respostas dos passos 1 e 5 a 9 com o status; o PDF aberto (passos 2 e 3) e o `psql`
do passo 4, que é o que prova o fuso.

## 4. Rastreabilidade

| Caso | Origem no README | Cards que o sustentam | Onde roda |
|---|---|---|---|
| 1 | "Login (demo)" | Hash BCrypt + perfis; POST /auth/login | API / Swagger |
| 2 | "Endpoints" (perfis) | Hash BCrypt + perfis | API / Swagger |
| 3 | "Endpoints" (CRUD de equipamentos) | CRUD de equipamentos; cadastro no app (Kevin) | App e API |
| 4 | "Endpoints" (PUT, DELETE, série) | CRUD de equipamentos; leituras | API / Swagger |
| 5 | "Regra de alerta" | Regra de faixa → alerta | API e xUnit |
| 6 | "Regra de alerta" | Regra de faixa → alerta | API e xUnit |
| 7 | "Segurança" | JWT + validação + anti-SQLi + anti-XSS | API / Swagger |
| 8 | "Contrato para o app" | Fila e resolução; lista de alertas no app (Kevin) | App e API |
| 9 | "Endpoints" (relatório) | Relatório de logs em PDF; fuso do projeto | API / Swagger |

## 5. Fora deste plano (declarado)

- **Alerta de umidade e de porta aberta:** a regra existe (`UMIDADE`, `PORTA_ABERTA` por tempo), mas
  fica fora dos 9 casos. O seed traz um exemplo de cada (alertas 3 e 4) para a demonstração.
- **Sensor MQTT ao vivo:** o caso 5 e o 6 enviam as leituras pelo mesmo `POST /api/leituras` que o
  sensor usa; a publicação MQTT é do Roger e não está neste repositório.
- **Token vencido:** o token vale 8 h e esperar isso não cabe numa execução manual. O plano cobre
  o token adulterado (caso 7, passo 2); o token expirado foi verificado na auditoria do card de
  segurança, com um token gerado já vencido.
- **Carga e desempenho** e **HTTPS** não fazem parte da entrega.
- **Health check (`/api/health`):** é infraestrutura, não promessa de produto — responde 200 sem token e
  é exercitado indiretamente em toda execução. Não vira caso.
