# Storyboard para Vídeo do Pitch — Frente IoT e Validação

**Responsável:** Roger · **Formato:** vídeo gravado (não é apresentação ao vivo) · **Duração alvo:** 2 a 3 minutos

> Roteiro para criar o vídeo do pitch com IA: define **narração** e **o que aparece na tela** por cena.
> Os prompts de renderização ficam em [`docs/prompts-renderizacao.md`](prompts-renderizacao.md).
>
> Use em conjunto com o acervo de recursos reais (prints, testes, docs) para fidelidade — só itens que
> de fato existem no projeto entram no vídeo.

---

## 🎬 Storyboard de Cenas

### Cena 0 — Capa (5 s)
**Narração:** *"Frio Solidário — monitoramento da cadeia fria que salva alimentos doados."*

**Na tela:** logo + nome + tagline sobre um fundo de câmera fria/mercado.

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 0

---

### Cena 1 — O problema: a planilha de papel (10 s)
**Narração:** *"Hoje, quem recebe doação de perecível confere a temperatura numa planilha de papel,
assinada por turno. Entre uma leitura e outra — principalmente de madrugada — um freezer pode falhar em
silêncio."*

**Na tela:** close de uma planilha de papel com colunas de temperatura preenchidas à mão; relógio
marcando 03:00; silhueta de uma câmara fria escura.

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 1

---

### Cena 2 — O custo da falha silenciosa (10 s)
**Narração:** *"Uma falha na madrugada, e a doação que ia alimentar famílias vira lixo. Passa a ser
prejuízo e, muitas vezes, risco sanitário."*

**Na tela:** alimentos perecíveis (laticínios/congelados) sendo descartados; transição para o impacto em
famílias.

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 2

---

### Cena 3 — A solução: sensores + MQTT + API + alerta (15 s)
**Narração:** *"Nosso sistema coloca um sensor em cada equipamento: mede temperatura, umidade e abertura
de porta. Um ESP32 com DHT22 e um reed switch publicam a cada dois minutos via MQTT. A API compara com a
faixa segura de cada equipamento e avisa o voluntário antes do lote ser perdido."*

**Na tela:** diagrama animado do fluxo `ESP32 → MQTT → API .NET → App`. Mostrar o app com o alerta
surgindo.

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 3

---

### Cena 4 — Anti-alarme-falso: a prova que evita fadiga (10 s)
**Narração:** *"Portas abrem o tempo todo para carga e descarga. Por isso, exigimos confirmação: uma porta
aberta por 40 segundos não gera nada — é operação normal. Só alerta quando há uma falha de verdade."*

**Na tela:** porta de câmara abrindo e fechando (40 s, sem alarme) vs. porta ficando aberta (sem alarme até
o limite).

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 4

---

### Cena 5 — A quebra do compressor: 3 leituras, 1 alerta (10 s)
**Narração:** *"Se um compressor falha, a temperatura sobe. Na terceira leitura seguida fora da faixa, o
alerta crítico chega — em minutos, bem abaixo dos 15 que o alimento aguenta. E um alerta só: nada de
notificação duplicada a cada leitura."*

**Na tela:** gráfico de temperatura subindo (4 → 6 → 7,8 °C); ponto onde o alerta crítico dispara na 3ª
leitura; mostrando que a 4ª não duplica.

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 5

---

### Cena 6 — Baixo custo e hardware acessível (8 s)
**Narração:** *"E é barato: um ESP32 com dois sensores custa poucas dezenas de reais por ponto — acesso a
telemetria antes só possível com sistemas industriais de milhares de reais."*

**Na tela:** ESP32 físico + DHT22 + reed switch sobre uma mesa; legenda de custo "~R$ 70 por ponto".

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 6

---

### Cena 7 — Fechamento / CTA (8 s)
**Narração:** *"Frio Solidário: comida boa que chega a quem precisa — e nunca vira lixo por uma falha que
ninguém viu."*

**Na tela:** logo + contato do grupo + tagline final sobre fundo acolhedor.

> 🎨 Prompt em `docs/prompts-renderizacao.md` → Cena 7

---

## 🎙️ Narração completa (texto único para locução)

"Frio Solidário — monitoramento da cadeia fria que salva alimentos doados.

Hoje, quem recebe doação de perecível confere a temperatura numa planilha de papel, assinada por turno.
Entre uma leitura e outra — principalmente de madrugada — um freezer pode falhar em silêncio.

Uma falha na madrugada, e a doação que ia alimentar famílias vira lixo. Passa a ser prejuízo e, muitas
vezes, risco sanitário.

Nosso sistema coloca um sensor em cada equipamento: mede temperatura, umidade e abertura de porta. Um
ESP32 com DHT22 e um reed switch publicam a cada dois minutos via MQTT. A API compara com a faixa segura
de cada equipamento e avisa o voluntário antes do lote ser perdido.

Portas abrem o tempo todo para carga e descarga. Por isso, exigimos confirmação: uma porta aberta por 40
segundos não gera nada — é operação normal. Só alerta quando há uma falha de verdade.

Se um compressor falha, a temperatura sobe. Na terceira leitura seguida fora da faixa, o alerta crítico
chega — em minutos, bem abaixo dos 15 que o alimento aguenta. E um alerta só: nada de notificação
duplicada a cada leitura.

E é barato: um ESP32 com dois sensores custa poucas dezenas de reais por ponto — acessível para quem antes
só podia sonhar com telemetria industrial.

Frio Solidário: comida boa que chega a quem precisa — e nunca vira lixo por uma falha que ninguém viu."

---

## 🧪 Limitações e fidelidade (importante para renderização com IA)

- **O hardware físico (ESP32/DHT22/reed switch) não foi montado** — a entrega é via **simulação** em
  `simulacao/sensor.py`. As cenas de hardware (Cena 6) são **ilustrativas**; não afirmar que foi testado
  com hardware real.
- Os **números** (3 leituras, 40 s, 15 min, ~R$ 70) são **reais** e vêm de `RegraDeAlerta.cs` e
  `docs/evidencias-cenarios.md`.
- Os **prints do app** e **testes reais** estão em `docs/evidencias/` e podem substituir quaisquer cenas
  geradas por IA, mantendo fidelidade:
  - App mobile: `docs/evidencias/app/*.png` (login, painel, alerta, relatório PDF).
  - Swagger/terminal: `docs/evidencias/prints/*.png` (swagger + `dotnet test` verde).
- **Recomendação:** para máxima credibilidade, intercale o vídeo gerado por IA com **capturas reais** do
  app e do terminal executando os cenários (`dotnet test` e `unittest`), em vez de só imagens sintéticas.

---

## ✅ Checklist de produção do vídeo

- [ ] Definir duração final (alvo: 2 a 3 min) e cortar cenas se preciso.
- [ ] Gerar cada cena com os prompts de `docs/prompts-renderizacao.md` em estilo consistente.
- [ ] Gravar a locução (texto único da seção acima) ou gerar com IA.
- [ ] **Intercalar capturas reais:** `dotnet test` (68 OK), `unittest` (4/4 OK), prints do app com alerta.
- [ ] Ajustar ritmo: cortar pausas, sincronizar narração com o visual.
- [ ] Revisar fidelidade: não mostrar hardware físico como se tivesse sido testado.
- [ ] Exportar em alta resolução (16:9) com legenda opcional.