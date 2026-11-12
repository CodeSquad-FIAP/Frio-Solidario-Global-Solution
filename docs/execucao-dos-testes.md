# Execução dos testes — Frio Solidário

Execução do [plano de testes](plano-de-testes.md) (8 casos na rodada; o caso 9 foi acrescentado depois e
está no fim deste documento) em **11/11/2026**, por Arthur (frente
Segurança + Testes). Cada caso tem um arquivo de evidência em `docs/evidencias/` com a
requisição enviada, a resposta recebida (status, cabeçalhos de segurança e corpo) e a consulta ao
banco que confirma o efeito, e há **27 prints** em `docs/evidencias/prints/`: 21 do Swagger UI e 6 do terminal com as
consultas ao banco (seções "Prints do Swagger" e "Prints do terminal").

## Resultado

**8 de 8 casos passaram, 58 de 58 passos.** Nenhuma falha, e o log da API não registrou erro
inesperado durante a rodada.

| Caso | O que verifica | Passos | Resultado | Cobertura | Evidência |
|---|---|---|---|---|---|
| 1 | Login com BCrypt, mesmo 401 para e-mail inexistente e senha errada | 5/5 | Passou | completa | [caso-01](evidencias/caso-01-login-bcrypt.md) |
| 2 | ADMIN escreve, VOLUNTARIO não (401/403) | 7/7 | Passou | completa | [caso-02](evidencias/caso-02-perfis.md) |
| 3 | Cadastro de equipamento e faixa inválida | 6/6 | Passou | **só a API** (tela do app não executada) | [caso-03](evidencias/caso-03-cadastro-equipamento.md) |
| 4 | Editar, desativar (DELETE lógico, histórico preservado) e série de 24 h | 7/7 | Passou | completa | [caso-04](evidencias/caso-04-manutencao-equipamento.md) |
| 5 | 1 ou 2 leituras fora da faixa não abrem alerta | 7/7 | Passou | completa | [caso-05](evidencias/caso-05-regra-menos-de-3-leituras.md) |
| 6 | 3 leituras seguidas abrem 1 alerta, sem duplicar | 6/6 | Passou | completa | [caso-06](evidencias/caso-06-regra-3-leituras.md) |
| 7 | Token, SQL Injection, XSS, isolamento e limite de login | 13/13 | Passou | completa (10 passos do plano + 3 extras) | [caso-07](evidencias/caso-07-seguranca.md) |
| 8 | Fila de alertas, resolução, 409 e 403 | 7/7 | Passou | **só a API** (lista do app não executada) | [caso-08](evidencias/caso-08-fila-e-resolucao.md) |

Os casos 1, 2, 4, 5, 6 e 7 foram executados por inteiro. Os casos 3 e 8 foram executados pela API, que é
o que o app chama; falta neles a captura da tela do app (ver "O que não foi executado").

## Prints do Swagger

Gerados pelo Swagger UI da própria API (`/swagger`), automatizados no Microsoft Edge, numa rodada
com o banco recarregado e na mesma ordem do plano. Cada print é o bloco da operação **depois de
executar**: corpo enviado, `curl`, URL, status, corpo da resposta e cabeçalhos de segurança. O
token de acesso foi trocado por `<token JWT omitido>` no `curl` e no corpo da resposta; o resto da
imagem não foi alterado. Cada nome de arquivo traz o status HTTP, e a automação conferiu que a
resposta real era a do nome.

| Caso | Prints (`docs/evidencias/prints/`) |
|---|---|
| 1 | `caso-01-a-login-200.png`, `caso-01-b-login-senha-errada-401.png` |
| 2 | `caso-02-a-cadastro-sem-token-401.png`, `caso-02-b-cadastro-voluntario-403.png` |
| 3 | `caso-03-a-cadastro-admin-201.png`, `caso-03-b-faixa-invertida-400.png` |
| 4 | `caso-04-a-serie-24h-200.png`, `caso-04-b-remover-com-leituras-409.png` |
| 5 | `caso-05-a-segunda-leitura-sem-alerta-201.png` |
| 6 | `caso-06-a-terceira-leitura-abre-alerta-201.png`, `caso-06-b-quarta-leitura-sem-duplicar-201.png` |
| 7 | `caso-07-a-sem-token-401.png`, `caso-07-b-sql-injection-email-400.png`, `caso-07-c-sql-injection-senha-401.png`, `caso-07-d-xss-no-nome-400.png`, `caso-07-e-outra-instituicao-403.png`, `caso-07-f-limite-de-login-429.png` |
| 8 | `caso-08-a-fila-de-alertas-200.png`, `caso-08-b-resolver-200.png`, `caso-08-c-resolver-de-novo-409.png`, `caso-08-d-outra-instituicao-403.png` |

Os mais indicados para o PDF e o pitch: `caso-06-a` (o alerta `TEMP_ALTA` abre na 3ª leitura),
`caso-08-a` e `caso-08-b` (fila ordenada e resolução), `caso-02-b` (voluntário barrado) e
`caso-07-f` (limite de login).

## Prints do terminal

Seis capturas do painel de terminal do app, tiradas durante a execução de um roteiro que faz a
ação pela API, mostra o `HTTP` devolvido e roda a consulta `psql` que confirma o efeito no banco
(o `psql` roda no contêiner PostgreSQL 17.11 do ambiente de teste). A captura é da tela, recortada
no painel do terminal, e foi conferida uma a uma. O roteiro foi uma rodada separada, com o banco
recarregado, e termina nos mesmos estados do plano.

| Caso | Print (`docs/evidencias/prints/`) | O que mostra |
|---|---|---|
| 1 | `caso-01-terminal-hashes-bcrypt.png` | os 6 usuários com `senha_hash` `$2a$10$` de 60 caracteres, sem senha em texto |
| 3 | `caso-03-terminal-equipamento-gravado.png` | o equipamento 7 gravado com a faixa -22 a -15 |
| 5 | `caso-05-terminal-nenhum-alerta.png` | 6 leituras, nenhuma sequência de 3 fora da faixa: 0 alertas |
| 6 | `caso-06-terminal-alerta-aberto.png` | 4 leituras a -16 °C: 1 alerta `TEMP_ALTA`, `ALTA`, `ABERTO` (sem duplicar) |
| 8 | `caso-08-terminal-alerta-resolvido.png` | alerta 2 `RESOLVIDO` por 2; alerta 3 (outra instituição) continua `ABERTO` |
| 7 | `caso-07-terminal-xss-nada-gravado.png` | nome com `<script>` devolve 400 e a contagem de equipamentos fica em 7 |

## Ambiente

| Item | Valor |
|---|---|
| Data e hora | 11/11/2026, a partir das 18:33 (fuso -03:00); a rodada dura cerca de 3 minutos |
| API | Frio Solidário API (.NET 10.0.400), compilação limpa (0 avisos, 0 erros), com os cards de segurança aplicados; Development, `http://localhost:5312` |
| Banco | PostgreSQL 17.11 (imagem oficial `postgres:17` em contêiner Docker), `db/schema.sql` + `db/seed.sql` recarregados antes da rodada |
| Cliente | Executor em Python (`urllib`), com os passos do plano e a conferência automática de cada resultado; consultas ao banco por `psql`. Os prints do Swagger usam `puppeteer-core` com o Microsoft Edge; os do terminal são capturas da tela do app |
| Usuários | `mariana.costa` (ADMIN, instituição 1), `ana.rocha` (VOLUNTARIO, instituição 1), `bruna.oliveira` (VOLUNTARIO, instituição 2) |

A senha usada é a demonstrativa do seed (`Frio@2026`, documentada no README). Os tokens de acesso
**não** aparecem nas evidências: cada arquivo mostra só o tamanho e o que o token declara (papel,
instituição, validade).

## Destaques para o PDF

- **Regra de alerta (casos 5 e 6).** No equipamento 4 (faixa -25 a -18 °C), duas leituras a -16 °C não
  abriram alerta, e uma leitura a -20 °C zerou a sequência. Leituras exatamente em -18 °C também não
  contaram como fora da faixa. Na sequência seguinte, as duas primeiras leituras a -16 °C não abriram
  alerta, a **terceira abriu `TEMP_ALTA` com severidade `ALTA`** (desvio de 2,0 °C), e a quarta não
  duplicou. O equipamento 1, que já tinha alerta aberto no seed, também não ganhou duplicata.
- **Isolamento entre instituições (casos 7 e 8).** A fila de alertas da Ana trouxe só os alertas da
  instituição 1, e resolver o alerta 3 (da instituição 2) devolveu 403 sem alterá-lo. Antes do card de
  segurança, um voluntário da instituição 1 conseguia resolver esse alerta (200).
- **Segurança (caso 7).** Sem token, 401; token adulterado, 401; injeção de SQL no e-mail, na senha e no
  filtro foi recusada (400/401/400); nome com `<script>` foi recusado (400) e não gravou nada; a 11ª
  tentativa de login no mesmo minuto voltou 429 com `Retry-After: 60`.
- **Extras do caso 7 (fora dos casos do plano).** Um token assinado com a chave certa e validade futura
  entra (200, é o controle); o mesmo token, vencido há 1 h, é recusado com 401 "Token expirado. Faca
  login novamente."; um token sem assinatura (`alg: none`) é recusado (401). É a evidência do token
  expirado que o plano declarava como não manual.
- **BCrypt (caso 1).** Os 6 usuários têm `senha_hash` com prefixo `$2a$10$` e 60 caracteres; o login com
  a senha errada e com e-mail inexistente devolve exatamente a mesma mensagem.

## O que não foi executado (declarado, não escondido)

| Item | Motivo |
|---|---|
| Telas do app nos casos 3 (cadastro) e 8 (lista de alertas) | O app do Kevin não está neste repositório nem no ambiente de teste. Os dois casos foram executados pela API; as capturas de tela do app precisam ser tiradas por quem roda o app |
| Prints do app | Há prints do Swagger UI e do terminal, mas não das telas do app (Kevin) |
| Sensor MQTT ao vivo | Os casos 5 e 6 enviam as leituras pelo mesmo `POST /api/leituras` que o sensor usa; a publicação MQTT é do Roger |
| Alertas de umidade e de porta aberta | Fora dos 9 casos do plano; ficam para o teste automatizado |

## Observações

- **Reprodução.** Os casos dependem do estado do seed e alteram o banco (cadastram e removem um
  equipamento, abrem e resolvem alertas). Para repetir, recarregue `schema.sql` e `seed.sql` e reinicie
  a API, porque o limite de login (10 por minuto) é guardado em memória.
- **Duração.** A rodada leva cerca de 3 minutos, porque o caso 7 espera 61 segundos para zerar a janela
  do limite de login antes do passo 10 e mais 61 segundos antes dos extras.
- **Entrada de dados.** Os dados de teste são fictícios; nenhuma credencial real foi usada.
- **Dois prints pendentes.** O 401 com token expirado (passo 12 do caso 7) e o `dotnet test` verde
  estão medidos e transcritos nos arquivos de evidência, mas ainda sem captura de tela na pasta
  `prints/`. São os dois únicos itens de evidência em falta.
- **DELETE lógico do equipamento (11/11).** `DELETE /api/equipamentos/{id}` passou a **desativar**
  (`ativo: false`, 204) em vez de apagar: leitura e alerta antigos continuam, e desativar de novo devolve
  409. O caso 4 foi reexecutado com esse comportamento — 204, `ativo: false`, as 5 leituras preservadas e
  o relatório do período ainda abrindo. O print `caso-04-b-remover-com-leituras-409.png` é do
  comportamento **anterior** (remoção física) e fica no repositório como histórico, não como evidência
  atual.

## Caso 9 — relatório em PDF (acrescentado depois da rodada)

O caso 9 entrou no plano porque os 8 casos não tocavam no relatório do período. Foi executado em
**11/11/2026**, no mesmo ambiente (PostgreSQL 17.11 com o seed recarregado e a API do commit `453aa03`,
que corrigiu o fuso do PDF), **pela API em HTTP direto** — sem Swagger UI, então este caso não tem print;
a evidência é o status, os cabeçalhos e o texto do PDF extraído, transcritos abaixo.

**9 de 9 passos.** Os resultados foram reproduzidos e confirmados por Arthur em 11/11/2026, na auditoria da versão atualizada
do repositório (mesmas respostas; o PDF do passo 1 saiu com 2 bytes de diferença, 43.248, e o horário do passo 4 acompanha o
seed recarregado naquela hora).

| # | Passo | Medido |
|---|---|---|
| 1 | `GET /api/equipamentos/1/relatorio` | 200, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename=relatorio-equipamento-1-20261104-20261111.pdf`, corpo com 43.246 bytes começando por `%PDF-` |
| 2 | Cabeçalho do PDF | "FRIO SOLIDARIO - LOG TERMICO", "Banco de Alimentos Caminhos do Bem - Câmara fria de hortifrúti (CAMARA)", faixa 0,0 a 4,0 C, "Periodo (horario de Brasilia): 04/11/2026 00:00 a 11/11/2026 23:59" |
| 3 | Linhas e alertas | 5 leituras em ordem crescente (3,1 °C a 7,6 °C), as 3 últimas "Fora da faixa"; "Alertas no periodo (1)" com `TEMP_ALTA` `CRITICA` `ABERTO` |
| 4 | Fuso do horário | o PDF imprime 19:02, igual a `registrada_em AT TIME ZONE 'America/Sao_Paulo'` no banco; o valor UTC seria 22:02 |
| 5 | `?de=2026-01-01&ate=2026-11-11` | 400 "Periodo maximo do relatorio e de 92 dias." |
| 6 | `?de=2026-11-10&ate=2026-11-01` | 400 "Periodo invalido: de (10/11/2026) e posterior a ate (01/11/2026)." |
| 7 | `?de=2026-10-01&ate=2026-10-02` (sem leitura) | 200, 39.317 bytes; o PDF diz "Sem leituras no periodo." |
| 8 | `GET /api/equipamentos/999/relatorio` | 404 "Equipamento 999 nao encontrado." |
| 9 | `GET /api/equipamentos/1/relatorio` com token da instituição 2 | 403 "Equipamento 1 pertence a outra instituicao." |

Com o caso 9, o plano fecha **9 de 9 casos e 67 de 67 passos**; os 8 primeiros seguem com as evidências
e os prints desta rodada.

## Testes automatizados (xUnit)

Os casos 5, 6 e o 401 do caso 7 do plano também rodam como testes automatizados, sem banco, em
`tests/FrioSolidario.Tests` (`dotnet test`): regra pura, serviço e API de verdade
(`WebApplicationFactory` = TestServer, com os repositórios em memória). O que o plano executa a mão pela
API roda em milissegundos a cada build.

| Arquivo | Testes | O que cobre |
|---|---|---|
| `RegraDeAlertaTests.cs` | 25 (13 métodos, 5 parametrizados) | a regra pura: 1 ou 2 leituras fora da faixa não abrem alerta; a sequência do plano; os limites exatos (-18 e -25) que não contam; 3 leituras abrem `TEMP_ALTA`/`TEMP_BAIXA`; severidade por desvio com os limites 1,5 e 3,0; a faixa vem do equipamento; a ordem de entrada não importa |
| `LeituraServiceTests.cs` | 4 | o fluxo do `POST /api/leituras` com repositórios em memória: a 3ª leitura abre 1 alerta e a resposta o traz; a 4ª não duplica; equipamento que já tem alerta aberto não ganha outro; resolvido o alerta, uma nova sequência abre outro |
| `LeituraMqttTests.cs` | 20 (5 métodos, 2 parametrizados) | a ponte MQTT sem broker: tópico e payload válidos viram `LeituraRequest` com `origem SENSOR`; `snake_case`, número em texto e vírgula decimal (`-18,5`) são aceitos; temperatura/umidade fora da faixa física, JSON inválido, tópico fora do padrão, `registradaEm` não-ISO e `equipamentoId` divergente do tópico são recusados com motivo |
| `ApiTests.cs` | 8 | a API por HTTP (TestServer): `POST /api/leituras` sem token 401; a sequência do caso 5 não abre alerta; a 3ª leitura de 7,8 °C abre `TEMP_ALTA`/`CRITICA` e a 4ª não duplica; `/instituicoes/{id}/equipamentos` da própria instituição 200 e de outra 403; `DELETE` desativa (`ativo: false`) preservando a leitura e repete em 409 |

**Resultado:** 57 testes, 0 falhas (a contagem inclui as variações dos testes parametrizados). Rodam em
`dotnet test FrioSolidario.slnx`, ou por caso: `--filter Caso=5` e `--filter Caso=6`. A saída real desta
rodada está em [`evidencias/xunit-dotnet-test.md`](evidencias/xunit-dotnet-test.md).

**Os testes pegam defeitos?** Para saber se eles protegem a regra, e não só passam, introduzi 10 defeitos
de propósito numa cópia (nunca no repositório) e rodei os testes em cada um. **Os 10 foram pegos**, e com o
código original restaurado os 29 voltaram a passar:

| Defeito introduzido | Testes que falharam |
|---|---|
| exige 2 leituras em vez de 3 | 8 |
| o limite superior (-18) passa a contar como fora da faixa (`>` vira `>=`) | 4 |
| o limite inferior (-25) passa a contar como fora (`<` vira `<=`) | 1 |
| CRÍTICA só acima de 3 °C (`>=` vira `>`) | 2 |
| ALTA só acima de 1,5 °C (`>=` vira `>`) | 1 |
| faixa fixa no código (-18) no lugar da faixa do equipamento | 1 |
| severidade pelo desvio da 1ª leitura, não da última | 1 |
| deixa de ignorar leitura posterior à nova | 1 |
| deixa de bloquear alerta duplicado | 2 |
| `TEMP_BAIXA` sempre `MEDIA` | 2 |

**O que os testes não cobrem:** os alertas de umidade e de porta aberta (a regra existe, mas não está nos
casos 5 e 6), o banco e o HTTP. Login, perfis, CRUD, segurança e relatório continuam verificados pelos casos
manuais e pelos prints acima.
