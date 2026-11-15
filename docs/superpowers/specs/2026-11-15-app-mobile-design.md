# App mobile Frio Solidário — design

Aprovado por Kevin em 15/11/2026. Cobre os 7 cards "App mobile" do board
(projeto + login, painel com semáforo, detalhe com gráfico, alertas com resolução,
cadastro de equipamento + lote, botão de PDF, integração real e erro de conexão).

## Intenção

O voluntário abre o app e responde "está tudo bem?" em 3 segundos; quando não está, o
alerta chega, ele registra o que fez e o histórico sai impresso no lugar da planilha de
papel da vigilância sanitária. O edital pontua (3,0) as telas **integradas à API**, com
tratamento de erro de conexão.

## Objetivos

- 5 telas (login, painel, detalhe, alertas, cadastro) + exportação do PDF, lendo e
  gravando só pela API. Nenhum dado fixo no app.
- Derrubar a API e navegar por todas as telas não gera crash: mensagem + "tentar de novo".
- 401 em qualquer tela limpa a sessão e volta ao login.
- Layout sem texto cortado em tela pequena e em tablet.

## Fora do escopo

- Cadastro de usuário e de instituição, edição/remoção de equipamento, notificações push,
  modo offline, iOS.

## O que seria fracasso mesmo funcionando

- Semáforo calculado com valor fixo no app em vez da faixa do equipamento.
- Horário exibido como "agora" em vez do horário do dado.
- Ação tomada escolhida na tela e não gravada em lugar nenhum.

## Abordagens consideradas

1. **Kotlin + Jetpack Compose (escolhida).** É a stack do app anterior do Kevin
   (Vinheria Agnello); Android SDK e Java 17 já estão instalados; o card pede folha de
   compartilhamento/impressão do Android, que é nativa aqui.
2. React Native/Expo. Exigiria montar um toolchain novo a 4 dias da entrega e módulos
   nativos para PDF/impressão.
3. Flutter. Mesmo custo de toolchain, sem ninguém do grupo com histórico.

## Decisões

| Tema | Decisão |
|---|---|
| Local | `mobile/` na raiz do repositório, projeto Gradle independente da solução .NET |
| Pacote | `br.com.fiap.friosolidario`, minSdk 24, compile/target 36 |
| Arquitetura | Activity única, Navigation Compose, um ViewModel por tela, repositórios sobre Retrofit, injeção manual (`AppContainer`) |
| Rede | Retrofit + OkHttp + kotlinx.serialization; timeouts de 10 s |
| URL da API | Um único ponto: `core/ApiConfig.kt`, alimentado por `frio.apiUrl` em `mobile/gradle.properties` (padrão `http://10.0.2.2:5199/`) |
| Sessão | Token, validade, nome, papel e instituição em DataStore; logout limpa tudo |
| Erros | Um tipo comum (`ErroApp`) e um componente comum (mensagem + "Tentar de novo") nas 5 telas; `detail` do ProblemDetails é a mensagem de 400/409 |
| Semáforo | Vermelho: temperatura fora de `tempMin..tempMax`. Amarelo: dentro da faixa mas a menos de 10% da largura da faixa (mínimo 0,5 °C, máximo um quarto da faixa) de um limite, ou umidade acima de `umidMax`, ou porta aberta. Verde: o resto. Cinza: sem leitura |
| Última leitura | A lista de equipamentos da API não traz leitura; o app busca a série de 24h de cada equipamento (7d se vier vazia) e usa a última. Sem mudança na API |
| Gráfico | Desenhado em Canvas do Compose (linha da série, limites tracejados, pontos fora da faixa em vermelho); sem biblioteca de gráfico |
| Alertas | Abas "Abertos" e "Histórico"; atualização automática a cada 10 s; resolver abre diálogo com a ação tomada |
| Ação tomada | Mudança compatível na API: corpo **opcional** `{"acao":"AJUSTOU"\|"TRANSFERIU_LOTE"\|"DESCARTOU"}` em `PATCH /api/alertas/{id}/resolver`, coluna `alerta.acao_tomada` (nula para alertas antigos) e campo `acaoTomada` na resposta. Sem corpo continua funcionando como hoje |
| Cadastro | Só ADMIN vê as ações de cadastro (a API devolve 403 para VOLUNTARIO). Equipamento valida `tempMin < tempMax` e os limites físicos antes do envio |
| Lote | A API cria o lote sempre `DISPONIVEL`; o formulário mostra o status fixo e a lista de lotes permite registrar a saída (`DISTRIBUIDO`/`PERDIDO`) |
| PDF | Período padrão de 7 dias, com opção de 30 dias ou datas escolhidas (até 92 dias, limite da API); salva em cache e oferece Compartilhar (folha do sistema) e Imprimir (PrintManager) |
| Datas | A API manda UTC; o app converte para o fuso do aparelho na exibição |
| Idioma | Interface em português |

## Verificação

API real rodando na máquina (.NET 10 + PostgreSQL com `db/schema.sql` e `db/seed.sql`),
app instalado em aparelho físico por adb. Testes unitários (JUnit + MockWebServer) para o
semáforo, o mapeamento de erros, a validação dos formulários e os repositórios; a mudança
da API entra com teste xUnit.

## Entrega

Branch `feat/app-mobile`, um commit por alteração relevante, push a cada etapa. Sem merge
na `main` até o Kevin pedir.
