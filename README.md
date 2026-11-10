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

A senha nunca é gravada em texto puro: o `senha_hash` do seed é BCrypt (work factor 10) e o login
confere a senha contra ele. E-mail inexistente e senha errada devolvem o mesmo 401 com a mesma
mensagem, para não revelar quais e-mails existem. No Swagger, use **Authorize** e cole apenas o
token. O token vale 8 horas e carrega papel (`ADMIN`/`VOLUNTARIO`) e instituição; a API usa a
instituição do token para recusar (403) leitura, alerta ou relatório de equipamento de outra
instituição.

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
| GET | `/api/health` | status da API + quantos equipamentos existem no banco |
| GET | `/api/instituicoes` | lista instituições em ordem alfabética |
| GET | `/api/instituicoes/{id}` | busca uma instituição (404 se não existe) |
| POST | `/api/instituicoes` | cadastra (CNPJ com 14 dígitos, sem pontuação; 409 se repetido) |
| PUT | `/api/instituicoes/{id}` | atualiza |
| DELETE | `/api/instituicoes/{id}` | remove (409 se tiver equipamentos/usuários/lotes) |
| GET | `/api/equipamentos?instituicaoId=` | lista equipamentos, filtrando por instituição |
| GET | `/api/equipamentos/{id}` | busca um equipamento |
| POST | `/api/equipamentos` | cadastra (temp_min < temp_max, umidade até 100) |
| PUT | `/api/equipamentos/{id}` | atualiza |
| DELETE | `/api/equipamentos/{id}` | remove (409 se tiver leituras/alertas) |
| POST | `/api/auth/login` | autentica e devolve o token (única rota pública) |
| POST | `/api/leituras` | registra leitura do sensor e devolve os alertas que ela abriu |
| GET | `/api/equipamentos/{id}/leituras?periodo=24h\|7d` | série do gráfico (24h por padrão) |
| GET | `/api/alertas?status=ABERTO&equipamentoId=` | fila de alertas, mais grave primeiro |
| PATCH | `/api/alertas/{id}/resolver` | resolve o alerta com o usuário do token |
| GET | `/api/equipamentos/{id}/relatorio?de=&ate=` | log térmico em PDF (7 dias por padrão) |

As rotas de `/api/auth/login`, `/api/health` e o Swagger são públicas; **leituras, alertas e
relatório exigem `Authorization: Bearer <token>`**. Os endpoints de CRUD de instituições e
equipamentos continuam abertos por enquanto — aplicá-los a autenticação é o card de segurança do
Arthur (10→14/11).

Os enums trafegam como texto (`CAMARA`, `MERCADO`, `ABERTO`), nunca como número. Erros voltam em
`ProblemDetails`: 400 regra de negócio/validação, 401 credenciais ou token inválido, 403 equipamento
de outra instituição, 404 não encontrado, 409 conflito com o estado do banco.

## Estado atual

| Parte | Situação |
|---|---|
| API em 3 camadas + Swagger | pronto (estrutura, injeção de dependência e health check lendo o banco) |
| Banco de dados | pronto — ER, `schema.sql` com constraints, carga, verificação e 5 consultas (André), testado em PostgreSQL 17.11 |
| Entidades + acesso a dados (EF Core/Npgsql) | pronto — 6 entidades em `Domain/Entities`, `FrioSolidarioDbContext` e repositórios na Infrastructure |
| CRUD de instituições e equipamentos | pronto — `/api/instituicoes` e `/api/equipamentos`, com validação e 400/404/409 |
| Login com JWT | pronto — `POST /api/auth/login`, BCrypt + token com papel e instituição |
| Leituras (ingestão + série) | pronto — `POST /api/leituras` (201) e `GET /api/equipamentos/{id}/leituras?periodo=24h\|7d` |
| Regra de faixa → alerta | pronto — 3 leituras seguidas fora da faixa, porta aberta por tempo, severidade por desvio; `GET /api/alertas` e `PATCH /api/alertas/{id}/resolver` |
| Relatório de logs em PDF | pronto — `GET /api/equipamentos/{id}/relatorio?de=&ate=` (QuestPDF) |
| App mobile | Kevin |
| IoT (MQTT) | Roger — o comando do Mosquitto e o script do sensor entram no README quando ele fechar (16/11) |

## Uso de IA

Ferramentas de IA foram usadas como apoio de pesquisa, revisão de texto e explicação de erros, com
revisão humana do conteúdo. A declaração completa, com as ferramentas e o que cada uma fez, entra
neste README até o dia 17/11.

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
