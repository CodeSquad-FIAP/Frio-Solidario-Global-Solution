# Roteiro de Demo ao Vivo & Defesa para a Banca — Frente IoT

**Responsável:** Roger · **Duração alvo:** 2 a 3 minutos dentro do pitch geral

> Guia prático da apresentação da frente de IoT e validação. Sincroniza o **script falado** com os
> **comandos no terminal** e prepara as respostas às perguntas difíceis da banca.

---

## 1. Script Falado (passo a passo)

### Passo 1 — Abertura: da planilha de papel ao risco silencioso (~30 s)

> **Fala:** *"Hoje, quem recebe doação de perecível confere a temperatura numa planilha de papel,
> assinada por turno. Entre uma leitura e outra — principalmente de madrugada — um freezer pode falhar
> em silêncio e a doação que ia alimentar famílias vira lixo. A validação de campo confirmou: grandes
> centrais já têm telemetria, mas ONGs receptoras e comércios doadores menores não têm como saber que o
> equipamento falhou."*

> **Ação:** mostrar `docs/validacao.md` (recorte de mercado) ou apenas citar a conclusão.

### Passo 2 — Demo ao vivo: o alerta surgindo na aplicação (~60–90 s)

> **Fala:** *"Vou provar com o nosso sistema. Um script simula o sensor — o mesmo contrato MQTT que um
> ESP32 com DHT22 e Reed Switch usaria em produção. Primeiro, o cenário que não deve incomodar
> ninguém: uma porta aberta por 40 segundos, carga e descarga normal."*

**Comando (Cenário 2B — anti-alarme-falso):**
```bash
# broker
docker run -d --name frio-mqtt -p 1883:1883 eclipse-mosquitto:2 \
  sh -c "printf 'listener 1883\nallow_anonymous true\n' > /mosquitto/config/mosquitto.conf && mosquitto -c /mosquitto/config/mosquitto.conf"

# API com a ponte ligada (em outro terminal, a partir da raiz do repositorio)
Mqtt__Habilitado=true dotnet run --project src/FrioSolidario.Api

# script: porta aberta 40s -> 0 alertas (equip. 6 = Padaria Sabor e Partilha)
python3 simulacao/sensor.py --equipamento 6 --cenario porta-40s --rapido --conferir-api
```

> **Fala:** *"Nenhum alerta. Agora o cenário que vende o produto: a quebra do compressor. Três leituras
> seguidas fora da faixa — e só na terceira o alerta aparece, sem duplicar."*

**Comando (Cenário 1 — quebra de compressor, modo rápido):**
```bash
# No App Mobile / Swagger, logar na Instituicao 2 (Mercado Bairro Solidario):
#   email: rafael.nunes@bairrosolidario.example  |  senha: Frio@2026
python3 simulacao/sensor.py --equipamento 4 --perfil freezer --cenario quebra-compressor --rapido --conferir-api
```

> **Fala:** *"Veja o alerta `TEMP_ALTA` surgindo na aplicação. Uma leitura fora não gera nada — evitamos
> falso positivo. Três leituras seguidas confirmam a falha, e o alerta não se repete a cada leitura."*

### Passo 3 — Fechamento: resiliência e custo (~30 s)

> **Fala:** *"O sistema é resiliente: QoS 1 garante que a leitura não se perde se a rede oscilar, e a
> regra de alerta é idempotente — não enche a fila do app durante uma falha real. E é barato: um ESP32
> com DHT22 e Reed Switch custa poucas dezenas de reais por ponto, sem telemetria industrial."*

> **Ação:** apontar para `docs/evidencias-cenarios.md` (matriz de evidências) se a banca pedir prova.

---

## 2. FAQ da Banca — Perguntas Difíceis + Respostas Curtas

### Q1 — "Por que vocês não usaram um sensor físico real?"
**R:** *"A entrega usa simulação com o mesmo contrato MQTT de produção. O hardware físico (ESP32 + DHT22
+ Reed Switch) está especificado e custa poucas dezenas de reais por ponto. A simulação prova a regra de
negócio e a integração ponta a ponta — sensor → MQTT → API → alerta — sem depender de hardware na demo."*

### Q2 — "E se acabar a luz ou a internet na ONG?"
**R:** *"Em produção, o ESP32 tem buffer local em memória flash/NVS: se a internet cair, as leituras
ficam armazenadas e são reenviadas quando a conexão volta. O QoS 1 do MQTT garante que a mensagem não se
perde no meio do caminho. Na demo, o broker é local, então não depende de internet externa."*

### Q3 — "Por que esperar 3 leituras (6 minutos) e não alertar na primeira leitura fora?"
**R:** *"Porque um pico momentâneo — porta abrindo por um minuto, degelo automático — não é uma falha.
Alertar na primeira leitura geraria fadiga de alerta e o operador pararia de confiar no sistema. Três
leituras seguidas confirmam falha contínua em ~6 minutos, bem abaixo da meta de 15 minutos do pitch, e
sem falso positivo."*

### Q4 — "Por que 5 minutos para a porta e não 1 minuto?"
**R:** *"Carga e descarga normal leva dezenas de segundos. Se alertássemos em 1 minuto, toda operação
legítima geraria alerta. Cinco minutos separam a abertura operacional da porta esquecida — a causa mais
comum de perda de frio — mantendo o alerta confiável. Acima de 15 minutos, a severidade sobe para ALTA."*

---

## 3. Checklist do dia da apresentação

- [ ] Broker Mosquitto rodando (`docker ps` mostra `frio-mqtt`).
- [ ] API rodando com `Mqtt__Habilitado=true`.
- [ ] Script `simulacao/sensor.py` testado nos cenários 1 e 2B, com banco recém-carregado do `seed.sql`.
- [ ] Vídeo plano B gravado (ver checklist em `docs/evidencias-cenarios.md`).
- [ ] `docs/validacao.md`, `docs/evidencias-cenarios.md` e `docs/roteiro-pitch.md` acessíveis.
- [ ] Ensaio do FAQ (seção 2) com o grupo.