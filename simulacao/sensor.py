#!/usr/bin/env python3
"""Simulador de sensores IoT do Frio Solidario (DHT22 + Reed Switch via MQTT).

Publica leituras no broker Mosquitto obedecendo 100% ao contrato real de
PonteMqttDeLeituras.cs, LeituraMqttMensagem.cs, LeituraService.cs,
RegraDeAlerta.cs e db/seed.sql:

    topico   frio/equipamentos/{equipamento}/leitura   (QoS 1)
    payload  {"temperatura": ..., "umidade": ..., "portaAberta": ...,
              "registradaEm": "ISO 8601 com offset", "origem": "SIMULADO"}

Notas de integracao com o backend e banco de dados:
- A ponte MQTT aceita campos adicionais (como `origem="SIMULADO"` para
  rastreabilidade no broker) e persiste `OrigemLeitura.SENSOR` no PostgreSQL.
- `quebra-compressor` e `pico-curto` publicam com timestamp em tempo real
  (`agora`), evitando qualquer colisao com leituras recem-carregadas do
  `db/seed.sql` e aparecendo imediatamente em `GET /leituras?periodo=24h`.
- `porta-aberta` no modo `--rapido` ancora os timestamps em `agora - 6m` (ou
  logo apos a ultima leitura `portaAberta=false`, se a API estiver acessivel e
  o `seed.sql` tiver rodado ha menos de 6 min), garantindo `>= 5 min` sem
  estourar a tolerancia de +5 min do `LeituraService`.
- Com `--conferir-api`, autentica via JWT no usuario da instituicao dona do
  equipamento (conforme `db/seed.sql`), confirma a contagem de alertas criados
  e mede a latencia ponta a ponta.
"""

from __future__ import annotations

import argparse
import json
import random
import sys
import time
import urllib.parse
import urllib.request
from datetime import datetime, timedelta
from typing import Any

# paho-mqtt so e importado dentro de conectar()/publicar(): assim --help, a geracao de
# payloads e o teste de contrato (simulacao/teste_contrato.py) funcionam sem a dependencia
# instalada. O import falha apenas quando o usuario realmente vai publicar no broker.

# ---------------------------------------------------------------------------
# Catalogo fiel ao db/seed.sql (6 equipamentos, 3 instituicoes, credenciais demo)
# ---------------------------------------------------------------------------

SENHA_DEMO = "Frio@2026"

EQUIPAMENTOS_SEED: dict[int, dict[str, Any]] = {
    1: {
        "nome": "Camara fria de hortifruti",
        "instituicao_id": 1,
        "instituicao": "Banco de Alimentos Caminhos do Bem",
        "email_admin": "mariana.costa@caminhosdobem.example",
        "perfil": "camara",
        "temp_min": 0.0,
        "temp_max": 4.0,
        "umid_max": 85.0,
        "alerta_aberto_seed": "TEMP_ALTA",
        "porta_aberta_seed": False,
    },
    2: {
        "nome": "Freezer de proteinas",
        "instituicao_id": 1,
        "instituicao": "Banco de Alimentos Caminhos do Bem",
        "email_admin": "mariana.costa@caminhosdobem.example",
        "perfil": "freezer",
        "temp_min": -25.0,
        "temp_max": -18.0,
        "umid_max": 90.0,
        "alerta_aberto_seed": "TEMP_BAIXA",
        "porta_aberta_seed": False,
    },
    3: {
        "nome": "Balcao refrigerado de doacoes",
        "instituicao_id": 2,
        "instituicao": "Mercado Bairro Solidario",
        "email_admin": "rafael.nunes@bairrosolidario.example",
        "perfil": "geladeira",
        "temp_min": 0.0,
        "temp_max": 5.0,
        "umid_max": 80.0,
        "alerta_aberto_seed": "PORTA_ABERTA",
        "porta_aberta_seed": True,
    },
    4: {
        "nome": "Freezer de congelados",
        "instituicao_id": 2,
        "instituicao": "Mercado Bairro Solidario",
        "email_admin": "rafael.nunes@bairrosolidario.example",
        "perfil": "freezer",
        "temp_min": -25.0,
        "temp_max": -18.0,
        "umid_max": 85.0,
        "alerta_aberto_seed": None,
        "porta_aberta_seed": False,
    },
    5: {
        "nome": "Geladeira de excedentes",
        "instituicao_id": 3,
        "instituicao": "Padaria Sabor e Partilha",
        "email_admin": "carla.mendes@saborepartilha.example",
        "perfil": "geladeira",
        "temp_min": 1.0,
        "temp_max": 6.0,
        "umid_max": 78.0,
        "alerta_aberto_seed": "UMIDADE",
        "porta_aberta_seed": False,
    },
    6: {
        "nome": "Camara de resfriamento",
        "instituicao_id": 3,
        "instituicao": "Padaria Sabor e Partilha",
        "email_admin": "carla.mendes@saborepartilha.example",
        "perfil": "camara",
        "temp_min": 0.0,
        "temp_max": 4.0,
        "umid_max": 80.0,
        "alerta_aberto_seed": None,
        "porta_aberta_seed": False,
    },
}

PERFIS: dict[str, dict[str, float]] = {
    "camara": {"temperatura_base": 2.2, "umidade_base": 72.0},
    "geladeira": {"temperatura_base": 3.2, "umidade_base": 70.0},
    "freezer": {"temperatura_base": -20.5, "umidade_base": 70.0},
}

EQUIPAMENTO_PADRAO_POR_CENARIO: dict[str, int] = {
    "normal": 1,
    "pico-curto": 4,
    "quebra-compressor": 4,
    "porta-aberta": 6,
    "porta-40s": 6,
}

INTERVALO_NORMAL = 120.0  # segundos (operacao real: 2 min)
INTERVALO_RAPIDO = 2.0    # segundos (demo ao vivo)

# Sequencia de quebra de compressor: 3 seguidas fora abrem 1 alerta; 4a prova idempotencia.
SEQUENCIA_QUEBRA = [4.0, 6.0, 7.8, 7.9]

# Sequencia de pico curto: apenas 2 leituras fora seguidas de retorno a faixa -> 0 alertas.
SEQUENCIA_PICO_CURTO_FREEZER = [4.0, 6.0, -20.5]
SEQUENCIA_PICO_CURTO_REFRIGERADO = [6.5, 7.2, 2.4]


# ---------------------------------------------------------------------------
# Cliente MQTT confiavel (QoS 1 + confirmacao de publicacao)
# ---------------------------------------------------------------------------


class BrokerDesconectado(Exception):
    """Falha de conexao ou publicacao MQTT."""


def conectar(host: str, port: int) -> Any:
    import paho.mqtt.client as mqtt  # import lazy: so necessario para publicar

    cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    cliente.on_disconnect = _ao_desconectar

    try:
        cliente.connect(host, port, keepalive=60)
    except Exception as exc:
        raise BrokerDesconectado(
            f"nao foi possivel conectar ao broker {host}:{port} ({exc}). "
            "Subiu o Mosquitto? Veja o comando Docker no README.md."
        ) from exc

    cliente.loop_start()
    return cliente


def _ao_desconectar(
    _cliente: Any,
    _userdata: Any,
    _flags: Any,
    motivo: Any,
    _propriedades: Any = None,
) -> None:
    rc = motivo if isinstance(motivo, int) else getattr(motivo, "value", motivo)
    if rc != 0:
        print(f"[mqtt] aviso: desconectado do broker (rc={rc})", file=sys.stderr)


def publicar(cliente: Any, topico: str, payload: dict[str, Any]) -> None:
    """Publica com QoS 1 e falha alto se o broker nao confirmar."""
    import paho.mqtt.client as mqtt  # import lazy: so necessario para publicar

    corpo = json.dumps(payload, ensure_ascii=False)
    info = cliente.publish(topico, corpo, qos=1)
    info.wait_for_publish(timeout=5)

    if info.rc != mqtt.MQTT_ERR_SUCCESS:
        raise BrokerDesconectado(
            f"falha ao publicar em {topico} (rc={info.rc}). Broker caiu?"
        )

    print(f"[mqtt] -> {topico} {corpo}")


# ---------------------------------------------------------------------------
# Helpers de data/hora e integracao opcional com a API HTTP
# ---------------------------------------------------------------------------


def agora_local() -> datetime:
    """Timestamp atual com fuso local (ISO 8601 com offset)."""
    return datetime.now().astimezone()


def iso(dt: datetime) -> str:
    return dt.isoformat(timespec="seconds")


def montar_payload(
    temperatura: float,
    umidade: float,
    porta_aberta: bool,
    registrada_em: datetime,
) -> dict[str, Any]:
    return {
        "temperatura": round(temperatura, 2),
        "umidade": round(umidade, 2),
        "portaAberta": porta_aberta,
        "registradaEm": iso(registrada_em),
        "origem": "SIMULADO",
    }


def _email_admin_do_equipamento(equipamento: int) -> str:
    info = EQUIPAMENTOS_SEED.get(equipamento)
    return info["email_admin"] if info else "mariana.costa@caminhosdobem.example"


def _avisar_conflito_seed(equipamento: int, cenario: str) -> None:
    info = EQUIPAMENTOS_SEED.get(equipamento)
    if not info:
        return

    alerta_seed = info["alerta_aberto_seed"]
    if cenario == "quebra-compressor" and alerta_seed == "TEMP_ALTA":
        print(
            f"[aviso] equipamento {equipamento} ja inicia com alerta TEMP_ALTA ABERTO no seed.sql! "
            "Use --equipamento 4 ou resolva o alerta antes para ver um novo alerta nascer.",
            file=sys.stderr,
        )
    elif cenario in ("porta-aberta", "porta-40s") and (
        alerta_seed == "PORTA_ABERTA" or info["porta_aberta_seed"]
    ):
        print(
            f"[aviso] equipamento {equipamento} ja inicia com portaAberta=true e PORTA_ABERTA ABERTO "
            "no seed.sql! Use --equipamento 6 para um estado inicial limpo.",
            file=sys.stderr,
        )


class ClienteApiDemo:
    """Cliente HTTP opcional (stdlib) para conferir alertas e medir latencia."""

    def __init__(self, base_url: str, email: str, senha: str = SENHA_DEMO) -> None:
        self.base_url = base_url.rstrip("/")
        self.email = email
        self.senha = senha
        self.token: str | None = None

    def autenticar(self) -> str:
        corpo = json.dumps({"email": self.email, "senha": self.senha}).encode("utf-8")
        req = urllib.request.Request(
            f"{self.base_url}/api/auth/login",
            data=corpo,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=4) as resp:
            dados = json.loads(resp.read().decode("utf-8"))
        self.token = str(dados["token"])
        return self.token

    def alertas_abertos(self, equipamento: int) -> list[dict[str, Any]]:
        if not self.token:
            self.autenticar()
        url = (
            f"{self.base_url}/api/alertas?"
            f"{urllib.parse.urlencode({'status': 'ABERTO', 'equipamentoId': equipamento})}"
        )
        req = urllib.request.Request(
            url,
            headers={"Authorization": f"Bearer {self.token}"},
            method="GET",
        )
        with urllib.request.urlopen(req, timeout=4) as resp:
            return json.loads(resp.read().decode("utf-8"))

    def ultima_leitura_em(self, equipamento: int) -> datetime | None:
        if not self.token:
            self.autenticar()
        url = f"{self.base_url}/api/equipamentos/{equipamento}/leituras?periodo=24h"
        req = urllib.request.Request(
            url,
            headers={"Authorization": f"Bearer {self.token}"},
            method="GET",
        )
        with urllib.request.urlopen(req, timeout=3) as resp:
            leituras = json.loads(resp.read().decode("utf-8"))
        if not leituras:
            return None
        texto = str(leituras[-1]["registradaEm"]).replace("Z", "+00:00")
        return datetime.fromisoformat(texto).astimezone()


def _descobrir_ultima_leitura_silencioso(api_url: str, equipamento: int) -> datetime | None:
    """Tenta consultar a ultima leitura na API (timeout curto) para evitar colisao com seed recente."""
    try:
        api = ClienteApiDemo(api_url, _email_admin_do_equipamento(equipamento))
        return api.ultima_leitura_em(equipamento)
    except Exception:
        return None


def _aguardar(rapido: bool, segundos_normais: float) -> None:
    intervalo = INTERVALO_RAPIDO if rapido else segundos_normais
    if intervalo > 0:
        time.sleep(intervalo)


def _imprimir_resumo(
    cenario: str,
    equipamento: int,
    rapido: bool,
    leituras_enviadas: int,
    alertas_esperados: int,
    api_url: str,
) -> None:
    email = _email_admin_do_equipamento(equipamento)
    info = EQUIPAMENTOS_SEED.get(equipamento)
    nome_inst = info["instituicao"] if info else "Instituicao do equipamento"

    print("\n=== RESUMO ===")
    print(f"cenario           : {cenario}")
    print(f"equipamento       : {equipamento} ({info['nome'] if info else 'custom'})")
    print(f"instituicao       : {nome_inst}")
    print(f"modo              : {'--rapido (2s)' if rapido else 'normal (120s)'}")
    print(f"leituras enviadas : {leituras_enviadas}")
    print(f"alertas esperados : {alertas_esperados}")
    print("\nConferencia autenticada na API (rotas exigem JWT da mesma instituicao):")
    print(
        f'  TOKEN=$(curl -s -X POST {api_url}/api/auth/login '
        f'-H "Content-Type: application/json" '
        f'-d \'{{"email":"{email}","senha":"{SENHA_DEMO}"}}\' '
        f"| python3 -c \"import sys,json; print(json.load(sys.stdin)['token'])\")"
    )
    print(
        f'  curl -s "{api_url}/api/alertas?status=ABERTO&equipamentoId={equipamento}" '
        '-H "Authorization: Bearer $TOKEN"'
    )
    print(
        f'  curl -s "{api_url}/api/equipamentos/{equipamento}/leituras?periodo=24h" '
        '-H "Authorization: Bearer $TOKEN"'
    )


def _conferir_na_api(
    api: ClienteApiDemo,
    ids_antes: set[int],
    equipamento: int,
    alertas_esperados: int,
    inicio_mono: float,
) -> int:
    """Confere via HTTP se a ponte MQTT gravou o(s) alerta(s) esperado(s) e mede o tempo."""
    novos: list[dict[str, Any]] = []
    prazo = time.perf_counter() + 6.0

    while time.perf_counter() < prazo:
        atuais = api.alertas_abertos(equipamento)
        novos = [a for a in atuais if int(a["id"]) not in ids_antes]
        if alertas_esperados > 0 and len(novos) >= alertas_esperados:
            break
        if alertas_esperados == 0:
            time.sleep(1.2)
            atuais = api.alertas_abertos(equipamento)
            novos = [a for a in atuais if int(a["id"]) not in ids_antes]
            break
        time.sleep(0.3)

    latencia = time.perf_counter() - inicio_mono
    print("\n=== CONFERENCIA AUTOMATICA NA API ===")
    print(f"tempo 1a leitura -> verificacao : {latencia:.2f}s")
    print(f"alertas novos encontrados       : {len(novos)} (esperado: {alertas_esperados})")
    for alerta in novos:
        print(
            f"  [alerta #{alerta['id']}] tipo={alerta['tipo']} "
            f"severidade={alerta['severidade']} status={alerta['status']} "
            f"criadoEm={alerta['criadoEm']}"
        )

    if len(novos) != alertas_esperados:
        print(
            f"[erro] contagem de alertas divergente do esperado ({len(novos)} != {alertas_esperados}). "
            "Verifique se a API subiu com Mqtt__Habilitado=true e se o seed.sql esta limpo.",
            file=sys.stderr,
        )
        return 2
    return 0


# ---------------------------------------------------------------------------
# Cenarios
# ---------------------------------------------------------------------------


def _topico(equipamento: int) -> str:
    return f"frio/equipamentos/{equipamento}/leitura"


def cenario_normal(cliente: Any, args: argparse.Namespace, perfil: dict[str, float]) -> int:
    intervalo = INTERVALO_RAPIDO if args.rapido else INTERVALO_NORMAL
    print(
        f"[normal] publicando continuamente no equipamento {args.equipamento} "
        f"(perfil {args.perfil}, intervalo {intervalo:.0f}s). Ctrl+C para encerrar."
    )
    try:
        while True:
            temperatura = perfil["temperatura_base"] + random.uniform(-0.4, 0.4)
            umidade = perfil["umidade_base"] + random.uniform(-3.0, 3.0)
            payload = montar_payload(temperatura, umidade, False, agora_local())
            publicar(cliente, _topico(args.equipamento), payload)
            _aguardar(args.rapido, INTERVALO_NORMAL)
    except KeyboardInterrupt:
        print("\n[normal] encerrado pelo usuario (Ctrl+C).")
        return 0


def cenario_pico_curto(
    cliente: Any,
    args: argparse.Namespace,
    api: ClienteApiDemo | None,
    ids_antes: set[int],
) -> int:
    """Prova da Tarefa 4: 1 ou 2 leituras fora da faixa NAO disparam alerta."""
    sequencia = (
        SEQUENCIA_PICO_CURTO_FREEZER
        if args.perfil == "freezer"
        else SEQUENCIA_PICO_CURTO_REFRIGERADO
    )
    print(
        f"[pico-curto] equipamento {args.equipamento}: 2 leituras fora da faixa "
        "seguidas de retorno ao normal -> deve gerar 0 alertas"
    )
    inicio_mono = time.perf_counter()

    for i, temperatura in enumerate(sequencia, start=1):
        payload = montar_payload(temperatura, 74.0, False, agora_local())
        publicar(cliente, _topico(args.equipamento), payload)
        rotulo = "fora da faixa" if i <= 2 else "retorno a faixa (zera contagem)"
        print(f"  -> leitura {i} ({temperatura} C, {rotulo}): 0 alertas")
        if i < len(sequencia):
            _aguardar(args.rapido, INTERVALO_NORMAL)

    _imprimir_resumo("pico-curto", args.equipamento, args.rapido, len(sequencia), 0, args.api_url)
    if api is not None:
        return _conferir_na_api(api, ids_antes, args.equipamento, 0, inicio_mono)
    return 0


def cenario_quebra_compressor(
    cliente: Any,
    args: argparse.Namespace,
    api: ClienteApiDemo | None,
    ids_antes: set[int],
) -> int:
    """4 leituras seguidas acima de temp_max: alerta nasce na 3a e nao duplica na 4a."""
    print(f"[quebra-compressor] equipamento {args.equipamento} (sequencia 4.0 -> 6.0 -> 7.8 -> 7.9 C)")
    inicio_mono = time.perf_counter()

    for i, temperatura in enumerate(SEQUENCIA_QUEBRA, start=1):
        # Timestamp real (`agora_local`): sempre posterior ao db/seed.sql (mesmo recem-carregado)
        # e sempre <= UtcNow (visivel imediatamente em GET /leituras?periodo=24h e consultas.sql).
        payload = montar_payload(temperatura, 75.0, False, agora_local())
        publicar(cliente, _topico(args.equipamento), payload)

        if i == 1:
            print(f"  -> leitura 1 ({temperatura} C): 0 alertas (1a fora da faixa)")
        elif i == 2:
            print(f"  -> leitura 2 ({temperatura} C): 0 alertas (2a fora da faixa)")
        elif i == 3:
            decorrido = time.perf_counter() - inicio_mono
            print(
                f"  -> leitura 3 ({temperatura} C, +{decorrido:.1f}s): "
                "dispara 1 alerta TEMP_ALTA (CRITICA)"
            )
        else:
            print(f"  -> leitura {i} ({temperatura} C): 0 alertas novos (idempotencia — nao duplica)")

        if i < len(SEQUENCIA_QUEBRA):
            _aguardar(args.rapido, INTERVALO_NORMAL)

    _imprimir_resumo(
        "quebra-compressor", args.equipamento, args.rapido, len(SEQUENCIA_QUEBRA), 1, args.api_url
    )
    if api is not None:
        return _conferir_na_api(api, ids_antes, args.equipamento, 1, inicio_mono)
    return 0


def _calcular_timestamps_porta_aberta_rapido(
    agora: datetime,
    ultima_leitura: datetime | None,
) -> list[datetime]:
    """Gera 4 timestamps espacados (total >= 5 min) sem colidir com leituras anteriores
    de porta fechada (ex.: seed.sql recem-carregado) e sem estourar +5 min no futuro."""
    inicio_padrao = agora - timedelta(minutes=6)
    if ultima_leitura is not None and ultima_leitura >= inicio_padrao:
        # O seed.sql (ou porta-40s) rodou ha menos de 6 min: ancoramos 1s apos a ultima
        # leitura (truncado em segundos) e avancamos 100s por passo (total 300s = 5m00s,
        # que ja satisfaz `portaAberta >= TimeSpan.FromMinutes(5)` em RegraDeAlerta.cs)
        # mantendo t4 dentro da tolerancia de +5 min do LeituraService.
        base = ultima_leitura.replace(microsecond=0) + timedelta(seconds=1)
        passo = timedelta(seconds=100)
        return [base + passo * idx for idx in range(4)]

    return [agora - timedelta(minutes=2 * (3 - idx)) for idx in range(4)]


def cenario_porta_aberta(
    cliente: Any,
    args: argparse.Namespace,
    perfil: dict[str, float],
    api: ClienteApiDemo | None,
    ids_antes: set[int],
) -> int:
    """4 leituras com portaAberta=true: alerta PORTA_ABERTA nasce quando acumulado >= 5 min."""
    print(f"[porta-aberta] equipamento {args.equipamento} (porta aberta ininterrupta >= 5 min)")
    inicio_mono = time.perf_counter()

    timestamps_rapidos: list[datetime] | None = None
    if args.rapido:
        ultima = (
            api.ultima_leitura_em(args.equipamento)
            if api is not None
            else _descobrir_ultima_leitura_silencioso(args.api_url, args.equipamento)
        )
        timestamps_rapidos = _calcular_timestamps_porta_aberta_rapido(agora_local(), ultima)

    t0_simulado: datetime | None = None
    for idx in range(4):
        registrada = timestamps_rapidos[idx] if timestamps_rapidos is not None else agora_local()
        if t0_simulado is None:
            t0_simulado = registrada

        acumulado_min = (registrada - t0_simulado).total_seconds() / 60.0
        payload = montar_payload(
            perfil["temperatura_base"],
            perfil["umidade_base"],
            True,
            registrada,
        )
        publicar(cliente, _topico(args.equipamento), payload)

        if acumulado_min >= 5.0:
            print(
                f"  -> leitura {idx + 1} (t={acumulado_min:.1f} min, >= 5 min): "
                "dispara 1 alerta PORTA_ABERTA (MEDIA)"
            )
        else:
            print(f"  -> leitura {idx + 1} (t={acumulado_min:.1f} min): 0 alertas")

        if idx < 3:
            _aguardar(args.rapido, INTERVALO_NORMAL)

    _imprimir_resumo("porta-aberta", args.equipamento, args.rapido, 4, 1, args.api_url)
    if api is not None:
        return _conferir_na_api(api, ids_antes, args.equipamento, 1, inicio_mono)
    return 0


def cenario_porta_40s(
    cliente: Any,
    args: argparse.Namespace,
    perfil: dict[str, float],
    api: ClienteApiDemo | None,
    ids_antes: set[int],
) -> int:
    """Porta aberta por 40s seguida de fechamento -> 0 alertas (anti-alarme-falso)."""
    print(f"[porta-40s] equipamento {args.equipamento} (carga/descarga normal: 40s < 5 min)")
    inicio_mono = time.perf_counter()

    agora = agora_local()
    if args.rapido:
        ultima = (
            api.ultima_leitura_em(args.equipamento)
            if api is not None
            else _descobrir_ultima_leitura_silencioso(args.api_url, args.equipamento)
        )
        candidato = agora - timedelta(seconds=40)
        if ultima is not None and ultima >= candidato:
            t_abertura = min(agora, ultima.replace(microsecond=0) + timedelta(seconds=1))
        else:
            t_abertura = candidato
    else:
        t_abertura = agora

    payload_aberta = montar_payload(
        perfil["temperatura_base"],
        perfil["umidade_base"],
        True,
        t_abertura,
    )
    publicar(cliente, _topico(args.equipamento), payload_aberta)
    print("  -> leitura 1 (portaAberta: true): 0 alertas")

    _aguardar(args.rapido, 40.0)

    t_fechamento = max(agora_local(), t_abertura)

    payload_fechada = montar_payload(
        perfil["temperatura_base"],
        perfil["umidade_base"],
        False,
        t_fechamento,
    )
    publicar(cliente, _topico(args.equipamento), payload_fechada)
    duracao_s = (t_fechamento - t_abertura).total_seconds()
    print(f"  -> leitura 2 (portaAberta: false, apos {duracao_s:.0f}s < 300s): 0 alertas")

    _imprimir_resumo("porta-40s", args.equipamento, args.rapido, 2, 0, args.api_url)
    if api is not None:
        return _conferir_na_api(api, ids_antes, args.equipamento, 0, inicio_mono)
    return 0


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def _inferir_perfil(equipamento: int) -> str:
    info = EQUIPAMENTOS_SEED.get(equipamento)
    if info:
        return str(info["perfil"])
    return "camara"


def _parsear(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Simulador de sensores IoT do Frio Solidario (DHT22 + Reed Switch -> MQTT)."
    )
    _ = parser.add_argument("--host", type=str, default="localhost", help="host do broker MQTT")
    _ = parser.add_argument("--port", type=int, default=1883, help="porta do broker MQTT")
    _ = parser.add_argument(
        "--equipamento",
        type=int,
        default=None,
        help="id do equipamento no db/seed.sql (default automatico por cenario)",
    )
    _ = parser.add_argument(
        "--perfil",
        choices=["camara", "geladeira", "freezer"],
        default=None,
        help="perfil termico (inferido automaticamente pelo equipamento se omitido)",
    )
    _ = parser.add_argument(
        "--rapido",
        action="store_true",
        help="intervalo de 2s entre publicacoes em vez de 120s (para demo ao vivo)",
    )
    _ = parser.add_argument(
        "--cenario",
        choices=["normal", "pico-curto", "quebra-compressor", "porta-aberta", "porta-40s"],
        default="normal",
        help="cenario de simulacao a executar",
    )
    _ = parser.add_argument(
        "--api-url",
        type=str,
        default="http://localhost:5199",
        help="URL base da API .NET para comandos curl e --conferir-api",
    )
    _ = parser.add_argument(
        "--conferir-api",
        action="store_true",
        help="autentica na API, confirma a contagem de alertas criados e mede a latencia",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = _parsear(argv)

    if args.equipamento is None:
        args.equipamento = EQUIPAMENTO_PADRAO_POR_CENARIO[args.cenario]
    if args.perfil is None:
        args.perfil = _inferir_perfil(args.equipamento)

    _avisar_conflito_seed(args.equipamento, args.cenario)
    perfil = PERFIS[args.perfil]

    api: ClienteApiDemo | None = None
    ids_antes: set[int] = set()
    if args.conferir_api:
        api = ClienteApiDemo(args.api_url, _email_admin_do_equipamento(args.equipamento))
        try:
            ids_antes = {int(a["id"]) for a in api.alertas_abertos(args.equipamento)}
        except Exception as exc:
            print(
                f"[erro] falha ao autenticar/consultar API em {args.api_url} ({exc}).",
                file=sys.stderr,
            )
            return 2

    try:
        cliente = conectar(args.host, args.port)
    except BrokerDesconectado as exc:
        print(f"[erro] {exc}", file=sys.stderr)
        return 1

    try:
        if args.cenario == "normal":
            return cenario_normal(cliente, args, perfil)
        if args.cenario == "pico-curto":
            return cenario_pico_curto(cliente, args, api, ids_antes)
        if args.cenario == "quebra-compressor":
            return cenario_quebra_compressor(cliente, args, api, ids_antes)
        if args.cenario == "porta-aberta":
            return cenario_porta_aberta(cliente, args, perfil, api, ids_antes)
        if args.cenario == "porta-40s":
            return cenario_porta_40s(cliente, args, perfil, api, ids_antes)
        raise AssertionError(f"cenario desconhecido: {args.cenario}")
    except BrokerDesconectado as exc:
        print(f"[erro] {exc}", file=sys.stderr)
        return 1
    finally:
        cliente.loop_stop()
        cliente.disconnect()


if __name__ == "__main__":
    sys.exit(main())
