# Ponte MQTT → API: publicação do broker vira leitura e alerta

**O que prova:** o critério do card "Leituras: POST /leituras" — *uma publicação do script do Roger
aparece no endpoint em segundos*. Até 11/11 a API só aceitava HTTP; agora existe a ponte que assina o
tópico do broker e grava pelo mesmo `LeituraService` do endpoint.

**Ambiente:** Windows 11, .NET 10.0.401, API em `http://localhost:5199`, PostgreSQL 17.11 em container
(`frio-pg`, `schema.sql` + `seed.sql` do zero), broker `eclipse-mosquitto:2` em container (`frio-mqtt`,
`1883`). Data: 12/11/2026 00:16 (America/Sao_Paulo).

```console
$ docker run -d --name frio-mqtt -p 1883:1883 eclipse-mosquitto:2 \
    sh -c "printf 'listener 1883\nallow_anonymous true\n' > /mosquitto/config/mosquitto.conf \
           && mosquitto -c /mosquitto/config/mosquitto.conf"

$ Mqtt__Habilitado=true ASPNETCORE_URLS=http://localhost:5199 dotnet run --no-build --no-launch-profile
info: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: assinando frio/equipamentos/+/leitura em localhost:1883 como frio-solidario-api.
```

## 1. Três publicações → três leituras e um alerta

Equipamento 4 (Freezer de congelados, faixa **-25 a -18 °C**), três leituras acima do máximo:

```console
$ for t in -10.2 -10.1 -9.9; do
    docker exec frio-mqtt mosquitto_pub -h localhost \
      -t "frio/equipamentos/4/leitura" -m "{\"temperatura\": $t, \"umidade\": 60, \"portaAberta\": false}"
    sleep 1
  done

$ docker exec frio-pg psql -U postgres -d frio_solidario -c \
    "select id, equipamento_id, temperatura, origem, registrada_em from leitura where equipamento_id=4 order by id desc limit 4;"
 id | equipamento_id | temperatura | origem |        registrada_em
----+----------------+-------------+--------+------------------------------
 36 |              4 |       -9.90 | SENSOR | 2026-11-12 00:16:53.238324+00
 35 |              4 |      -10.10 | SENSOR | 2026-11-12 00:16:51.966272+00
 34 |              4 |      -10.20 | SENSOR | 2026-11-12 00:16:50.724389+00
 20 |              4 |      -20.10 | SENSOR | 2026-11-12 00:16:14.504302+00   <- seed

$ docker exec frio-pg psql -U postgres -d frio_solidario -c \
    "select id, equipamento_id, tipo, severidade, status, criado_em from alerta where equipamento_id=4;"
 id | equipamento_id |   tipo    | severidade | status |          criado_em
----+----------------+-----------+------------+--------+------------------------------
  6 |              4 | TEMP_ALTA | CRITICA    | ABERTO | 2026-11-12 00:16:53.238324+00
```

`criado_em` do alerta é o instante da **terceira** leitura: a regra das 3 seguidas rodou dentro da
ponte, não em um caminho paralelo. Do `mosquitto_pub` ao `INSERT` do alerta: **1 segundo**.

```console
info: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: equipamento 4 -9.9C / 60% porta fechada -> leitura 36, 1 alerta(s) aberto(s).
```

## 2. Equipamento que já tem alerta aberto não duplica

Mesmo tópico do equipamento 1 (Câmara fria, 0 a 4 °C), 3 leituras de 7,8 / 7,9 / 8,1 °C — todas fora
da faixa, mas o seed já deixou um `TEMP_ALTA` aberto para ele:

```console
info: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: equipamento 1 7.8C / 55% porta fechada -> leitura 31, 0 alerta(s) aberto(s).
      MQTT: equipamento 1 7.9C / 56% porta aberta -> leitura 32, 0 alerta(s) aberto(s).
      MQTT: equipamento 1 8.1C / 57% porta fechada -> leitura 33, 0 alerta(s) aberto(s).
```

Leituras gravadas, alerta não repetido — exatamente a regra do `POST /api/leituras`.

## 3. Mensagem ruim é recusada sem derrubar a assinatura

```console
$ mosquitto_pub -t "frio/equipamentos/4/leitura" -m '{"temperatura": 200, "umidade": 60}'
$ mosquitto_pub -t "frio/equipamentos/99/leitura" -m '{"temperatura": -20, "umidade": 60}'
$ mosquitto_pub -t "frio/equipamentos/4/leitura" -m 'nao-e-json'
$ mosquitto_pub -t "frio/equipamentos/4/leitura" -m '{"temperatura": -20, "umidade": 60}'   <- válida

warn: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: mensagem ignorada em frio/equipamentos/4/leitura — temperatura 200 fora da faixa fisica (-40 a 80).
warn: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: leitura do equipamento 99 recusada — Equipamento 99 nao encontrado..
warn: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: mensagem ignorada em frio/equipamentos/4/leitura — payload nao e JSON valido (...).
info: FrioSolidario.Infrastructure.Mqtt.PonteMqttDeLeituras[0]
      MQTT: equipamento 4 -20C / 60% porta fechada -> leitura 37, 0 alerta(s) aberto(s).
```

Só a quarta virou linha (`id 37`); as três primeiras não chegaram ao banco e a assinatura continuou
de pé — a mensagem seguinte foi processada normalmente.

## 4. Desligada, não abre socket

`Mqtt:Habilitado=false` no `appsettings.json` (padrão). `dotnet test` roda **57 testes, 0 falhas** sem
broker; com a ponte ligada, o mesmo build serve a demo. O parser (`LeituraMqttMensagem`) tem 20 testes
próprios: `snake_case`, número em texto, vírgula decimal, faixa física, tópico fora do padrão e
`equipamentoId` divergente do tópico.

## Limites declarados

- O broker da demo roda **sem autenticação** (`allow_anonymous true`) e em rede local; em produção a
  ponte já aceita `Mqtt:Usuario`/`Mqtt:Senha`.
- **O script do sensor não existe no repositório** — o que foi medido usa `mosquitto_pub` com o mesmo
  payload combinado. O dono do script é o Roger; o contrato está no README.
- QoS 1 na assinatura, não exatamente-uma-vez: se a API cair entre o `INSERT` e o ack, a mensagem pode
  ser reentregue e gerar leitura duplicada. Não há chave de idempotência por mensagem.
