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
└── FrioSolidario.Tests/           # testes xUnit: regra pura, serviço e API via TestServer (sem banco)
mobile/                            # app Android (Kotlin + Jetpack Compose), projeto Gradle à parte (Kevin)
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

Os casos 5, 6 e 7 do plano de testes (`docs/plano-de-testes.md`) rodam em xUnit, sem banco: regra pura,
serviço e **API de verdade** (`WebApplicationFactory` = TestServer, com os repositórios em memória).

```bash
dotnet test
dotnet test --filter Caso=5   # menos de 3 leituras fora da faixa não abrem alerta
dotnet test --filter Caso=6   # 3 leituras seguidas abrem 1 alerta, sem duplicar
```

São **66 testes** em `tests/FrioSolidario.Tests`: a regra pura (`RegraDeAlertaTests`: limites da faixa,
severidade por desvio), o fluxo do `POST /api/leituras` com repositórios em memória
(`LeituraServiceTests`: um alerta aberto por tipo, sem duplicar) e a API por HTTP (`ApiTests`: 401 sem
token, casos 5 e 6, a rota `/instituicoes/{id}/equipamentos` e o DELETE lógico do equipamento). Os
demais casos do plano (login, perfis, segurança, relatório) são verificados por chamada real, com
evidência em `docs/`.

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
| GET | `/api/instituicoes/{id}/equipamentos` | equipamentos de uma instituição (`?ativo=`, 403 se não for a do token) |
| POST | `/api/instituicoes` | cadastra — `ADMIN` (CNPJ com 14 dígitos, sem pontuação; 409 se repetido) |
| PUT | `/api/instituicoes/{id}` | atualiza — `ADMIN`, só a própria instituição |
| DELETE | `/api/instituicoes/{id}` | remove — `ADMIN`, só a própria (409 se tiver equipamentos/usuários/lotes) |
| GET | `/api/equipamentos?instituicaoId=&ativo=` | lista os equipamentos da instituição do token (`instituicaoId` opcional, de outra = 403; `ativo=true` esconde os desativados) |
| GET | `/api/equipamentos/{id}` | busca um equipamento da instituição do token |
| POST | `/api/equipamentos` | cadastra — `ADMIN`, na própria instituição (temp_min < temp_max, umidade até 100) |
| PUT | `/api/equipamentos/{id}` | atualiza — `ADMIN`, sem trocar de instituição |
| DELETE | `/api/equipamentos/{id}` | **desativa** o equipamento — `ADMIN`; 204 e `ativo: false`, leituras e alertas continuam no banco (409 se já está desativado) |
| POST | `/api/auth/login` | autentica e devolve o token (pública; 10 tentativas/min por IP) |
| POST | `/api/leituras` | registra leitura do sensor e devolve os alertas que ela abriu |
| GET | `/api/equipamentos/{id}/leituras?periodo=24h\|7d` | série do gráfico (24h por padrão) |
| GET | `/api/equipamentos/{id}/leituras/ultima` | leitura mais recente, de quando for (painel do app); 204 se o equipamento nunca enviou |
| GET | `/api/alertas?status=ABERTO&equipamentoId=` | fila de alertas da instituição do token, mais grave primeiro |
| PATCH | `/api/alertas/{id}/resolver` | resolve o alerta com o usuário do token (só da própria instituição); corpo opcional `{"acao":"AJUSTOU"}` grava a ação tomada |
| GET | `/api/lotes?status=` | lotes doados da instituição do token, por validade (o que vence primeiro) |
| GET | `/api/lotes/{id}` | busca um lote (403 se for de outra instituição) |
| POST | `/api/lotes` | cadastra lote doado — `ADMIN`; nasce `DISPONIVEL` (descrição, categoria, kg, validade) |
| PATCH | `/api/lotes/{id}/retirada` | registra a saída — `ADMIN`: `DISTRIBUIDO` ou `PERDIDO`, grava `retiradoEm` (409 se já saiu) |
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

Limites físicos aceitos (os mesmos `CHECK` do schema): temperatura de **-40 a 80 °C** e umidade de
**0 a 100 %**. Fora disso é 400 de validação, antes de chegar ao banco.

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
2. `GET /api/equipamentos?instituicaoId=1` (ou `?ativo=true`) → lista com `nome`, `tipo`, `tempMin`,
   `tempMax`, `umidMax` e **`ultimaLeitura`**: a leitura mais recente de cada equipamento, ou `null`
   se ele nunca enviou nenhuma. O painel monta o semáforo só com essa resposta — uma requisição por
   ciclo de 15 s. O mesmo campo vem em `GET /api/equipamentos/{id}`, então o cabeçalho do detalhe
   também não busca a leitura à parte. `GET /api/equipamentos/{id}/leituras/ultima` continua
   disponível para quem só quer a última leitura de um equipamento.
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

5. `PATCH /api/alertas/{id}/resolver` → devolve o alerta com `status: "RESOLVIDO"`, `resolvidoEm` e
   `resolvidoPorId`. O corpo é **opcional**: `{"acao":"AJUSTOU"}`, `"TRANSFERIU_LOTE"` ou `"DESCARTOU"`
   grava o que o voluntário fez e volta em `acaoTomada` (também no `GET /api/alertas?status=RESOLVIDO`,
   que é o histórico do app); sem corpo, `acaoTomada` fica `null`. Ação fora do vocabulário devolve 400
   e repetir a chamada devolve 409. Banco já carregado precisa de
   `db/migracoes/2026-11-15-alerta-acao-tomada.sql`.
6. `GET /api/equipamentos/{id}/relatorio?de=2026-11-01&ate=2026-11-10` → **corpo é o PDF**
   (`Content-Type: application/pdf`, nome do arquivo em `Content-Disposition`). Salve os bytes em cache e
   abra com a folha de compartilhamento/impressão do Android. `de`/`ate` são opcionais (padrão: últimos
   7 dias, fim inclusivo no dia).
7. Lote doado (tela de cadastro): `POST /api/lotes` com `{"descricao":"...","categoria":"HORTIFRUTI",
   "kg":120.5,"validade":"2026-11-20"}` → **201** com `id`, `status` `DISPONIVEL` e `instituicaoId` do
   token. A categoria vai como o app mandar e o servidor grava em maiúsculas. Lista com
   `GET /api/lotes?status=DISPONIVEL` (já ordenada pela validade) e saída do estoque com
   `PATCH /api/lotes/{id}/retirada` `{"status":"DISTRIBUIDO"}` → 200; repetir devolve 409.

Para o painel, use `GET /api/equipamentos?ativo=true`: equipamento removido é **desativado**, não
apagado (o histórico do log térmico fica), então sem o filtro a lista traz os inativos com `ativo: false`.

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

## Ponte MQTT (sensor → API)

Além do `POST /api/leituras`, a API sabe ouvir o broker. A ponte é um `BackgroundService`
(`Infrastructure/Mqtt/PonteMqttDeLeituras.cs`) que assina o tópico, converte a mensagem em
`LeituraRequest` e chama o **mesmo** `LeituraService` do endpoint — a leitura do sensor passa pela
regra de faixa e abre alerta igual a uma leitura vinda do app.

```bash
# 1. broker (Docker) — sem config externa, só para a demo
docker run -d --name frio-mqtt -p 1883:1883 eclipse-mosquitto:2 \
  sh -c "printf 'listener 1883\nallow_anonymous true\n' > /mosquitto/config/mosquitto.conf && mosquitto -c /mosquitto/config/mosquitto.conf"

# 2. API com a ponte ligada (desligada por padrão, no appsettings Mqtt:Habilitado=false)
cd src/FrioSolidario.Api
Mqtt__Habilitado=true dotnet run

# 3. publicar como o sensor
mosquitto_pub -h localhost -t "frio/equipamentos/4/leitura" \
  -m '{"temperatura": -10.2, "umidade": 60, "portaAberta": false}'
```

| Item | Valor |
|---|---|
| Tópico | `frio/equipamentos/{idEquipamento}/leitura` (a ponte assina `frio/equipamentos/+/leitura`) |
| Payload | `{"temperatura": 7.8, "umidade": 55, "portaAberta": false, "registradaEm": "2026-11-12T00:10:00-03:00"}` |
| Obrigatórios | `temperatura` e `umidade` (número ou texto; aceita `-18,5`) |
| Opcionais | `portaAberta` (padrão `false`) e `registradaEm` (sem ela, o servidor usa o recebimento) |
| Aceita também | `snake_case` (`porta_aberta`, `registrada_em`) — script em Python costuma sair assim |
| QoS da assinatura | 1 — o broker reentrega se a API cair no meio |

Configuração (`appsettings.json`, seção `Mqtt`): `Habilitado`, `Host`, `Porta`, `PrefixoTopico`,
`ClienteId`, `Usuario`, `Senha`. Tudo trocável por ambiente (`Mqtt__Host=...`). Com
`Habilitado=false` nenhum socket é aberto — `dotnet run` e `dotnet test` continuam sem broker.

### Simulação vs. hardware físico

O script `simulacao/sensor.py` substitui o microcontrolador **na demo**, publicando no **mesmo** tópico,
payload e QoS 1 que o hardware físico usaria em produção. A ponte não distingue os dois: qualquer
mensagem MQTT é persistida com `origem = SENSOR` (o campo `origem` do payload é ignorado pela ponte,
que fixa `OrigemLeitura.SENSOR`). Ou seja, leituras simuladas aparecem como `SENSOR` no banco —
transparência importante para a banca.

| | **Simulado (demo)** | **Hardware físico (produção)** |
|---|---|---|
| Sensor temp/umidade | Script Python gerando valores | **DHT22** (-40 a 80 °C, ±0,5 °C; 0 a 100% UR) |
| Sensor de porta | Campo `portaAberta` alternado pelo script | **Reed Switch** magnético |
| Microcontrolador | Processo local (`paho-mqtt`) | **ESP32** (Wi-Fi + MQTT) |
| Publicação | `paho-mqtt` → broker local | MQTT QoS 1 → broker |
| Queda de internet | Não se aplica (broker local) | Buffer circular em memória flash/NVS; reenvio na reconexão |

**DHT22:** mede temperatura (-40 a 80 °C, precisão ±0,5 °C) e umidade relativa (0 a 100% UR). Cobre a
faixa de todos os equipamentos do seed (de -25 °C do freezer a 6 °C da geladeira) com precisão
suficiente para a regra de alerta (desvio ≥ 1,5 °C = ALTA; ≥ 3 °C = CRÍTICA).

**Reed Switch:** interruptor magnético que sinaliza `portaAberta: true/false`. O limiar de **`>= 5 min`**
separa uma abertura operacional (carga/descarga de dezenas de segundos) de uma porta esquecida — a
causa mais comum de perda de frio — sem gerar falso positivo.

**ESP32 + MQTT:** publica QoS 1 em `frio/equipamentos/{id}/leitura` com o payload JSON abaixo. Se a
internet cair, as leituras ficam num buffer circular em memória flash/NVS e são reenviadas na
reconexão; o QoS 1 garante que a mensagem não se perde no meio do caminho.

```json
{"temperatura": -20.4, "umidade": 72.0, "portaAberta": false, "registradaEm": "2026-11-15T15:10:00-03:00", "origem": "SIMULADO"}
```

### Script de simulação (`simulacao/sensor.py`)

Dependência: `pip install -r simulacao/requirements.txt` (apenas `paho-mqtt>=2.0.0`).
Validação offline do contrato (sem broker): `python3 simulacao/teste_contrato.py`.

```bash
# 1. broker (Docker)
docker run -d --name frio-mqtt -p 1883:1883 eclipse-mosquitto:2 \
  sh -c "printf 'listener 1883\nallow_anonymous true\n' > /mosquitto/config/mosquitto.conf && mosquitto -c /mosquitto/config/mosquitto.conf"

# 2. (recomendado) recarregar o seed para estado limpo — ver seção Banco de Dados
# 3. API com a ponte ligada (a partir da raiz do repositório)
Mqtt__Habilitado=true dotnet run --project src/FrioSolidario.Api

# 4. cenários do sensor (adicione --conferir-api para validar alerta e latência via HTTP/JWT)
python3 simulacao/sensor.py --cenario normal --equipamento 1
python3 simulacao/sensor.py --cenario pico-curto --rapido --conferir-api          # 0 alertas (2 fora + retorno)
python3 simulacao/sensor.py --cenario quebra-compressor --rapido --conferir-api   # 1 alerta TEMP_ALTA (equip. 4)
python3 simulacao/sensor.py --cenario porta-aberta --rapido --conferir-api        # 1 alerta PORTA_ABERTA (equip. 6)
python3 simulacao/sensor.py --cenario porta-40s --rapido --conferir-api           # 0 alertas (anti-alarme-falso)

# 5. conferir manualmente via curl (autenticado na instituição 2, dona do equip. 4)
TOKEN=$(curl -s -X POST http://localhost:5199/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rafael.nunes@bairrosolidario.example","senha":"Frio@2026"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
curl -s "http://localhost:5199/api/alertas?status=ABERTO&equipamentoId=4" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:5199/api/equipamentos/4/leituras?periodo=24h" -H "Authorization: Bearer $TOKEN"
```

Argumentos: `--host` (default `localhost`), `--port` (default `1883`), `--equipamento` (default por
cenário: `4` para `quebra-compressor`/`pico-curto`, `6` para `porta-aberta`/`porta-40s`, `1` para
`normal`), `--perfil` (`camara` | `geladeira` | `freezer`, inferido pelo equipamento no `seed.sql`),
`--rapido` (intervalo de 2 s em vez de 120 s), `--conferir-api` (login JWT + checagem automática de
alertas e latência) e `--cenario` (`normal` | `pico-curto` | `quebra-compressor` | `porta-aberta` |
`porta-40s`).

> **Timestamps no `--rapido` e compatibilidade com `db/seed.sql`:** `quebra-compressor` e `pico-curto`
> publicam em tempo real (`agora`), ficando sempre posteriores ao `seed.sql` recém-carregado e visíveis
> de imediato em `GET /leituras?periodo=24h`. Já `porta-aberta` mede duração pelos `registradaEm` e
> ancora em `agora - 6m` (`-6m, -4m, -2m, agora`); se detectar que o `seed.sql` rodou há menos de 6 min,
> ancora `1s` após a última leitura do seed e avança `+100s` por leitura (`5,0 min` totais), sem colidir
> com leituras `portaAberta=false` anteriores nem estourar a tolerância de `+5 min` do `LeituraService`.

**O que a ponte faz com mensagem ruim:** só registra no log e mantém a assinatura. Temperatura fora de
-40..80, JSON inválido, tópico fora do padrão e equipamento inexistente **não** viram leitura, e o
sensor não tem canal para receber erro — o log é a evidência. Medido em
`docs/evidencias/mqtt-ponte-ponta-a-ponta.md`.

## Como apontar o app para a API

`localhost` no celular é o celular. Três caminhos, do mais simples ao mais realista na apresentação:

| Onde o app roda | URL da API | O que precisa |
|---|---|---|
| Emulador Android | `http://10.0.2.2:5199` | nada além do emulador (`10.0.2.2` é o PC visto de dentro dele) |
| Aparelho no USB | `http://localhost:5199` | `adb reverse tcp:5199 tcp:5199` (o cabo leva a porta do PC para o celular) |
| Celular na mesma Wi-Fi | `http://<ip-do-pc>:5199` | API escutando em `0.0.0.0`, a porta liberada no firewall e o app no build **debug** (os builds release e demo só aceitam HTTP sem TLS para `10.0.2.2` e `localhost`) |

Para a API aceitar conexão de fora do PC (Wi-Fi na demo), suba com:

```bash
ASPNETCORE_URLS=http://0.0.0.0:5199 dotnet run --project src/FrioSolidario.Api
```

e libere a porta uma vez: `netsh advfirewall firewall add rule name="Frio Solidario API" dir=in action=allow protocol=TCP localport=5199`.
Com `localhost:5199` a API só responde na própria máquina — é a causa nº 1 de "o app não conecta".

## App mobile (Kevin)

App Android em `mobile/` (Kotlin + Jetpack Compose, Android 7 ou mais novo). Cinco telas e a exportação
do relatório, todas lendo e gravando pela API — não há dado fixo no app:

| Tela | O que faz | Rotas da API |
|---|---|---|
| Login | e-mail e senha; guarda o token até o logout ou o 401 | `POST /api/auth/login` |
| Painel | equipamentos ativos com semáforo e horário da última leitura; atualiza a cada 15 s | `GET /api/equipamentos?ativo=true` (traz `ultimaLeitura`) |
| Detalhe | gráfico de 24 h / 7 d com os limites da faixa, tabela das últimas leituras, relatório em PDF | `GET /api/equipamentos/{id}` (traz `ultimaLeitura`), `.../leituras?periodo=`, `.../relatorio?de=&ate=` |
| Alertas | fila de abertos (atualiza a cada 10 s), resolução com a ação tomada e histórico | `GET /api/alertas?status=`, `PATCH /api/alertas/{id}/resolver` |
| Cadastro | equipamento com faixa; lote doado e saída do estoque — só `ADMIN` vê essas ações | `POST /api/equipamentos`, `GET`/`POST /api/lotes`, `PATCH /api/lotes/{id}/retirada` |

O semáforo usa a faixa **do próprio equipamento**: vermelho com a temperatura fora de `tempMin`..`tempMax`;
amarelo dentro da faixa mas encostando em um limite (10% da largura da faixa, entre 0,5 °C e um quarto
dela), com umidade acima de `umidMax` ou com a porta aberta; verde no resto; cinza sem leitura. No
painel a cor não fica só no texto: cada equipamento é uma linha pintada com o estado — cheia de
vermelho fora da faixa, amarela em atenção, verde clara em ordem. Se a
última leitura tem mais de 30 minutos o cartão vira "Sem sinal", seja qual for a temperatura dela: sensor
parado não prova que o equipamento está frio, e o painel deixa de dizer "tudo dentro da faixa". A regra
está em `mobile/app/src/main/java/br/com/fiap/friosolidario/dominio/Semaforo.kt`.

```bash
cd mobile
./gradlew testDebugUnitTest      # 119 testes, sem aparelho e sem API
./gradlew assembleDemo            # APK para demonstrar em aparelho: sem modo de depuração, bem mais fluido
./gradlew installDebug           # instala no emulador ou no aparelho conectado
```

Precisa do Android SDK (plataforma 36) e de um JDK 17 ou mais novo; abrir a pasta `mobile/` no Android Studio também
funciona. A URL da API fica em **um lugar só**, `frio.apiUrl` em `mobile/gradle.properties` (o código lê
por `core/ApiConfig.kt`). O padrão é o do emulador, `http://10.0.2.2:5199/`; para aparelho no USB:

```bash
adb reverse tcp:5199 tcp:5199
./gradlew installDebug -Pfrio.apiUrl=http://localhost:5199/
```

Os usuários são os da seção "Login (demo)". Erro de conexão, 401, 500, lista vazia e carregamento têm o
mesmo tratamento nas cinco telas (mensagem + "Tentar de novo"); 401 em qualquer tela volta ao login. As
capturas e o roteiro executado estão em `docs/evidencias/app-mobile.md`.

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
| Leituras (ingestão + série) | pronto — `POST /api/leituras` (201), `GET /api/equipamentos/{id}/leituras?periodo=24h\|7d` e `GET .../leituras/ultima` (painel do app) |
| Regra de faixa → alerta | pronto — 3 leituras seguidas fora da faixa, porta aberta por tempo, severidade por desvio; `GET /api/alertas` e `PATCH /api/alertas/{id}/resolver` |
| Testes automatizados (xUnit) | pronto — casos 5, 6 e 7 do plano: **66 testes** em `tests/FrioSolidario.Tests`, incluindo a API por HTTP (`WebApplicationFactory`/TestServer) |
| Relatório de logs em PDF | pronto — `GET /api/equipamentos/{id}/relatorio?de=&ate=` (QuestPDF) |
| Lotes doados | pronto — `/api/lotes` (lista, busca, cadastro e retirada), sempre na instituição do token; alimenta os indicadores de kg doados e perda |
| App mobile | pronto — 5 telas + relatório em PDF integrados à API, em `mobile/`; 119 testes unitários; ver "App mobile (Kevin)" e `docs/evidencias/app-mobile.md` |
| Ação tomada na resolução do alerta | pronto — corpo opcional em `PATCH /api/alertas/{id}/resolver`, coluna `alerta.acao_tomada`; banco já carregado precisa de `db/migracoes/2026-11-15-alerta-acao-tomada.sql` |
| IoT (MQTT) | pronto — ponte na API (`frio/equipamentos/{id}/leitura`) + simulador `simulacao/sensor.py` com 5 cenários e testes de contrato em `simulacao/teste_contrato.py` (ver "Ponte MQTT") |

## Uso de IA

Regra combinada no grupo (reunião de 07/11): cada integrante declara as ferramentas que usou e para quê,
com revisão humana do resultado. Preencha a sua linha — a tabela vai para o PDF da entrega.

| Integrante | RM | Ferramenta de IA | Para quê |
|---|---|---|---|
| André | 554952 | a preencher | a preencher |
| Arthur | 97781 | a preencher | a preencher |
| Kevin | 557898 | Claude Code (Claude) | geração do código e dos testes do app mobile, da ação tomada no alerta (API, schema e teste) e da documentação do app; execução do roteiro de evidências |
| Roger | 97540 | a preencher | a preencher |
| Yasmin | 557413 | a preencher (sugestão: Hermes Agent + Claude Code) | geração e revisão de código da API, testes de integração e revisão do README |

O que vale declarar, sem medo: código gerado com IA e revisado por humano, texto revisado, pesquisa de
referência (Atlas, ANVISA, Lei 15.224/2025) e explicação de erro. Nenhuma senha, token ou credencial de
instituição real foi usada; os dados do projeto são fictícios.

## O que ainda não roda (declarado, não escondido)

| Item | Situação | Dono |
|---|---|---|
| App mobile: impressora física | o roteiro rodou em Fire TV (paisagem) e em celular em pé contra PostgreSQL 18.6, com a prévia de impressão do Android abrindo o PDF (`docs/evidencias/app-mobile.md`); falta só mandar para uma impressora de verdade | Kevin |
| App mobile em tablet | a interface nova foi conferida em celular, com o roteiro de erros repetido (`docs/evidencias/app-mobile.md`); o trilho lateral de tela larga não foi visto em aparelho | Kevin |
| Notificação fora do app | o alerta aparece com o app aberto (atualização a cada 10 s); não há push com o app fechado | grupo |
| Sensor MQTT publicando ao vivo | ponte da API pronta e medida (~1 s) + `simulacao/sensor.py` pronto com 5 cenários e validado contra o contrato (`simulacao/teste_contrato.py`); a demo ao vivo usa sensores simulados via software em vez de ESP32 físico | Roger |
| Testes automatizados (xUnit) abrangentes | os casos 5, 6 e o 401 do caso 7 rodam sem banco (regra, serviço e API via TestServer); login com BCrypt, perfis, CRUD restante e relatório seguem verificados por chamada real, com evidência em `docs/` | Arthur |
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
