# App mobile — evidências (cards "App mobile", Kevin)

Execução de 15/11/2026, branch `feat/app-mobile`. As capturas estão em `docs/evidencias/app/`.

## Como foi executado (e o que isso não cobre)

| Item | Como foi |
|---|---|
| App | APK de debug (`./gradlew assembleDebug -Pfrio.apiUrl=http://localhost:5199/`) |
| Aparelho | Fire TV Stick (Android 9, API 28), por `adb` em rede, com `adb reverse tcp:5199 tcp:5199`. Tela de 960 × 540 dp em paisagem — equivale a um tablet deitado |
| API | A API deste repositório (controllers, JWT, regra de alerta, QuestPDF), em `http://localhost:5199`, com o `DbContext` trocado pelo provedor **em memória** do EF Core. A máquina usada não tinha PostgreSQL nem Docker disponíveis |
| Dados | Mesmas 3 instituições, 6 usuários e 6 equipamentos do `db/seed.sql`; 7 dias de leituras dentro da faixa gerados na subida; tudo o que sai da faixa entrou por `POST /api/leituras`, passando pela regra de alerta |

Esta primeira execução deixou três lacunas (PostgreSQL de verdade, celular em pé, impressão do PDF).
Elas foram fechadas numa segunda execução, descrita em "Segunda execução: PostgreSQL e celular" no
fim deste documento.

## Fluxo principal

| # | O que foi feito | Resultado | Captura |
|---|---|---|---|
| 1 | Login com senha errada (`errada1`) | "E-mail ou senha incorretos.", tela continua utilizável | — |
| 2 | Login válido (`mariana.costa@caminhosdobem.example`) | Entra no painel; ao sair, o e-mail fica e a senha some do campo | `00-login.png` |
| 3 | Painel com tudo dentro da faixa | Resumo verde, horário do dado em cada cartão ("há 10 min · 03:13") | `01-painel-tudo-dentro-da-faixa.png` |
| 4 | 3 leituras de 7,8 / 7,9 / 8,1 °C na câmara de 0 a 4 °C | Cartão vermelho "Fora da faixa", sino com 1 alerta | `02-painel-com-alerta.png` |
| 5 | Leitura de -18,3 °C no freezer de -25 a -18 °C | Cartão amarelo "Atenção" (dentro da faixa, encostando no limite) | — |
| 6 | Detalhe do equipamento, 24 h e 7 d | Gráfico com dados da API, limites tracejados, pontos fora da faixa em vermelho; o seletor troca só a série | `03-detalhe-grafico.png` |
| 7 | Alerta aberto pelas 3 leituras | Aparece na fila em ~13 s sem tocar na tela | `04-alertas-abertos.png` |
| 8 | Resolver com a ação tomada | Diálogo com as 3 ações; o alerta sai da fila | `05-alerta-acao-tomada.png` |
| 9 | Histórico | Alerta resolvido com a ação e o tempo que ficou aberto; `GET /api/alertas?status=RESOLVIDO` devolve `acaoTomada` | `06-alertas-historico.png` |
| 10 | Cadastro com mínima 5 e máxima 1 | Erro no campo, nada é enviado | `07-cadastro-validacao-da-faixa.png` |
| 11 | Cadastro válido | Equipamento criado na API e visível no painel ao voltar, como "Sem leitura" | — |
| 12 | Lote doado sem validade, depois com validade | Erro no campo; depois criado (12,5 kg) e somado em "Em estoque" | `08-lotes-doados.png` |
| 13 | Relatório em PDF, últimos 7 dias | PDF de 177 978 bytes baixado, com 1 011 leituras; compartilhamento do sistema abre | `09a-relatorio-periodo.png`, `09b-relatorio-pronto.png` |

## Integração e erro de conexão (card "Integração real")

| # | Cenário | Resultado | Captura |
|---|---|---|---|
| 14 | API desligada, abrir o app | Painel: "Sem conexão" + "Tentar de novo" | `20-api-desligada-painel.png` |
| 15 | API desligada, alertas | Mesma tela de erro, com as abas | `21-api-desligada-alertas.png` |
| 16 | API desligada, lotes | Mesma tela de erro | `22-api-desligada-lotes.png` |
| 17 | API desligada, enviar o cadastro | Erro acima do botão, o que foi digitado continua; com a API de volta, "Tentar de novo" salva | `23-api-desligada-cadastro.png` |
| 18 | API cai com o detalhe aberto | Faixa de erro no topo, gráfico e tabela anteriores continuam na tela | `24-api-desligada-detalhe.png` |
| 19 | API desligada, gerar o PDF | Erro dentro do diálogo, com "Tentar de novo" | `25-api-desligada-relatorio-pdf.png` |
| 20 | API desligada, login | "Sem conexão" + "Tentar de novo" (mensagem diferente da de senha errada) | `26-api-desligada-login.png` |
| 21 | Token recusado (401) com o app aberto | Volta ao login com "Sua sessão expirou. Entre de novo." | `40-token-expirado-volta-ao-login.png` |
| 22 | Banco sem equipamentos, alertas e lotes | "Nenhum equipamento cadastrado", "Nenhum alerta aberto", "Nenhum lote doado registrado" | `30-`, `31-`, `32-banco-vazio-*.png` |
| 23 | Tela de 640 × 360 dp com fonte 1,3× | Nenhum texto cortado no painel e no detalhe | `10-`, `11-tela-pequena-fonte-grande-*.png` |
| 24 | Dados fixos no código | `grep` por nomes de equipamento, e-mails, temperaturas e `listOf("` em `mobile/app/src/main`: nenhum resultado. As sugestões de categoria do lote vêm das categorias já usadas em `GET /api/lotes`; os únicos textos fixos são rótulos de tela e os vocabulários da API (tipos, severidades, ações) | — |

Em nenhum dos cenários de 14 a 22 o app fechou: o processo continuou o mesmo e o `logcat` não
registrou `FATAL EXCEPTION`.

## Testes automatizados do app

`cd mobile && ./gradlew testDebugUnitTest` — **121 testes**, sem aparelho e sem API: mapeamento de erro
por status e por falha de rede, login, semáforo, escala do gráfico, validação dos formulários, os
repositórios contra um servidor HTTP de teste (MockWebServer), incluindo token vencido e download de
PDF interrompido, e os ViewModels de painel, alertas, detalhe e lotes.

## Depois da revisão da branch

Uma revisão independente do código, feita depois do roteiro acima, levou a cinco correções (cada uma
com teste): o aviso de confirmação ("Alerta resolvido…") era cancelado antes de aparecer; uma leitura
antiga deixava o cartão verde — agora, com mais de 30 minutos sem leitura, o equipamento aparece como
"Sem sinal"; as sugestões de categoria do lote eram uma lista fixa e passaram a vir da API; o "Tentar
de novo" da faixa de erro do detalhe não chamava a API; e uma atualização em andamento podia devolver à
fila, por alguns segundos, um alerta recém-resolvido. As capturas de 1 a 23 são anteriores a essas
correções; o aviso e o "Sem sinal" foram conferidos de novo no aparelho (`50-` e `51-`).

## Segunda execução: PostgreSQL e celular

Feita em 15/11/2026, depois das correções da revisão, para fechar o que a primeira execução não cobriu.

| Item | Como foi |
|---|---|
| Banco | PostgreSQL 18.6 local, em cluster temporário |
| API | `dotnet run --project src/FrioSolidario.Api`, sem troca de `DbContext`, apontada para esse banco |
| Aparelho | Galaxy S23 Ultra (Android 17, 1080 × 2316, em pé, tema escuro), por `adb` sem fio com `adb reverse tcp:5199 tcp:5199` |

| # | Cenário | Resultado | Captura |
|---|---|---|---|
| 25 | Banco novo: `schema.sql` + `seed.sql` + `verificar.sql` | Os três rodam sem erro com `ON_ERROR_STOP=1` | — |
| 26 | Banco antigo: `schema.sql` da `main` + `seed.sql`, depois o script de `db/migracoes/` | Aplica sem erro; rodar de novo também (só avisos de "já existe"); `pg_dump -s` do banco migrado é idêntico ao do banco novo | — |
| 27 | CHECK `ck_alerta_acao_tomada` direto no banco | `UPDATE` com ação fora da lista e com ação em alerta aberto são recusados | — |
| 28 | `PATCH /api/alertas/{id}/resolver` contra PostgreSQL | `{"acao":"XYZ"}` → 400; `{"acao":"DESCARTOU"}` → 200 e a coluna `acao_tomada` fica `DESCARTOU` | — |
| 29 | Login, painel e detalhe em pé | Nada cortado; o gráfico e os limites cabem na largura | `60-`, `61-celular-*.png` |
| 30 | Resolver alerta pelo app com "Transferi o lote" | Linha do banco fica `RESOLVIDO / TRANSFERIU_LOTE`; o histórico mostra a ação | `63-celular-acao-tomada-postgres.png` |
| 31 | Imprimir o relatório | Abre a impressão do Android com a prévia do PDF (leituras fora da faixa destacadas) e "Salvar como PDF" | `62-celular-imprimir-pdf.png` |
| 32 | Compartilhar o relatório | A folha de compartilhamento abre com permissão de leitura do arquivo concedida | — |
| 33 | Erro 400 que o app deixa passar: lote de 0,001 kg | A frase da API ("Peso em kg deve ser maior que zero.") aparece acima do botão, o que foi digitado continua e nada é gravado | `64-celular-400-da-api.png` |
| 34 | Fonte do sistema em 1,3× | Painel e formulário de equipamento continuam legíveis; no seletor de tipo, "Câmara fria" aparece abreviado | — |
| 35 | API desligada com o painel aberto | Faixa "Sem conexão" e os cartões com a última leitura conhecida | `65-celular-sem-conexao.png` |
| 36 | Build só com JDK 17 | `clean testDebugUnitTest assembleDebug` com apenas o JDK 17 visível e sem download de toolchain: passa | — |

Essa execução achou dois defeitos, já corrigidos:

- **Título invisível no tema escuro.** O "Frio Solidário" do login saía preto sobre fundo escuro. O
  Fire TV usa tema claro, por isso a primeira execução não viu. A raiz do app agora define a cor
  padrão do texto.
- **Equipamento fora da faixa virando "tudo dentro da faixa".** Quando a busca da leitura de um
  equipamento falhava (API caindo no meio da atualização), o cartão passava a "Sem leitura" e o
  resumo dizia "Tudo dentro da faixa". Agora o painel mantém a última leitura conhecida, mostra a
  faixa de erro e, sem leitura anterior, diz "sem resposta" em vez de "tudo dentro da faixa"
  (3 testes novos em `PainelViewModelTest` e 1 em `EquipamentoRepositoryTest`).

Continua sem conferência: enviar o trabalho a uma impressora física e abrir o PDF em um leitor de
terceiros (a prévia de impressão do Android já desenha o arquivo), e o caso em que a leitura de um
só equipamento falha foi reproduzido em teste automatizado, não no aparelho.

## Interface refeita

Depois das duas execuções acima a interface foi redesenhada (ver "Direção visual" em
`docs/superpowers/specs/2026-11-15-app-mobile-design.md`). **As capturas de `00-` a `65-` mostram a
interface antiga**; o comportamento que elas documentam não mudou, só a aparência e a navegação
(Alertas e Lotes saíram do sino e do menu e foram para a barra inferior).

O visual novo foi conferido no Galaxy S23 Ultra, com a API local sobre o banco em memória, em fonte
normal e em 1,3×, e com o sistema em modo escuro (o app continua claro):

| Tela | Captura |
|---|---|
| Login | `70-novo-visual-login.png` |
| Painel com um equipamento fora da faixa e um em atenção | `71-novo-visual-painel.png` |
| Alerta aberto | `72-novo-visual-alertas.png` |
| Detalhe com gráfico | `73-novo-visual-detalhe.png` |
| Lotes | `74-novo-visual-lotes.png` |
| Formulário de lote | `75-novo-visual-formulario.png` |

Depois disso o roteiro foi repetido na interface nova, no mesmo celular, com a API local sobre o
banco em memória:

| # | Cenário | Resultado |
|---|---|---|
| 37 | Equipamento fora da faixa sem alerta ainda (a API só abre na 3ª leitura seguida) | Linha vermelha sem o botão de alerta; na 3ª leitura o botão aparece e leva à aba Alertas |
| 38 | Resolver o alerta com "Ajustei o equipamento" | Aviso "Alerta resolvido: ajustei o equipamento"; o histórico mostra a ação |
| 39 | Detalhe em 7 dias, gerar o PDF e imprimir | Gráfico com 993 leituras; "Relatório pronto"; a prévia de impressão do Android abre o PDF de 25 páginas |
| 40 | Cadastro de equipamento com mínima maior que a máxima | Erro no campo, nada enviado (`76-`); corrigido, o equipamento aparece no painel como "sem leitura" |
| 41 | Marcar um lote como perdido | Confirmação, aviso e o balanço do cabeçalho recalculado |
| 42 | Cadastro de lote sem validade | Erro no campo de validade |
| 43 | API desligada: painel e lotes já carregados | Faixa "Sem conexão" e o conteúdo anterior na tela |
| 44 | API desligada: alertas e detalhe abertos do zero | Tela "Sem conexão" com "Tentar de novo" |
| 45 | API cai com o detalhe aberto | Faixa de erro no topo, leitura e gráfico continuam |
| 46 | API desligada ao gerar o PDF | Erro dentro do diálogo, com "Tentar de novo" |
| 47 | API desligada ao enviar o lote | Erro acima do botão, campos preservados; com a API de volta, "Tentar de novo" salva |
| 48 | Token recusado (401) | Volta ao login com "Sua sessão expirou. Entre de novo." (`77-`) |
| 49 | Login sem API e com senha errada | "Sem conexão…" com "Tentar de novo" e "E-mail ou senha incorretos.", mensagens diferentes |
| 50 | Banco sem equipamentos, alertas e lotes | Os três estados vazios (`78-`) |

Em nenhum desses cenários o app fechou (`logcat` sem `FATAL EXCEPTION`).

Fica sem conferência na interface nova: o trilho lateral de tela larga (tablet) não foi visto em
aparelho nenhum, e os cenários 25 a 28 (PostgreSQL) não dependem da interface e não foram refeitos.
Uma vez durante o roteiro o app apareceu na tela de login sem que o teste tivesse pedido para sair;
não se repetiu ao sair com "voltar" nem ao reiniciar o app, e a causa não foi identificada.

## Ressalvas de revisão tratadas (15/11/2026)

| Ressalva | O que foi feito |
|---|---|
| HTTP liberado para qualquer host | `network_security_config.xml` passou a liberar texto puro só para `10.0.2.2`, `localhost` e `127.0.0.1`; o build de debug mantém uma versão aberta em `src/debug` para testar pela Wi-Fi. Essa versão de debug só entrou no repositório num commit posterior: o `.gitignore` da raiz ignora `[Dd]ebug/` por causa do .NET e o arquivo tinha ficado de fora, como uma revisão apontou. O lint deixou de acusar `InsecureBaseConfiguration` no build demo |
| Umidade máxima de 1 a 100 no app e de 0,01 a 100 na API | O app passou a aceitar a faixa da API (teste novo em `ValidacoesTest`) |
| "Últimos 7 dias" gerava relatório de 8 dias | O período agora conta hoje e os 6 dias anteriores, e o de 30 dias conta 30 (3 testes em `RelatorioViewModelTest`). O padrão da própria API, quando chamada sem datas, tinha o mesmo desvio e também foi corrigido (`RelatorioService`, com teste xUnit; 64 no total) |
| Falta de `dataExtractionRules` | Regras que excluem tudo de backup e de transferência entre aparelhos, mais `fullBackupContent="false"` |
| `OldTargetApi` | Não alterado: o alvo continua na API 36, como no design; subir para 37 a quatro dias da entrega mudaria comportamento sem roteiro refeito |
| Painel baixa a série de 24 h para ler a última leitura | Rota nova na API, `GET /api/equipamentos/{id}/leituras/ultima` (200 com a leitura, 204 sem nenhuma; 3 testes xUnit), e o painel e o cabeçalho do detalhe passaram a usá-la. Na API local a resposta tem 151 bytes, contra 21,6 KB da série de 24 h do mesmo equipamento. O texto "Nenhuma leitura nos últimos 7 dias" virou "Nenhuma leitura recebida ainda", porque a rota não tem janela |

Conferido depois no Galaxy S23 Ultra, com o build demo apontado para `localhost` e a API local:

| Item | Resultado |
|---|---|
| Build demo com a rede restrita | Login mantido, painel, detalhe e PDF funcionam por `localhost` |
| Painel pela rota de última leitura | Os três equipamentos com leitura aparecem com valor e horário; o equipamento recém-cadastrado (204) aparece como "Nenhuma leitura recebida ainda" |
| Relatório | "Últimos 7 dias" mostra 09/11 a 15/11 e gera o PDF; "Últimos 30 dias" mostra 17/10 a 15/11 |
| Umidade máxima de 0,5% | Aceita no formulário e gravada pela API |

Não foi feito o teste negativo no aparelho (um host fora da lista sendo recusado por texto puro): a
restrição foi conferida no arquivo empacotado nos dois APKs (debug aberto, demo restrito), não em
execução. O número de requisições do painel também não foi medido no aparelho; quem garante que ele
não pede mais a série é o teste `painel pede so a ultima leitura, nunca a serie do grafico`.
