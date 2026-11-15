# App mobile — evidências (cards "App mobile", Kevin)

Execução de 15/11/2026, branch `feat/app-mobile`. As capturas estão em `docs/evidencias/app/`.

## Como foi executado (e o que isso não cobre)

| Item | Como foi |
|---|---|
| App | APK de debug (`./gradlew assembleDebug -Pfrio.apiUrl=http://localhost:5199/`) |
| Aparelho | Fire TV Stick (Android 9, API 28), por `adb` em rede, com `adb reverse tcp:5199 tcp:5199`. Tela de 960 × 540 dp em paisagem — equivale a um tablet deitado |
| API | A API deste repositório (controllers, JWT, regra de alerta, QuestPDF), em `http://localhost:5199`, com o `DbContext` trocado pelo provedor **em memória** do EF Core. A máquina usada não tinha PostgreSQL nem Docker disponíveis |
| Dados | Mesmas 3 instituições, 6 usuários e 6 equipamentos do `db/seed.sql`; 7 dias de leituras dentro da faixa gerados na subida; tudo o que sai da faixa entrou por `POST /api/leituras`, passando pela regra de alerta |

O que **não** foi exercitado por esta execução, e precisa de uma passada do grupo antes da entrega:

- **PostgreSQL de verdade.** O `db/schema.sql` com a coluna nova `alerta.acao_tomada` e o
  `db/migracoes/2026-11-15-alerta-acao-tomada.sql` não foram rodados em banco real; a mudança da API
  está coberta por 3 testes xUnit via TestServer (60 no total, todos passando).
- **Celular em pé.** O Fire TV só gira em paisagem. A tela pequena foi simulada em 640 × 360 dp com
  fonte 30% maior (linhas 10 e 11), mas não em retrato.
- **Impressão e leitor de PDF.** O aparelho não tem serviço de impressão nem leitor de PDF. O arquivo
  baixado foi conferido fora dele (é um PDF válido, com o log do período) e a folha de
  compartilhamento do sistema abriu com o arquivo; imprimir de fato fica para um celular.

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

`cd mobile && ./gradlew testDebugUnitTest` — **111 testes**, sem aparelho e sem API: mapeamento de erro
por status e por falha de rede, login, semáforo, escala do gráfico, validação dos formulários, os
repositórios contra um servidor HTTP de teste (MockWebServer), incluindo token vencido e download de
PDF interrompido, e os ViewModels de alertas, detalhe e lotes.

## Depois da revisão da branch

Uma revisão independente do código, feita depois do roteiro acima, levou a cinco correções (cada uma
com teste): o aviso de confirmação ("Alerta resolvido…") era cancelado antes de aparecer; uma leitura
antiga deixava o cartão verde — agora, com mais de 30 minutos sem leitura, o equipamento aparece como
"Sem sinal"; as sugestões de categoria do lote eram uma lista fixa e passaram a vir da API; o "Tentar
de novo" da faixa de erro do detalhe não chamava a API; e uma atualização em andamento podia devolver à
fila, por alguns segundos, um alerta recém-resolvido. As capturas de 1 a 23 são anteriores a essas
correções; o aviso e o "Sem sinal" foram conferidos de novo no aparelho (`50-` e `51-`).
