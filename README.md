# Frio Solidário — Global Solution 2026/2 (FIAP 3ESOA)

Monitoramento da cadeia de frio em bancos de alimentos e comércios doadores: sensores medem
temperatura, umidade e abertura de porta, a API compara com a faixa de cada equipamento e o app
avisa o voluntário antes de o lote ser perdido.

Projeto em construção — Global Solution 2026/2, entrega em 19/11/2026.

## Estrutura (camadas separadas)

```
src/
├── FrioSolidario.Domain/          # entidades do negócio (dono: André)
├── FrioSolidario.Application/     # serviços e regras de negócio + interfaces dos repositórios
├── FrioSolidario.Infrastructure/  # implementação dos repositórios (banco, MQTT)
└── FrioSolidario.Api/             # controllers, Swagger, injeção de dependência
tests/
└── FrioSolidario.Tests/           # testes xUnit da regra de alerta (sem banco e sem HTTP)
```

O fluxo é sempre **Controller → Service → Repository**: o controller não conhece banco, o serviço não
conhece HTTP, e o repositório não devolve DTO de tela. As interfaces de repositório ficam em
`Application/Repositories` e as implementações em `Infrastructure/Repositories`.

## Como rodar (API)

Pré-requisitos: .NET SDK 10 e o banco carregado (seção "Como carregar o banco").

```bash
dotnet restore
dotnet run --project src/FrioSolidario.Api
```

- Swagger: http://localhost:5199/swagger
- Health check: http://localhost:5199/api/health

`GET /api/health` responde no formato:

```json
{"servico":"Frio Solidario API","status":"ok","versao":"1.0.0","ambiente":"Development",
 "verificadoEm":"2026-11-10T18:51:30-03:00","origemDosDados":"6 equipamentos no banco"}
```

O campo `origemDosDados` é lido do banco de verdade (`SELECT COUNT(*)` em `equipamento`): quando o
Postgres está fora do ar ou o schema não foi carregado, a API não quebra — responde
`"banco indisponivel (schema nao carregado ou Postgres fora do ar)"` com HTTP 200.

## Testes automatizados

Os casos 5 e 6 do plano de testes (`docs/plano-de-testes.md`) rodam em xUnit, sem banco e sem HTTP:

```bash
dotnet test
dotnet test --filter Caso=5   # menos de 3 leituras fora da faixa não abrem alerta
dotnet test --filter Caso=6   # 3 leituras seguidas abrem 1 alerta, sem duplicar
```

São 29 testes em `tests/FrioSolidario.Tests`: a regra pura (`RegraDeAlertaTests`: limites da faixa,
severidade por desvio) e o fluxo do `POST /api/leituras` com repositórios em memória
(`LeituraServiceTests`: um alerta aberto por tipo, sem duplicar). Os demais casos do plano (login,
perfis, CRUD, segurança, relatório) são verificados por chamada real, com evidência em `docs/`.

## Login (demo)

`POST /api/auth/login` devolve o token que as rotas protegidas exigem. Usuários da carga inicial
(`db/seed.sql`), todos com a senha demonstrativa **`Frio@2026`**:

| E-mail | Papel | Instituição |
|---|---|---|
| mariana.costa@caminhosdobem.example | ADMIN | 1 — Banco de Alimentos Caminhos do Bem |
| ana.rocha@caminhosdobem.example | VOLUNTARIO | 1 |
| rafael.nunes@bairrosolidario.example | ADMIN | 2 — Mercado Bairro Solidário |
| bruna.oliveira@bairrosolidario.example | VOLUNTARIO | 2 |
| carla.mendes@saborepartilha.example | ADMIN | 3 — Padaria Sabor e Partilha |
| pedro.lima@saborepartilha.example | VOLUNTARIO | 3 |

```bash
curl -s -X POST http://localhost:5199/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"mariana.costa@caminhosdobem.example","senha":"Frio@2026"}'
```

A senha nunca é gravada em texto puro: o `senha_hash` do seed é BCrypt (work factor 10), o login
confere a senha contra ele e `IPasswordHasher.Gerar` produz hashes novos com o mesmo custo.
E-mail inexistente e senha errada devolvem o mesmo 401 com a mesma mensagem, para não revelar
quais e-mails existem, e o login aceita **10 tentativas por minuto por IP** (a 11ª volta 429). No
Swagger, use **Authorize** e cole apenas o token. O token vale 8 horas e carrega papel
(`ADMIN`/`VOLUNTARIO`) e instituição; a API usa a instituição do token para recusar (403)
equipamento, leitura, alerta ou relatório de outra instituição.

A chave do token fica em `Jwt:Key` no `appsettings.json` (valor de desenvolvimento) e pode ser
trocada sem editar arquivo:

```bash
export Jwt__Key="uma-chave-de-pelo-menos-32-caracteres"
```

## Regra de alerta

A regra vive em `Application/Services/RegraDeAlerta.cs`, sem banco e sem HTTP, e roda em toda leitura
gravada por `POST /api/leituras`:

| Tipo | Quando abre | Severidade |
|---|---|---|
| `TEMP_ALTA` | 3 leituras seguidas acima de `temp_max` | desvio ≥ 1,5 °C = ALTA; ≥ 3 °C = CRÍTICA; senão MEDIA |
| `TEMP_BAIXA` | 3 leituras seguidas abaixo de `temp_min` | mesmo critério de desvio |
| `UMIDADE` | 3 leituras seguidas acima de `umid_max` | até 5 pontos acima = BAIXA; acima = MEDIA |
| `PORTA_ABERTA` | porta aberta sem interrupção por mais de 5 min | 15 min ou mais = ALTA; senão MEDIA |

Por que 3 leituras: porta abrindo por um minuto derruba a leitura de um sensor só, e um alerta por
leitura encheria a fila do app durante uma falha real. Um alerta aberto por tipo e equipamento basta
— leituras seguintes fora da faixa não criam alerta duplicado. O alerta nasce `ABERTO` e só fecha em
`PATCH /api/alertas/{id}/resolver`, que grava quem resolveu e quando (o CHECK do schema exige os dois
campos). As faixas de severidade são proposta da API para o grupo fechar; mudar a faixa de um
equipamento no cadastro muda o comportamento sem tocar em código.

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| GET | `/api/health` | status da API + quantos equipamentos existem no banco (pública) |
| GET | `/api/instituicoes` | lista todas as instituições em ordem alfabética |
| GET | `/api/instituicoes/{id}` | busca uma instituição (404 se não existe) |
| POST | `/api/instituicoes` | cadastra — `ADMIN` (CNPJ com 14 dígitos, sem pontuação; 409 se repetido) |
| PUT | `/api/instituicoes/{id}` | atualiza — `ADMIN`, só a própria instituição |
| DELETE | `/api/instituicoes/{id}` | remove — `ADMIN`, só a própria (409 se tiver equipamentos/usuários/lotes) |
| GET | `/api/equipamentos?instituicaoId=` | lista os equipamentos da instituição do token (`instituicaoId` opcional; de outra = 403) |
| GET | `/api/equipamentos/{id}` | busca um equipamento da instituição do token |
| POST | `/api/equipamentos` | cadastra — `ADMIN`, na própria instituição (temp_min < temp_max, umidade até 100) |
| PUT | `/api/equipamentos/{id}` | atualiza — `ADMIN`, sem trocar de instituição |
| DELETE | `/api/equipamentos/{id}` | remove — `ADMIN` (409 se tiver leituras/alertas) |
| POST | `/api/auth/login` | autentica e devolve o token (pública; 10 tentativas/min por IP) |
| POST | `/api/leituras` | registra leitura do sensor e devolve os alertas que ela abriu |
| GET | `/api/equipamentos/{id}/leituras?periodo=24h\|7d` | série do gráfico (24h por padrão) |
| GET | `/api/alertas?status=ABERTO&equipamentoId=` | fila de alertas da instituição do token, mais grave primeiro |
| PATCH | `/api/alertas/{id}/resolver` | resolve o alerta com o usuário do token (só da própria instituição) |
| GET | `/api/equipamentos/{id}/relatorio?de=&ate=` | log térmico em PDF (7 dias por padrão, até 92 dias; horários em horário de Brasília) |

Só `/api/auth/login`, `/api/health` e o Swagger são públicos; **todas as outras rotas exigem
`Authorization: Bearer <token>`** (a API é fechada por padrão: rota nova nasce protegida). No CRUD
de instituições e equipamentos, as **escritas (POST, PUT, DELETE) exigem o papel `ADMIN`**: sem
token vem 401 e com token de `VOLUNTARIO` vem 403.

Os enums trafegam como texto (`CAMARA`, `MERCADO`, `ABERTO`), nunca como número — número no lugar
do texto volta 400. A grafia do nome é aceita em qualquer caixa (`"CAMARA"` e `"camara"` valem o
mesmo): quem manda é o vocabulário, não as maiúsculas/minúsculas. Erros voltam em `ProblemDetails`: 400 regra de negócio/validação, 401
credenciais ou token ausente/inválido/expirado, 403 perfil sem permissão ou recurso de outra
instituição, 404 não encontrado, 409 conflito com o estado do banco, 429 tentativas de login demais.

## Segurança

Resumo do card JWT + validação + anti-SQLi + anti-XSS (Arthur), para a seção de segurança do PDF.
Cada linha foi conferida rodando a API contra o banco do seed (PostgreSQL 17.11).

| Ameaça | Defesa na API | Como conferir |
|---|---|---|
| Acesso sem login | Política de fallback exige token em toda rota não marcada como pública | `GET /api/equipamentos` sem token → 401 |
| Token forjado ou adulterado | Assinatura HMAC-SHA256 obrigatória, algoritmo fixo (`HS256`; `alg: none` recusado), emissor, público e validade conferidos, folga de 30 s | token com outra chave ou `alg: none` → 401 |
| Token vencido | Validade de 8 h; 401 com a mensagem "Token expirado" para o app voltar ao login | token vencido → 401 |
| Voluntário fazendo tarefa de administrador | Papel do token (`ADMIN`/`VOLUNTARIO`) exigido nas escritas do CRUD | `VOLUNTARIO` em `POST /api/equipamentos` → 403 |
| Usuário mexendo em dados de outra instituição (IDOR) | Instituição do token conferida em equipamentos, alertas, resolução, leituras, relatório e na alteração de instituição; token sem instituição é recusado | ADMIN da instituição 1 em `GET /api/equipamentos/3` → 403 |
| Adivinhar senha | BCrypt (lento de propósito) + 10 tentativas de login por minuto por IP; mesma resposta para e-mail inexistente e senha errada | 11ª tentativa em 1 min → 429 |
| SQL Injection | Todo acesso ao banco é EF Core com LINQ (consultas parametrizadas); nenhum SQL montado por concatenação no código. Parâmetros têm tipo (`long`, enum, data), então texto no lugar de número nem chega ao serviço | `?instituicaoId=1 OR 1=1` → 400; senha `' OR '1'='1` → 401 |
| XSS armazenado | Nome, cidade e endereço recusam `<`, `>` e caracteres de controle; o JSON sai escapado (`<` vira `\u003C`) e o PDF desenha texto, não HTML | nome `<script>alert(1)</script>` → 400 |
| Resposta interpretada como página | `X-Content-Type-Options: nosniff`, `Content-Security-Policy: default-src 'none'` (fora do Swagger), `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store` em `/api`; sem cabeçalho `Server` | cabeçalhos de qualquer resposta de `/api` |
| Dado fora do vocabulário | Enum só por nome (`"FREEZER"`), validação dos DTOs (tamanho, faixa, CNPJ) antes do serviço | `"tipo": 2` → 400 |
| Requisição cara de atender | Relatório limitado a 92 dias por pedido | `de=2026-01-01&ate=2026-11-10` → 400 |
| Senha truncada em silêncio | `IPasswordHasher.Gerar` recusa senha vazia ou acima de 72 bytes (limite do BCrypt) com `RegraDeNegocioException`, que a API devolve como 400 | — (sem cadastro de usuário na API ainda) |

## Contrato para o app (Kevin)

Ordem que o app segue, com o corpo real de cada resposta (medido rodando a API):

1. `POST /api/auth/login` → guarda `token` e `expiraEm`. Em todo request seguinte:
   `Authorization: Bearer <token>`.
2. `GET /api/equipamentos?instituicaoId=1` → lista com `nome`, `tipo`, `tempMin`, `tempMax`, `umidMax`.
3. `GET /api/equipamentos/{id}/leituras?periodo=24h` → série pronta para o gráfico, **já em ordem
   cronológica** (a mais antiga primeiro):

```json
[{"id":5,"equipamentoId":1,"temperatura":7.6,"umidade":75.0,"portaAberta":false,
  "registradaEm":"2026-11-10T23:30:00+00:00","origem":"SENSOR"}]
```

4. `GET /api/alertas?status=ABERTO` → fila já ordenada por severidade (`CRITICA`, `ALTA`, `MEDIA`,
   `BAIXA`) e, dentro da mesma severidade, do mais recente para o mais antigo:

```json
[{"id":6,"equipamentoId":6,"equipamento":"Câmara de resfriamento","leituraId":36,
  "tipo":"TEMP_ALTA","severidade":"CRITICA","status":"ABERTO",
  "criadoEm":"2026-11-10T23:30:00+00:00","resolvidoEm":null,"resolvidoPorId":null}]
```

5. `PATCH /api/alertas/{id}/resolver` (sem corpo) → devolve o alerta com `status: "RESOLVIDO"`,
   `resolvidoEm` e `resolvidoPorId`. Repetir a chamada devolve 409.
6. `GET /api/equipamentos/{id}/relatorio?de=2026-11-01&ate=2026-11-10` → **corpo é o PDF**
   (`Content-Type: application/pdf`, nome do arquivo em `Content-Disposition`). Salve os bytes em cache e
   abra com a folha de compartilhamento/impressão do Android. `de`/`ate` são opcionais (padrão: últimos
   7 dias, fim inclusivo no dia).

Detalhes que evitam retrabalho no app: enums trafegam em texto (`CAMARA`, `TEMP_ALTA`, `ABERTO`); datas
vêm em ISO 8601 **UTC** (`+00:00`) e precisam ser convertidas para o fuso do aparelho na exibição; 401
significa token ausente ou vencido (8 h) — volte para o login; 403 significa perfil sem permissão
(`VOLUNTARIO` tentando cadastrar) ou recurso de outra instituição; 429 no login significa tentativas
demais — espere um minuto. Toda resposta de erro, inclusive 401 e 403, vem em `ProblemDetails`
(`title`, `status`, `detail`).

## Contrato para o sensor (Roger)

O sensor publica exatamente como o app, com o mesmo token:

```bash
# 1. token (vale 8 horas; guarde e renove)
curl -s -X POST http://localhost:5199/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rafael.nunes@bairrosolidario.example","senha":"Frio@2026"}'

# 2. leitura (201 se gravou)
curl -s -X POST http://localhost:5199/api/leituras \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"equipamentoId":3,"temperatura":8.4,"umidade":72.0,"portaAberta":false,"origem":"SENSOR"}'
```

| Campo | Obrigatório | Regra |
|---|---|---|
| `equipamentoId` | sim | precisa ser de uma instituição do token (senão 403) |
| `temperatura` | sim | entre -40 e 80 °C (400 fora disso) |
| `umidade` | sim | entre 0 e 100 % (400 fora disso) |
| `portaAberta` | sim | booleano |
| `registradaEm` | não | ISO 8601; sem isso o servidor usa o recebimento. No futuro = 400 (tolerância de 5 min para relógio do sensor) |
| `origem` | não | `SENSOR` (padrão) ou `SIMULADO` |

A resposta traz os alertas que a leitura abriu, então o próprio sensor (ou o script da demo) vê o alarme
sem outra chamada:

```json
{"leitura":{"id":37,"equipamentoId":3,"temperatura":9.1,"umidade":72.0,"portaAberta":false,
            "registradaEm":"2026-11-10T23:35:00+00:00","origem":"SENSOR"},
 "alertasAbertos":[{"id":8,"tipo":"TEMP_ALTA","severidade":"CRITICA","status":"ABERTO"}]}
```

Intervalo sugerido de publicação: **2 minutos** — é o intervalo da carga inicial e a regra exige 3 leituras
seguidas fora da faixa, então em 2 min o alarme aparece no app em ~6 min (dentro da meta de 15 min do
pitch). Publicar mais rápido que isso também funciona: a fila não duplica alerta aberto.

## Como apontar o app para a API

`localhost` no celular é o celular. Três caminhos, do mais simples ao mais realista na apresentação:

| Onde o app roda | URL da API | O que precisa |
|---|---|---|
| Emulador Android | `http://10.0.2.2:5199` | nada além do emulador (`10.0.2.2` é o PC visto de dentro dele) |
| Aparelho no USB | `http://localhost:5199` | `adb reverse tcp:5199 tcp:5199` (o cabo leva a porta do PC para o celular) |
| Celular na mesma Wi-Fi | `http://<ip-do-pc>:5199` | API escutando em `0.0.0.0` e a porta liberada no firewall |

Para a API aceitar conexão de fora do PC (Wi-Fi na demo), suba com:

```bash
ASPNETCORE_URLS=http://0.0.0.0:5199 dotnet run --project src/FrioSolidario.Api
```

e libere a porta uma vez: `netsh advfirewall firewall add rule name="Frio Solidario API" dir=in action=allow protocol=TCP localport=5199`.
Com `localhost:5199` a API só responde na própria máquina — é a causa nº 1 de "o app não conecta".

## Estado atual

| Parte | Situação |
|---|---|
| API em 3 camadas + Swagger | pronto (estrutura, injeção de dependência e health check lendo o banco) |
| Banco de dados | pronto — ER, `schema.sql` com constraints, carga, verificação e 5 consultas (André), testado em PostgreSQL 17.11 |
| Entidades + acesso a dados (EF Core/Npgsql) | pronto — 6 entidades em `Domain/Entities`, `FrioSolidarioDbContext` e repositórios na Infrastructure |
| CRUD de instituições e equipamentos | pronto — `/api/instituicoes` e `/api/equipamentos`, com validação e 400/404/409 |
| Login com JWT | pronto — `POST /api/auth/login`, BCrypt (gerar e conferir) + token com papel e instituição |
| Perfis ADMIN e VOLUNTARIO | pronto — escritas do CRUD de instituições e equipamentos exigem `ADMIN` (401 sem token, 403 para `VOLUNTARIO`) |
| Segurança (JWT, validação, anti-SQLi, anti-XSS) | pronto — todas as rotas com token, isolamento por instituição, limite no login, cabeçalhos de segurança; ver seção "Segurança" |
| Leituras (ingestão + série) | pronto — `POST /api/leituras` (201) e `GET /api/equipamentos/{id}/leituras?periodo=24h\|7d` |
| Regra de faixa → alerta | pronto — 3 leituras seguidas fora da faixa, porta aberta por tempo, severidade por desvio; `GET /api/alertas` e `PATCH /api/alertas/{id}/resolver` |
| Testes automatizados (xUnit) | pronto — casos 5 e 6 do plano (regra de alerta): 29 testes em `tests/FrioSolidario.Tests`, `dotnet test` |
| Relatório de logs em PDF | pronto — `GET /api/equipamentos/{id}/relatorio?de=&ate=` (QuestPDF) |
| App mobile | Kevin |
| IoT (MQTT) | Roger — o comando do Mosquitto e o script do sensor entram no README quando ele fechar (16/11) |

## Uso de IA

Regra combinada no grupo (reunião de 07/11): cada integrante declara as ferramentas que usou e para quê,
com revisão humana do resultado. Preencha a sua linha — a tabela vai para o PDF da entrega.

| Integrante | RM | Ferramenta de IA | Para quê |
|---|---|---|---|
| André | 554952 | a preencher | a preencher |
| Arthur | 97781 | a preencher | a preencher |
| Kevin | 557898 | a preencher | a preencher |
| Roger | 97540 | a preencher | a preencher |
| Yasmin | 557413 | a preencher (sugestão: Hermes Agent + Claude Code) | geração e revisão de código da API, testes de integração e revisão do README |

O que vale declarar, sem medo: código gerado com IA e revisado por humano, texto revisado, pesquisa de
referência (Atlas, ANVISA, Lei 15.224/2025) e explicação de erro. Nenhuma senha, token ou credencial de
instituição real foi usada; os dados do projeto são fictícios.

## O que ainda não roda (declarado, não escondido)

| Item | Situação | Dono |
|---|---|---|
| App mobile | não iniciado no repositório da API | Kevin |
| Sensor MQTT publicando ao vivo | script de simulação previsto para 10/11; a API já aceita `POST /api/leituras` | Roger |
| Testes automatizados (xUnit) abrangentes | só a regra de alerta (casos 5 e 6) tem testes; login, perfis, CRUD, segurança e relatório são verificados por chamada real, com evidência em `docs/` | Arthur |
| HTTPS | a demo roda em HTTP na rede local (`:5199`); em produção a API ficaria atrás de TLS | grupo |
| Chave do JWT | o `appsettings.json` traz uma chave de desenvolvimento; fora da demo, defina `Jwt__Key` | grupo |
| Administrador do sistema | não há papel acima de `ADMIN`: qualquer `ADMIN` pode cadastrar instituição nova (alterar e remover, só a própria) | grupo |

Melhor a banca ler isso aqui do que descobrir na apresentação — o edital desconta o que não roda.

## Banco de Dados — Frio Solidário

O projeto utiliza PostgreSQL 17 como sistema gerenciador
de banco de dados relacional.

### Modelo de dados

O banco possui 6 entidades:

- instituicao
- usuario
- equipamento
- leitura
- alerta
- lote

As tabelas possuem chaves primárias (PK), estrangeiras (FK),
restrições NOT NULL, UNIQUE e CHECK para garantir a
integridade dos dados.

### Arquivos SQL

- `db/schema.sql`: criação das tabelas, constraints e índices.
- `db/seed.sql`: carga inicial do banco de dados.
- `db/verificar.sql`: validação dos registros e constraints.
- `db/consultas.sql`: as 5 consultas de indicadores.
- `db/testar.ps1`: execução automatizada dos testes.

### Como carregar o banco

Pré-requisito: PostgreSQL 17 com o cliente `psql` no PATH.

```bash
createdb -U postgres frio_solidario
psql -U postgres -d frio_solidario -v ON_ERROR_STOP=1 -f db/schema.sql
psql -U postgres -d frio_solidario -v ON_ERROR_STOP=1 -f db/seed.sql
```

Sem PostgreSQL instalado na máquina, o mesmo banco sobe em container (foi assim que a integração foi
testada):

```bash
docker run -d --name frio-pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=frio_solidario -v "$PWD/db:/sql:ro" postgres:17
docker exec frio-pg psql -U postgres -d frio_solidario -v ON_ERROR_STOP=1 -f /sql/schema.sql
docker exec frio-pg psql -U postgres -d frio_solidario -v ON_ERROR_STOP=1 -f /sql/seed.sql
```

A API lê esse banco pela connection string `ConnectionStrings:FrioSolidario` do
`src/FrioSolidario.Api/appsettings.json` (por padrão `localhost:5432/frio_solidario`). Para apontar
para outro servidor sem editar arquivo, use a variável de ambiente:

```bash
export ConnectionStrings__FrioSolidario="Host=localhost;Port=5432;Database=frio_solidario;Username=postgres;Password=postgres"
```

O `schema.sql` é a fonte da verdade do modelo: o EF Core em `Infrastructure/Persistence` só mapeia as
tabelas, não cria nem altera nenhuma delas.

### Carga inicial

A carga inicial contém:

- 3 instituições
- 6 usuários
- 6 equipamentos
- 30 leituras de sensores
- 5 alertas
- 12 lotes de alimentos

Os dados são fictícios e simulam situações reais de
funcionamento, incluindo variações de temperatura,
umidade, abertura de porta e resolução de alertas.

### Índices obrigatórios

- `leitura(equipamento_id, registrada_em)`
- `alerta(status)`

Índice adicional, usado pelas consultas 2 e 3: `alerta(equipamento_id, criado_em)`.

### Regras de coerência

Além dos CHECK de faixa e de vocabulário, o schema exige coerência entre status e datas:

- alerta `ABERTO` não tem `resolvido_em`/`resolvido_por_id`; alerta `RESOLVIDO` tem os dois, com
  `resolvido_em >= criado_em`.
- lote `DISPONIVEL` não tem `retirado_em`; lote `DISTRIBUIDO` ou `PERDIDO` tem. Sem esse CHECK, um
  lote marcado como distribuído sem data sairia da conta da consulta 1 (kg doados) sem aviso.
- o alerta só pode apontar para uma leitura do mesmo equipamento (FK composta
  `(leitura_id, equipamento_id)`).

### Como testar

Requisitos:
- PostgreSQL 17
- PowerShell
- Ferramentas PostgreSQL disponíveis no PATH

Na raiz do projeto, executar:

```powershell
powershell -ExecutionPolicy Bypass -File .\db\testar.ps1
```

O script cria um banco temporário, executa os arquivos SQL,
verifica os dados e gera o log de evidências.

### Evidências

O teste foi executado com sucesso em PostgreSQL 17.11 (10/11/2026), incluindo os testes das
constraints de coerência (alerta resolvido e retirada de lote).

Log de execução:
`db/evidencias/teste_postgresql.log`

Resultado: SUCESSO.

### Diagrama ER

O modelo entidade-relacionamento está previsto nos arquivos:

- `docs/der.md`
- `docs/der.png`

## Consultas SQL — Indicadores de Impacto

O arquivo `db/consultas.sql` contém cinco consultas
utilizando JOIN, funções de agregação e agrupamento.

### Consultas implementadas

1. **Quilos doados por instituição nos últimos 30 dias**
   - JOIN entre instituicao e lote.
   - Utiliza SUM e GROUP BY.
   - Apoia o acompanhamento do volume de doações.

2. **Equipamentos com mais alertas críticos**
   - JOIN entre equipamento e alerta.
   - Utiliza COUNT, GROUP BY e ORDER BY.
   - Apoia a priorização de manutenção.

3. **Tempo médio de resolução dos alertas**
   - JOIN entre instituicao, equipamento e alerta.
   - Utiliza AVG e cálculo de diferença entre datas.
   - Apoia a avaliação da eficiência das equipes.

4. **Percentual de leituras fora da faixa**
   - JOIN entre equipamento e leitura.
   - Utiliza CASE e agregação condicional.
   - Apoia a identificação de equipamentos com falhas.

5. **Taxa de perda por categoria de alimento**
   - Utiliza SUM, CASE, GROUP BY e HAVING.
   - Calcula a porcentagem de lotes perdidos.
   - Apoia decisões para reduzir o desperdício.

### Validação

As cinco consultas foram executadas no PostgreSQL 17.11,
utilizando a carga inicial do projeto.

Todas retornaram resultados.

As evidências estão registradas no arquivo:

`db/evidencias/teste_postgresql.log`
