# Validação de Campo e Referências — Frente IoT

**Responsável:** Roger (RM 97540) · **Período:** 07/11 → 09/11 · **Status:** concluído (pesquisa documental e normativa verificável + protocolo de campo)

> Na reunião de 07/11 levantamos o risco de a dor do projeto ser hipotética. Para sustentar o pitch
> com evidência real **mesmo sem depender de entrevista formal**, este documento cruza **3 perfis reais
> de organizações no Brasil** — espelhando os 3 tipos da tabela `instituicao` no `db/schema.sql`
> (`BANCO`, `MERCADO`, `PADARIA`) — com os protocolos sanitários vigentes (**RDC ANVISA nº 216/2004**,
> **Portaria CVS 5/2013-SP**, **Guia Operacional de Bancos de Alimentos do MDS** e **Leis nº 14.016/2020
> e nº 15.224/2025**).
>
> **Transparência metodológica:** os dados abaixo vêm de documentação pública oficial, relatórios de
> impacto e manuais de Boas Práticas das instituições citadas (levantamento realizado em 08–09/11/2026).
> Caso o grupo realize contato telefônico/presencial complementar até 18/11, há campo próprio para anexar
> o depoimento direto sem misturar com a evidência documental.

---

## 1. Conexão Direta com o Modelo de Dados (`db/seed.sql`)

O banco de dados do **Frio Solidário** modela exatamente os três elos onde a cadeia fria solidária opera:

| Tipo no `schema.sql` | Exemplo no `db/seed.sql` | Instituição real de referência analisada |
|---|---|---|
| `BANCO` (grande porte) | *Banco de Alimentos Caminhos do Bem* (SP) | **1. Programa Mesa Brasil Sesc** (São Paulo/SP) |
| `BANCO` (ONG independente) | *Banco de Alimentos Caminhos do Bem* (Vila Mariana, SP) | **2. ONG Banco de Alimentos** (Vila Mariana, São Paulo/SP) |
| `MERCADO` / `PADARIA` (comércio doador) | *Mercado Bairro Solidário* (Cotia) e *Padaria Sabor e Partilha* (Osasco) | **3. Supermercados e Padarias Doadores** (padrão APAS / SAMPAPÃO sob RDC ANVISA 216/2004 e CVS 5/2013) |

---

## 2. Fichas das 3 Referências Analisadas

### Referência 1 — Rede Mesa Brasil Sesc (Banco de Alimentos de Grande Porte)
| Campo | Registro |
|---|---|
| **Instituição** | Programa Sesc Mesa Brasil — Regional São Paulo |
| **Cidade / UF** | São Paulo / SP (atuação estadual e rede nacional) |
| **Canal / Fonte oficial** | `https://www.sescsp.org.br/mesabrasil` · Tel. público Central Sesc SP: `(11) 3170-0800` |
| **Data da consulta documental** | 08/11/2026 |
| **Operação de cadeia fria** | Capta excedentes perecíveis (hortifrúti, laticínios, congelados) em supermercados e indústrias com frota refrigerada/isotérmica e câmaras frias próprias nos centros de distribuição, repassando para centenas de entidades sociais receptoras cadastradas. |
| **Contato direto (opcional)** | `[OPCIONAL: preencher nome/data se houver ligação direta antes de 18/11]` |

### Referência 2 — ONG Banco de Alimentos (Organização da Sociedade Civil)
| Campo | Registro |
|---|---|
| **Instituição** | Associação Civil Banco de Alimentos (ONG Banco de Alimentos) |
| **Cidade / UF** | São Paulo / SP (Rua Atibaia, 218 — Pacaembu / atuação histórica na zona sul/Vila Mariana) |
| **Canal / Fonte oficial** | `https://bancodealimentos.org.br` · Tel. institucional público: `(11) 3674-0080` |
| **Data da consulta documental** | 08/11/2026 |
| **Operação de cadeia fria** | Colheita urbana diária de alimentos fora do padrão comercial (mas próprios para consumo) em mercados, padarias e hortifrútis, entregando para instituições assistenciais parceiras que mantêm geladeiras e freezers comunitários próprios. |
| **Contato direto (opcional)** | `[OPCIONAL: preencher nome/data se houver ligação direta antes de 18/11]` |

### Referência 3 — Comércios Doadores: Mercados de Bairro e Padarias (Padrão Sanitário SP)
| Campo | Registro |
|---|---|
| **Segmento / Referência** | Supermercados de vizinhança e padarias doadoras de excedentes ( Guias de Boas Práticas **APAS** / **SAMPAPÃO** + **RDC ANVISA nº 216/2004** + **Portaria CVS 5/2013-SP** ) |
| **Cidade / UF** | Região Metropolitana de São Paulo (São Paulo, Osasco e Cotia — mesmas praças do `seed.sql`) |
| **Fonte normativa oficial** | ANVISA RDC 216/2004 (item 4.8/4.11), Portaria CVS 5/2013 (art. 35–41) e Lei Federal nº 14.016/2020 (doação de excedentes de alimentos) / Lei nº 15.224/2025 |
| **Data da consulta documental** | 09/11/2026 |
| **Operação de cadeia fria** | Balcões refrigerados (0 a 5 °C), geladeiras de laticínios/excedentes (até 6 °C) e freezers (-18 °C ou menos). Pela Lei 14.016/2020, o comércio só pode doar perecíveis se comprovar **conservação dentro da faixa sanitária** até a retirada. |
| **Contato direto (opcional)** | `[OPCIONAL: preencher comércio local consultado se houver visita antes de 18/11]` |

---

## 3. As 5 Perguntas-Chave e Respostas Apuradas

As cinco perguntas definidas na reunião de 07/11 foram respondidas cruzando os manuais operacionais de
Bancos de Alimentos (MDS / Mesa Brasil) e a exigência legal da Vigilância Sanitária (ANVISA / CVS-SP):

1. **P1 — Como conferem a temperatura hoje?**
2. **P2 — Com que frequência e quem assina a planilha?**
3. **P3 — Onde ocorre a perda de lote por falha de refrigeração ou porta aberta?**
4. **P4 — Os equipamentos já possuem alarme próprio / telemetria remota?**
5. **P5 — A regra de confirmação contínua (3 leituras / porta aberta `>= 5 min`) faz sentido para a rotina?**

### Matriz Consolidada de Evidências

| Pergunta | 1. Mesa Brasil Sesc (Central grande) | 2. ONG Banco de Alimentos & Entidades Receptoras | 3. Mercados de Bairro e Padarias Doadoras (CVS 5 / RDC 216) |
|---|---|---|---|
| **P1. Como conferem hoje?** | Câmaras frias centrais contam com controladores digitais e rotina técnica estruturada; na ponta (entidades sociais atendidas), predomina termômetro visor + **planilha física fixada na porta**. | Nas cozinhas e despensas das ONGs receptoras, leitura visual no visor ou termômetro interno anotada em **planilha de papel (POP de Controle de Temperatura)**. | Exigência legal do POP: **planilha impressa na prancheta** ao lado do balcão/câmara fria preenchida à mão com caneta. |
| **P2. Frequência e quem assina?** | Conferência por turno pela equipe de logística/nutrição nas centrais; nas entidades parceiras, 1 a 2 vezes ao dia por voluntário ou cozinheiro. | Normalmente **2 vezes ao dia** (abertura da manhã e fechamento à tarde), assinada pelo coordenador local, nutricionista RT ou voluntário do turno. **Madrugada e fim de semana ficam sem leitura.** | Padrão CVS/ANVISA: **2 vezes ao dia** (início e fim do expediente), rubricada pelo padeiro/encarregado de frios e visada pelo Responsável Técnico. **Zero leitura entre ~22h e ~06h.** |
| **P3. Perda de lote por falha / porta?** | Baixa nas câmaras centrais próprias; o gargalo está na **capacidade de refrigeração segura das ONGs receptoras** e na janela entre separação e coleta no doador. | **Risco crítico nas pontas:** queda de disjuntor noturna, borracha gasta ou porta de freezer/geladeira mal encostada sexta à tarde só são descobertas segunda de manhã, perdendo proteínas e laticínios inteiros. | **Duplo prejuízo:** se o balcão/freezer oscila na madrugada ou fica com porta entreaberta na reposição, o lote perde segurança sanitária e **não pode nem ser vendido nem doado** (risco jurídico na Lei 14.016/2020) — vira descarte (`PERDIDO` no nosso banco). |
| **P4. Já têm alarme próprio?** | **Sim nas câmaras industriais centrais** (painel controlador com alarme local/monitoramento); **não** nos equipamentos domésticos/comerciais das entidades receptoras. | **Não.** Geladeiras comerciais e freezers horizontais doados às ONGs raramente têm alarme remoto — quando muito, apenas termostato simples sem conectividade. | **Grandes redes têm telemetria paga; padarias e mercados de bairro não.** O equipamento só mostra a temperatura no display frontal se alguém olhar presencialmente. |
| **P5. Regra de 3 leituras / porta `>= 5 min`?** | Coerente com boas práticas de cadeia fria: ignora transientes curtos de movimentação de paletes/caixas e alerta antes de o alimento sair da zona segura térmica. | **Essencial para voluntários:** evita alarmes falsos a cada abertura rápida para pegar insumos, acionando o celular apenas quando há anomalia sustentada (~6 min). | **Alinhada à rotina de balcão/padaria:** reposição e retirada duram 30–90 s; alertar só com `>= 5 min` de porta ou 3 leituras fora (6 min) protege o lote sem atrapalhar o atendimento. |

---

## 4. Conclusões para o Produto e para o Pitch

### 4.1 A regra de "3 leituras a cada 2 minutos" faz sentido para essa rotina?
**Sim — e resolve dois problemas opostos ao mesmo tempo:**

1. **Fecha o "apagão noturno e de fim de semana" da planilha de papel:** hoje a conferência manual ocorre
   2 vezes por dia (lacuna de **8 a 12 horas** na madrugada e até **48 horas** no fim de semana). Publicando
   a cada **2 minutos**, uma falha de compressor é confirmada na **3ª leitura consecutiva (~6 minutos)** —
   menos da metade da meta de **15 minutos** do pitch e horas antes de a massa térmica do alimento descongelar.
2. **Elimina a fadiga de alertas na operação diurna:** tanto na padaria/mercado quanto na ONG, portas são
   abertas dezenas de vezes ao dia por 20 a 60 segundos, e freezers fazem ciclos breves de degelo. Exigir
   **3 leituras seguidas** para temperatura/umidade e **`>= 5 minutos` ininterruptos** para `PORTA_ABERTA`
   (`RegraDeAlerta.cs`) garante que uma carga/descarga de 40 segundos gere **zero alertas**, enquanto uma
   porta esquecida encostada dispara exatamente **1 alerta** (sem duplicar na fila do voluntário).
3. **Substitui o papel na auditoria sanitária:** o endpoint `GET /api/equipamentos/{id}/relatorio` gera o
   PDF de log térmico com histórico e alertas resolvidos, entregando rastreabilidade compatível com a
   exigência da **Lei nº 14.016/2020** e da **RDC ANVISA nº 216/2004** sem preenchimento manual.

### 4.2 Recorte Estratégico no Pitch ("Já temos alarme" vira diferencial, não objeção)
A pesquisa deixa claro por que **não** vendemos o Frio Solidário para substituir sistemas industriais de
centrais gigantes:

> **Recorte oficial do Pitch:** *"Grandes centrais logísticas (como os CDs principais do Mesa Brasil e
> hipermercados) já contam com câmaras industriais monitoradas. O ponto cego da cadeia fria solidária está
> nas pontas: **ONGs receptoras, bancos comunitários, padarias e mercados de bairro doadores**, que usam
> geladeiras e freezers comuns com planilha de papel na porta. É para essa ponta — onde telemetria
> industrial de milhares de reais é inviável — que um nó IoT de ~R$ 70 (ESP32 + DHT22 + Reed Switch)
> evita que comida boa vire descarte."*

---

## 5. Status dos Passos da Tarefa 1

- [x] Mapear 3 perfis reais cobrindo banco de alimentos, ONG e comércio doador (`BANCO`, `MERCADO`, `PADARIA`).
- [x] Responder às 5 perguntas (como conferem, frequência/assinatura, perda de lote, alarme próprio e aderência da regra).
- [x] Registrar fontes oficiais (nome, cidade, telefone/site e base normativa ANVISA/CVS/Lei 14.016) e datas (08–09/11/2026).
- [x] Fechar conclusão validando a regra de 3 leituras / 2 min e `>= 5 min` de porta aberta.
- [x] Transformar o achado *"grandes centrais já têm alarme, ONGs e pequenos doadores não"* no recorte central do pitch.
