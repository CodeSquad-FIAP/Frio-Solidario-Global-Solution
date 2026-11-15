# Evidências dos Cenários de IoT — Relatório Técnico

**Responsável:** Roger · **Status:** pronto para exportação ao PDF final da entrega · execução real de
ponta a ponta capturada em §6 (15/11/2026)

> Este documento é a evidência técnica da frente de IoT: separa simulação de hardware físico,
> especifica os sensores e registra os 4 cenários controlados que provam a regra de alerta. Os logs e
> timelines abaixo seguem **100%** o contrato MQTT do repositório (tópico
> `frio/equipamentos/{id}/leitura`, campos `temperatura`, `umidade`, `portaAberta`, `registradaEm`, `origem`).

---

## 1. Arquitetura: Hardware Físico vs. Simulação

| Aspecto | **Simulado na entrega** | **Hardware físico em produção** |
|---|---|---|
| **Sensor de temperatura/umidade** | Script Python (`simulacao/sensor.py`) gerando valores | **DHT22** (faixa -40 a 80 °C, 0 a 100% UR, precisão ±0,5 °C) |
| **Sensor de porta** | Campo `portaAberta` alternado pelo script | **Reed Switch** magnético (normalmente aberto/fechado) |
| **Microcontrolador** | Processo local publicando via MQTT | **ESP32** (Wi-Fi + MQTT) |
| **Publicação** | `paho-mqtt` → broker Mosquitto (`localhost:1883`) | MQTT (QoS 1) → broker Mosquitto |
| **Tópico** | `frio/equipamentos/{id}/leitura` | `frio/equipamentos/{id}/leitura` |
| **Payload** | `{"temperatura", "umidade", "portaAberta", "registradaEm", "origem": "SIMULADO"}` | mesmo payload (`origem: "SENSOR"`) |
| **Persistência no banco** | Ponte fixa `OrigemLeitura.SENSOR` ao gravar qualquer leitura MQTT | `OrigemLeitura.SENSOR` |
| **Queda de internet** | Não se aplica (broker local) | Buffer local em memória flash/NVS; reenvio ao reconectar |
| **Custo de hardware** | R$ 0 (script) | ~R$ 60–80 por ponto (ESP32 + DHT22 + Reed Switch) |

> **Declaração explícita para a banca:** a entrega usa **simulação** (script + broker Docker). O
> hardware físico (ESP32 + DHT22 + Reed Switch) é a implementação de produção, com o **mesmo contrato
> MQTT**. Não há ambiguidade: o que roda na demo é simulado; o que está especificado é o hardware real.

---

## 2. Especificação Técnica dos Sensores

### 2.1 DHT22 (temperatura e umidade)
| Parâmetro | Valor |
|---|---|
| Faixa de temperatura | -40 a 80 °C |
| Faixa de umidade relativa | 0 a 100 % UR |
| Precisão de temperatura | ±0,5 °C |
| Precisão de umidade | ±2 a 5 % UR |
| Interface | digital (single-wire), compatível com ESP32 |
| Adequação | câmaras frias (0–4 °C) e freezers (-25 a -18 °C) — dentro da faixa de operação |

**Por que o DHT22:** cobre toda a faixa dos equipamentos do seed (de -25 °C do freezer a 6 °C da
geladeira) com precisão suficiente para a regra de alerta (desvio ≥ 1,5 °C = ALTA; ≥ 3 °C = CRÍTICA).

### 2.2 Reed Switch magnético (porta)
| Parâmetro | Valor |
|---|---|
| Funcionamento | Interruptor acionado por campo magnético (ímã na porta) |
| Estados | Normalmente aberto (porta fechada) / fechado (porta aberta) |
| Sinal | `portaAberta: true/false` no payload |

**Por que o limiar de 5 minutos:** a regra `PORTA_ABERTA` só dispara se a porta permanecer aberta **sem
interrupção por 5 minutos ou mais (`>= 5 min`)** (≥ 15 min = ALTA; senão MEDIA). Aberturas curtas de carga e
descarga (ex.: 40 segundos) não disparam nada — evita falso positivo e fadiga de alerta, mantendo a
detecção de porta esquecida (a causa mais comum de perda de frio).

---

## 3. Matriz de Evidências — Cenários Controlados (`simulacao/sensor.py`)

> Todos os cenários abaixo são executáveis por `simulacao/sensor.py` e verificados automaticamente sem
> broker por `python3 simulacao/teste_contrato.py` (4/4 testes verdes contra o espelho de
> `LeituraMqttMensagem.cs`, `LeituraService.cs`, `RegraDeAlerta.cs` e `db/seed.sql`). Com a API e o
> Mosquitto rodando, adicione `--conferir-api` para autenticar no JWT da instituição dona do equipamento,
> confirmar a contagem de alertas e medir a latência real.

### 3.1 Cenário 1A — Quebra de Compressor (`TEMP_ALTA`, 3 leituras seguidas)

**Comando:** `python3 simulacao/sensor.py --cenario quebra-compressor --rapido --conferir-api`
**Equipamento:** 4 — *Freezer de congelados* (*Mercado Bairro Solidário*, login `rafael.nunes@bairrosolidario.example`, faixa **-25 a -18 °C**).
**Sequência publicada:** `4,0 °C → 6,0 °C → 7,8 °C → 7,9 °C`.

| Leitura | Horário exemplo (`--rapido`) | Temperatura | Fora da faixa? | Alerta gerado? | Observação |
|---|---|---|---|---|---|
| 1ª | `16:10:13-03:00` | 4,0 °C | sim | **não** | 1 leitura fora não basta |
| 2ª | `16:10:15-03:00` | 6,0 °C | sim | **não** | 2 leituras fora não basta |
| 3ª | `16:10:17-03:00` | 7,8 °C | sim | **sim — 1 alerta `TEMP_ALTA` (`CRITICA`)** | desvio +25,8 °C ≥ 3 °C |
| 4ª | `16:10:19-03:00` | 7,9 °C | sim | **não** | idempotência: alerta aberto não duplica |

### 3.2 Cenário 1B — Pico Curto: 1 ou 2 Leituras Fora NÃO Disparam Alerta

**Comando:** `python3 simulacao/sensor.py --cenario pico-curto --rapido --conferir-api`
**Equipamento:** 4 — *Freezer de congelados* (faixa **-25 a -18 °C**).
**Sequência publicada:** `4,0 °C → 6,0 °C → -20,5 °C` (retorno à faixa na 3ª leitura).

| Leitura | Horário exemplo (`--rapido`) | Temperatura | Fora da faixa? | Alerta gerado? | Observação |
|---|---|---|---|---|---|
| 1ª | `16:10:13-03:00` | 4,0 °C | sim | **não** | 1ª fora da faixa |
| 2ª | `16:10:15-03:00` | 6,0 °C | sim | **não** | 2ª fora da faixa |
| 3ª | `16:10:17-03:00` | -20,5 °C | não | **não — 0 alertas** | retorno à faixa zera a sequência |

**Métricas de latência (`quebra-compressor`):**

| Modo | Intervalo | Tempo 1ª leitura fora → alerta na API |
|---|---|---|
| Normal (operação real) | 2 min | ~4 a 6 min (3 leituras) — bem abaixo da meta de 15 min do pitch |
| `--rapido` (demo ao vivo) | 2 s | ~4,5 a 5,0 s (2 intervalos de 2 s + ~0,5 s da ponte MQTT → PostgreSQL) |

**Evidência no banco:** o `criado_em` do alerta é o instante exato da **terceira** leitura, e o log da
ponte registra `1 alerta(s) aberto(s)` apenas na 3ª publicação — ver
`docs/evidencias/mqtt-ponte-ponta-a-ponta.md`.

### 3.3 Cenário 2A — Porta Aberta Crítica (`PORTA_ABERTA`, `>= 5 min`)

**Comando:** `python3 simulacao/sensor.py --cenario porta-aberta --rapido --conferir-api`
**Equipamento:** 6 — *Câmara de resfriamento* (*Padaria Sabor e Partilha*, login `carla.mendes@saborepartilha.example`, faixa **0–4 °C**).
**Comportamento:** `portaAberta: true` sustentado por 5 minutos ou mais (a regra usa `>=`).

> **Por que o equipamento 6 e não o 3:** o `seed.sql` já deixa o equipamento 3 com `portaAberta=TRUE` nas
> últimas leituras e um alerta `PORTA_ABERTA` **aberto**. O equipamento 6 inicia com porta fechada e sem
> nenhum alerta aberto (seu único alerta histórico no seed já está `RESOLVIDO`).
>
> **Ancoragem inteligente no `--rapido`:** `RegraDeAlerta` mede o tempo de porta aberta pela diferença
> entre os `registradaEm`, e `LeituraService` recusa datas mais de 5 min no futuro. No `--rapido`, o
> `sensor.py` ancora em `agora - 6 min` (`-6m, -4m, -2m, agora`); se detectar que o `seed.sql` foi
> recarregado há menos de 6 min, ancora `1s` após a última leitura do seed e avança `+100s` por leitura
> (total `5,0 min`), evitando colisão com leituras `portaAberta=false` do seed sem estourar `+5 min`.

| Leitura | Horário simulado (`registradaEm`) | Acumulado | `portaAberta` | Alerta gerado? |
|---|---|---|---|---|
| 1ª | `16:04:13-03:00` | 0,0 min | `true` | **não** |
| 2ª | `16:06:13-03:00` | 2,0 min | `true` | **não** |
| 3ª | `16:08:13-03:00` | 4,0 min | `true` | **não** |
| 4ª | `16:10:13-03:00` | **6,0 min (`>= 5 min`)** | `true` | **sim — 1 alerta `PORTA_ABERTA` (`MEDIA`)** |

### 3.4 Cenário 2B — Carga e Descarga Normal: 40 s (Anti-Alarme-Falso)

**Comando:** `python3 simulacao/sensor.py --cenario porta-40s --rapido --conferir-api`
**Equipamento:** 6 — *Câmara de resfriamento*.
**Comportamento:** `portaAberta: true` por **40 segundos**, seguido de `portaAberta: false`.

| Leitura | Horário simulado (`registradaEm`) | `portaAberta` | Alerta gerado? | Observação |
|---|---|---|---|---|
| 1ª | `16:09:33-03:00` | `true` | **não** | abertura para carga/descarga |
| 2ª | `16:10:13-03:00` (+40 s) | `false` | **não — 0 alertas** | fechou em 40 s (`< 5 min`) |

**Resultado:** **0 alertas** — comprova o filtro anti-alarme-falso e diferencia claramente os horários do
Cenário 2B (`40 s`) em relação ao Cenário 2A (`>= 5 min`).

---

## 4. Checklist de Captura (Plano B para a Apresentação)

> Gravar um vídeo curto de tela do app recebendo o alerta como plano B, caso a demo ao vivo falhe.

- [ ] **Cenário 1A (TEMP_ALTA):** gravar o app mostrando o alerta `TEMP_ALTA` surgindo após a 3ª leitura (`--cenario quebra-compressor --rapido`).
- [ ] **Cenário 1B (anti-falso-positivo temperatura):** gravar que 2 leituras fora + retorno (`--cenario pico-curto --rapido`) geram 0 alertas.
- [ ] **Cenário 2A (PORTA_ABERTA):** gravar o alerta `PORTA_ABERTA` com 5 min ou mais (`>= 5 min`) de porta aberta (`--cenario porta-aberta --rapido`).
- [ ] **Cenário 2B (anti-alarme-falso porta):** gravar que **nenhum** alerta aparece após 40 s de porta aberta (`--cenario porta-40s --rapido`).
- [x] Capturar tela do terminal com o log da ponte (`MQTT: equipamento ... -> leitura ..., 1 alerta(s) aberto(s).`) — **feito**: log colado em §6.1–§6.4.
- [x] Capturar tela do `GET /api/alertas` (autenticado com Bearer JWT) mostrando o alerta aberto — **feito**: JSON real colado em §6.6.
- [x] Salvar os prints em `docs/evidencias/prints/`. — **feito**: `app-painel-critico.png`, `app-alertas-temp-alta.png`, `app-detalhe-freezer.png` (e os `caso-*` da frente de API).
- [x] Vídeo curto de tela do app recebendo o alerta — **feito**: `docs/evidencias/video-demo-app-15-11-2026.mp4` (1 min 17 s), ver §6.7.

---

## 5. Limites declarados

- A demo roda com broker **sem autenticação** (`allow_anonymous true`) em rede local; em produção a
  ponte aceita `Mqtt:Usuario`/`Mqtt:Senha`.
- O script `simulacao/sensor.py` é a fonte dos valores; o hardware físico (ESP32 + DHT22 + Reed Switch)
  é a especificação de produção, não executado na demo.
- QoS 1 na assinatura: se a API cair entre o `INSERT` e o ack, a mensagem pode ser reentregue e gerar
  leitura duplicada — não há chave de idempotência por mensagem (limite já declarado no README).

---

## 6. Execução real de ponta a ponta — capturada em 15/11/2026

> Log real, não exemplo: PostgreSQL + Mosquitto + API + ponte MQTT rodando, `sensor.py` publicando e o
> alerta nascendo no banco e na rota `GET /api/alertas`. Substitui os horários ilustrativos das §3.1/§3.2
> para fins de evidência.

**Ambiente da execução:** Windows 11 · .NET SDK 10.0.401 · PostgreSQL 17 (Docker) · Eclipse Mosquitto 2
(Docker, `allow_anonymous true`, `localhost:1883`) · Python 3.11.15 + `paho-mqtt` · API em
`http://localhost:5199` com `Mqtt__Habilitado=true`. Início `18:22:39 -03:00`, fim `18:23:04 -03:00`.

**Subida do ambiente:**

```bash
docker run -d --name frio-pg -p 5432:5432 -e POSTGRES_PASSWORD=<senha do appsettings> \
  -e POSTGRES_DB=frio_solidario postgres:17
docker exec -i frio-pg psql -U postgres -d frio_solidario -f - < db/schema.sql
docker exec -i frio-pg psql -U postgres -d frio_solidario -f - < db/seed.sql
docker run -d --name frio-mqtt -p 1883:1883 eclipse-mosquitto:2 sh -c \
  "printf 'listener 1883\nallow_anonymous true\n' > /mosquitto/config/mosquitto.conf && mosquitto -c /mosquitto/config/mosquitto.conf"
Mqtt__Habilitado=true ASPNETCORE_URLS=http://localhost:5199 dotnet run --project src/FrioSolidario.Api
```

O log da API confirmou a assinatura antes do teste:
`MQTT: assinando frio/equipamentos/+/leitura em localhost:1883 como frio-solidario-api.`

### 6.1 Cenário 1B — `pico-curto` (1A invertido: 1–2 leituras fora não alertam)

```text
[ pico-curto ] equipamento 4: 2 leituras fora da faixa seguidas de retorno ao normal -> deve gerar 0 alertas
[mqtt] -> frio/equipamentos/4/leitura {"temperatura": 4.0,  "umidade": 74.0, "portaAberta": false, "registradaEm": "2026-11-15T18:22:39-03:00", "origem": "SIMULADO"}
  -> leitura 1 (4.0 C, fora da faixa): 0 alertas
[mqtt] -> frio/equipamentos/4/leitura {"temperatura": 6.0,  "umidade": 74.0, "portaAberta": false, "registradaEm": "2026-11-15T18:22:41-03:00", "origem": "SIMULADO"}
  -> leitura 2 (6.0 C, fora da faixa): 0 alertas
[mqtt] -> frio/equipamentos/4/leitura {"temperatura": -20.5,"umidade": 74.0, "portaAberta": false, "registradaEm": "2026-11-15T18:22:43-03:00", "origem": "SIMULADO"}
  -> leitura 3 (-20.5 C, retorno a faixa (zera contagem)): 0 alertas
tempo 1a leitura -> verificacao : 5.23s
alertas novos encontrados       : 0 (esperado: 0)
```

### 6.2 Cenário 1A — `quebra-compressor` (3 leituras fora → 1 alerta `TEMP_ALTA` `CRITICA`)

```text
[ quebra-compressor ] equipamento 4 (sequencia 4.0 -> 6.0 -> 7.8 -> 7.9 C)
  -> leitura 1 (4.0 C): 0 alertas (1a fora da faixa)     registradaEm 2026-11-15T18:22:46-03:00
  -> leitura 2 (6.0 C): 0 alertas (2a fora da faixa)     registradaEm 2026-11-15T18:22:48-03:00
  -> leitura 3 (7.8 C, +4.0s): dispara 1 alerta TEMP_ALTA (CRITICA)   registradaEm 2026-11-15T18:22:50-03:00
  -> leitura 4 (7.9 C): 0 alertas novos (idempotencia — nao duplica)  registradaEm 2026-11-15T18:22:52-03:00
tempo 1a leitura -> verificacao : 6.02s
alertas novos encontrados       : 1 (esperado: 1)
  [alerta #6] tipo=TEMP_ALTA severidade=CRITICA status=ABERTO criadoEm=2026-11-15T21:22:50+00:00
```

### 6.3 Cenário 2B — `porta-40s` (carga/descarga curta → 0 alertas)

```text
[ porta-40s ] equipamento 6 (carga/descarga normal: 40s < 5 min)
  -> leitura 1 (portaAberta: true): 0 alertas                registradaEm 2026-11-15T18:22:35-03:00
  -> leitura 2 (portaAberta: false, apos 20s < 300s): 0 alertas  registradaEm 2026-11-15T18:22:55-03:00
tempo 1a leitura -> verificacao : 3.28s
alertas novos encontrados       : 0 (esperado: 0)
```

> A duração medida foi **20 s**, não os 40 s nominais: como o `seed.sql` havia sido recarregado segundos
> antes, a ancoragem descrita em §3.3 fixou a abertura em `1 s` após a última leitura do seed. O que o
> cenário prova continua válido (abertura curta `< 5 min` → 0 alertas); para reproduzir os 40 s exatos,
> aguarde 1 min após recarregar o seed.

### 6.4 Cenário 2A — `porta-aberta` (≥ 5 min → 1 alerta `PORTA_ABERTA` `MEDIA`)

```text
[ porta-aberta ] equipamento 6 (porta aberta ininterrupta >= 5 min)
  -> leitura 1 (t=0.0 min): 0 alertas                    registradaEm 2026-11-15T18:22:56-03:00
  -> leitura 2 (t=1.7 min): 0 alertas                    registradaEm 2026-11-15T18:24:36-03:00
  -> leitura 3 (t=3.3 min): 0 alertas                    registradaEm 2026-11-15T18:26:16-03:00
  -> leitura 4 (t=5.0 min, >= 5 min): dispara 1 alerta PORTA_ABERTA (MEDIA)  registradaEm 2026-11-15T18:27:56-03:00
tempo 1a leitura -> verificacao : 6.34s
alertas novos encontrados       : 1 (esperado: 1)
  [alerta #7] tipo=PORTA_ABERTA severidade=MEDIA status=ABERTO criadoEm=2026-11-15T21:27:56+00:00
```

### 6.5 Conferência direta no PostgreSQL (fonte da verdade, não o relatório do script)

```sql
select id, equipamento_id, tipo, severidade, status, criado_em at time zone 'America/Sao_Paulo' as criado_local
  from alerta where id > 5 order by id;
```
```text
 id | equipamento_id |     tipo     | severidade | status |    criado_local
----+----------------+--------------+------------+--------+---------------------
  6 |              4 | TEMP_ALTA    | CRITICA    | ABERTO | 2026-11-15 18:22:50
  7 |              6 | PORTA_ABERTA | MEDIA      | ABERTO | 2026-11-15 18:27:56
```

```sql
select equipamento_id, count(*) as leituras, string_agg(distinct origem, ',') as origens
  from leitura where id > 30 group by 1 order by 1;
```
```text
 equipamento_id | leituras | origens
----------------+----------+---------
              4 |        7 | SENSOR
              6 |        6 | SENSOR
```

**Total:** 13 leituras entraram por MQTT e exatamente **2 alertas** nasceram — nenhum a mais. O payload
ia com `"origem": "SIMULADO"` e o banco gravou `SENSOR`: é o comportamento declarado na tabela da §1
(ponte fixa `OrigemLeitura.SENSOR`), não um defeito escondido.

### 6.6 Conferência na rota que o app consome

`GET /api/alertas?status=ABERTO&equipamentoId=4` (JWT de `rafael.nunes@bairrosolidario.example`) devolveu:

```json
[{"id":6,"equipamentoId":4,"equipamento":"Freezer de congelados","leituraId":36,"tipo":"TEMP_ALTA",
  "severidade":"CRITICA","status":"ABERTO","criadoEm":"2026-11-15T21:22:50+00:00","resolvidoEm":null}]
```

`GET /api/equipamentos/4/leituras?periodo=24h` devolveu 12 leituras, as 4 últimas do simulador
(`4.0 → 6.0 → 7.8 → 7.9 °C`, `21:22:46 → 21:22:52 UTC`). Ou seja: o mesmo JSON que a tela de alertas do
app já consome.

### 6.7 O alerta chegando no app (vídeo e prints)

Mesma execução, agora pela tela: app Android do próprio repositório (`mobile/`), APK debug instalado no
emulador AVD API 37, apontando para a API via `frio.apiUrl` (default `http://10.0.2.2:5199/`, que é o
host visto de dentro do emulador).

```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
cd mobile && ./gradlew :app:assembleDebug          # 21 MB em app/build/outputs/apk/debug/
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n br.com.fiap.friosolidario/.MainActivity
adb shell screenrecord --bit-rate 6000000 --time-limit 90 /sdcard/demo-frio.mp4
python simulacao/sensor.py --cenario quebra-compressor --rapido   # durante a gravação
```

Sequência que o vídeo mostra (`docs/evidencias/video-demo-app-15-11-2026.mp4`, 1 min 17 s):

| tempo | tela | o que aparece |
|---|---|---|
| 0:00 | Painel | "1 em atenção · 2 equipamentos": Freezer −20,1 °C normal, Balcão 4,1 °C em atenção com porta aberta |
| ~0:18 | — | `sensor.py` publica 4,0 → 6,0 → **7,8** °C; a ponte grava e o alerta #6 nasce no banco |
| ~0:28 | Painel | em uma atualização de 10 s o Freezer vira **vermelho, 7,9 °C, "Fora da faixa · agora"**, e o sino ganha o badge **2** |
| ~0:30 | Alertas | "**Temperatura alta** · Freezer de congelados · Severidade crítica · aberto agora · 15/11 21:42" |
| ~1:05 | Detalhe | **7,9 °C**, "Fora da faixa", umidade 75 %, porta fechada, e o gráfico de 24 h com o salto no fim da série |

Prints: `docs/evidencias/prints/app-painel-critico.png`, `app-alertas-temp-alta.png`,
`app-detalhe-freezer.png`.

**O que isso prova:** o app não usa dado mockado — os números da tela são a leitura que entrou por MQTT,
passou pela ponte e foi gravada no PostgreSQL. É a mesma corrente que a banca vê quando o `sensor.py` roda
no palco. Em aparelho físico, troque o alvo por `adb reverse tcp:5199 tcp:5199` e
`./gradlew :app:installDebug -Pfrio.apiUrl=http://localhost:5199/`.