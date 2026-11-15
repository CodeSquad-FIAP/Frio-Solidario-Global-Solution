#!/usr/bin/env python3
"""Testes automatizados do simulador IoT contra o contrato real da API e do db/seed.sql.

Reproduz em Python puro as mesmas regras de:
  - src/FrioSolidario.Infrastructure/Mqtt/LeituraMqttMensagem.cs
  - src/FrioSolidario.Application/Services/LeituraService.cs
  - src/FrioSolidario.Application/Services/RegraDeAlerta.cs
  - db/seed.sql

Roda sem broker e sem pip externo:
    python3 simulacao/teste_contrato.py
"""

from __future__ import annotations

import json
import sys
import types
import unittest
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from unittest import mock

# Se `paho-mqtt` nao estiver instalado na maquina de CI/teste, injeta um stub leve
# antes de importar `sensor.py`.
try:
    import paho.mqtt.client  # type: ignore[import-not-found]
except ModuleNotFoundError:
    modulo_paho = types.ModuleType("paho")
    modulo_mqtt = types.ModuleType("paho.mqtt")
    modulo_client = types.ModuleType("paho.mqtt.client")
    setattr(modulo_client, "MQTT_ERR_SUCCESS", 0)

    class _CallbackAPIVersion:
        VERSION2 = 2

    class _PublishInfo:
        def __init__(self) -> None:
            self.rc = 0

        def wait_for_publish(self, timeout: float | None = None) -> None:
            _ = timeout

    class _StubClient:
        def __init__(self, *args: object, **kwargs: object) -> None:
            _ = (args, kwargs)
            self.on_disconnect = None

        def connect(self, *args: object, **kwargs: object) -> int:
            _ = (args, kwargs)
            return 0

        def loop_start(self) -> None:
            return None

        def loop_stop(self) -> None:
            return None

        def disconnect(self) -> None:
            return None

        def publish(self, topic: str, payload: str, qos: int = 0) -> _PublishInfo:
            _ = (topic, payload, qos)
            return _PublishInfo()

    setattr(modulo_client, "CallbackAPIVersion", _CallbackAPIVersion)
    setattr(modulo_client, "Client", _StubClient)
    setattr(modulo_paho, "mqtt", modulo_mqtt)
    setattr(modulo_mqtt, "client", modulo_client)
    sys.modules["paho"] = modulo_paho
    sys.modules["paho.mqtt"] = modulo_mqtt
    sys.modules["paho.mqtt.client"] = modulo_client

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sensor  # noqa: E402


@dataclass
class LeituraBanco:
    id: int
    equipamento_id: int
    temperatura: float
    umidade: float
    porta_aberta: bool
    registrada_em: datetime
    origem: str


@dataclass
class AlertaBanco:
    id: int
    equipamento_id: int
    leitura_id: int
    tipo: str
    severidade: str
    status: str
    criado_em: datetime


class SimuladorBackendFiel:
    """Espelho fiel de LeituraMqttMensagem + LeituraService + RegraDeAlerta + db/seed.sql."""

    def __init__(self, instante_seed: datetime, relogio_inicial: datetime | None = None) -> None:
        self.relogio: datetime = relogio_inicial or datetime.now().astimezone()
        self.leituras: list[LeituraBanco] = []
        self.alertas: list[AlertaBanco] = []
        self._prox_leitura_id = 31
        self._prox_alerta_id = 6
        self._carregar_seed(instante_seed)

    def _carregar_seed(self, t_seed: datetime) -> None:
        # Leituras relevantes do db/seed.sql (5 por equipamento, de -8m ate t_seed)
        amostras = {
            1: [(3.1, 71, False), (3.3, 72, False), (7.0, 73, False), (7.3, 74, False), (7.6, 75, False)],
            2: [(-21.8, 68, False), (-22.2, 67, False), (-26.2, 68, False), (-26.4, 69, False), (-26.6, 69, False)],
            3: [(3.1, 70, False), (3.7, 71, True), (3.5, 73, True), (3.9, 74, True), (4.1, 75, True)],
            4: [(-20.2, 75, False), (-20.0, 75, False), (-20.5, 76, False), (-21.0, 75, False), (-20.1, 75, False)],
            5: [(3.0, 75, False), (3.2, 76, False), (3.1, 81, False), (3.6, 83, False), (3.8, 87, False)],
            6: [(6.7, 76, False), (6.9, 76, False), (7.2, 77, False), (3.4, 76, False), (3.0, 75, False)],
        }
        lid = 1
        for eq_id, lista in amostras.items():
            for idx, (temp, umid, porta) in enumerate(lista):
                dt = t_seed - timedelta(minutes=2 * (4 - idx))
                self.leituras.append(
                    LeituraBanco(lid, eq_id, temp, float(umid), porta, dt, "SENSOR")
                )
                lid += 1

        # Alertas iniciais do db/seed.sql (equip 6 tem alerta RESOLVIDO, equip 4 nao tem alerta)
        self.alertas.extend(
            [
                AlertaBanco(1, 1, 5, "TEMP_ALTA", "CRITICA", "ABERTO", t_seed),
                AlertaBanco(2, 2, 10, "TEMP_BAIXA", "ALTA", "ABERTO", t_seed),
                AlertaBanco(3, 3, 15, "PORTA_ABERTA", "MEDIA", "ABERTO", t_seed),
                AlertaBanco(4, 5, 25, "UMIDADE", "ALTA", "ABERTO", t_seed),
                AlertaBanco(5, 6, 28, "TEMP_ALTA", "CRITICA", "RESOLVIDO", t_seed - timedelta(minutes=4)),
            ]
        )

    def ultima_leitura_em(self, equipamento_id: int) -> datetime | None:
        do_eq = [l.registrada_em for l in self.leituras if l.equipamento_id == equipamento_id]
        return max(do_eq) if do_eq else None

    def receber_mqtt(self, topico: str, payload_str: str, qos: int) -> list[AlertaBanco]:
        assert qos == 1, f"QoS esperado = 1, veio {qos}"

        # 1. LeituraMqttMensagem.TentarLer
        partes = [p for p in topico.split("/") if p]
        assert len(partes) == 4 and partes[0] == "frio" and partes[1] == "equipamentos" and partes[3] == "leitura", (
            f"topico fora do padrao: {topico}"
        )
        equipamento_id = int(partes[2])
        assert equipamento_id in sensor.EQUIPAMENTOS_SEED, f"equipamento inexistente: {equipamento_id}"

        dados = json.loads(payload_str)
        assert isinstance(dados, dict)
        assert "temperatura" in dados and "umidade" in dados
        if "equipamentoId" in dados:
            assert int(dados["equipamentoId"]) == equipamento_id

        temperatura = float(dados["temperatura"])
        umidade = float(dados["umidade"])
        assert -40.0 <= temperatura <= 80.0, f"temperatura fora da faixa fisica: {temperatura}"
        assert 0.0 <= umidade <= 100.0, f"umidade fora da faixa fisica: {umidade}"
        porta_aberta = bool(dados.get("portaAberta", False))
        registrada_em = datetime.fromisoformat(str(dados["registradaEm"])).astimezone()

        # 2. LeituraService.RegistrarAsync (tolerancia de relogio = +5 min)
        agora = self.relogio
        assert registrada_em <= agora + timedelta(minutes=5), (
            f"registradaEm estourou tolerancia de +5 min: {registrada_em} > {agora + timedelta(minutes=5)}"
        )

        nova = LeituraBanco(
            id=self._prox_leitura_id,
            equipamento_id=equipamento_id,
            temperatura=temperatura,
            umidade=umidade,
            porta_aberta=porta_aberta,
            registrada_em=registrada_em,
            origem="SENSOR",  # Ponte fixa OrigemLeitura.SENSOR
        )
        self._prox_leitura_id += 1
        self.leituras.append(nova)

        # 3. RegraDeAlerta.Avaliar
        anteriores = sorted(
            [l for l in self.leituras if l.equipamento_id == equipamento_id and l.id != nova.id],
            key=lambda l: (l.registrada_em, l.id),
            reverse=True,
        )[:50]
        recentes = [nova] + [
            l for l in anteriores if l.registrada_em <= nova.registrada_em
        ][:2]

        eq = sensor.EQUIPAMENTOS_SEED[equipamento_id]
        propostos: list[tuple[str, str]] = []
        if len(recentes) == 3:
            if all(l.temperatura > eq["temp_max"] for l in recentes):
                desvio = nova.temperatura - eq["temp_max"]
                sev = "CRITICA" if desvio >= 3.0 else ("ALTA" if desvio >= 1.5 else "MEDIA")
                propostos.append(("TEMP_ALTA", sev))
            elif all(l.temperatura < eq["temp_min"] for l in recentes):
                desvio = eq["temp_min"] - nova.temperatura
                sev = "CRITICA" if desvio >= 3.0 else ("ALTA" if desvio >= 1.5 else "MEDIA")
                propostos.append(("TEMP_BAIXA", sev))

        if nova.porta_aberta:
            inicio = nova.registrada_em
            for ant in sorted(anteriores, key=lambda l: l.registrada_em, reverse=True):
                if ant.registrada_em > nova.registrada_em:
                    continue
                if not ant.porta_aberta:
                    break
                inicio = ant.registrada_em
            duracao = nova.registrada_em - inicio
            if duracao >= timedelta(minutes=5):
                sev = "ALTA" if duracao >= timedelta(minutes=15) else "MEDIA"
                propostos.append(("PORTA_ABERTA", sev))

        criados: list[AlertaBanco] = []
        for tipo, sev in propostos:
            ja_aberto = any(
                a.equipamento_id == equipamento_id and a.tipo == tipo and a.status == "ABERTO"
                for a in self.alertas
            )
            if not ja_aberto:
                novo_alerta = AlertaBanco(
                    id=self._prox_alerta_id,
                    equipamento_id=equipamento_id,
                    leitura_id=nova.id,
                    tipo=tipo,
                    severidade=sev,
                    status="ABERTO",
                    criado_em=nova.registrada_em,
                )
                self._prox_alerta_id += 1
                self.alertas.append(novo_alerta)
                criados.append(novo_alerta)
        return criados


class TestesContratoSimulador(unittest.TestCase):
    def _rodar_cenario(
        self,
        backend: SimuladorBackendFiel,
        argv: list[str],
    ) -> list[list[AlertaBanco]]:
        historico_alertas_por_envio: list[list[AlertaBanco]] = []

        class FakeMqttClient:
            def loop_stop(self) -> None:
                pass

            def disconnect(self) -> None:
                pass

            def publish(self, topic: str, payload: str, qos: int = 0) -> object:
                criados = backend.receber_mqtt(topic, payload, qos)
                historico_alertas_por_envio.append(criados)
                info = types.SimpleNamespace(rc=0, wait_for_publish=lambda timeout=None: None)
                return info

        def avancar_relogio(rapido: bool, segundos_normais: float) -> None:
            passo = sensor.INTERVALO_RAPIDO if rapido else segundos_normais
            backend.relogio += timedelta(seconds=passo)

        with (
            mock.patch.object(sensor, "conectar", return_value=FakeMqttClient()),
            mock.patch.object(sensor, "agora_local", side_effect=lambda: backend.relogio),
            mock.patch.object(sensor, "_aguardar", side_effect=avancar_relogio),
            mock.patch.object(
                sensor,
                "_descobrir_ultima_leitura_silencioso",
                side_effect=lambda _url, eq: backend.ultima_leitura_em(eq),
            ),
        ):
            rc = sensor.main(argv)
            self.assertEqual(rc, 0)

        return historico_alertas_por_envio

    def test_quebra_compressor_dispara_exatamente_na_3a_e_nao_duplica_na_4a(self) -> None:
        # Mesmo com seed.sql rodado ha apenas 2 segundos!
        backend = SimuladorBackendFiel(datetime.now().astimezone() - timedelta(seconds=2))
        por_envio = self._rodar_cenario(backend, ["--cenario", "quebra-compressor", "--rapido"])

        self.assertEqual(len(por_envio), 4)
        self.assertEqual(len(por_envio[0]), 0)
        self.assertEqual(len(por_envio[1]), 0)
        self.assertEqual(len(por_envio[2]), 1)
        self.assertEqual(por_envio[2][0].tipo, "TEMP_ALTA")
        self.assertEqual(por_envio[2][0].severidade, "CRITICA")
        self.assertEqual(len(por_envio[3]), 0)  # Nao duplica!

    def test_pico_curto_duas_leituras_fora_nao_abrem_alerta(self) -> None:
        backend = SimuladorBackendFiel(datetime.now().astimezone() - timedelta(minutes=10))
        por_envio = self._rodar_cenario(backend, ["--cenario", "pico-curto", "--rapido"])

        self.assertEqual(len(por_envio), 3)
        self.assertTrue(all(len(criados) == 0 for criados in por_envio))

    def test_porta_aberta_seed_antigo_usa_passado_e_abre_na_4a_leitura(self) -> None:
        backend = SimuladorBackendFiel(datetime.now().astimezone() - timedelta(minutes=15))
        por_envio = self._rodar_cenario(backend, ["--cenario", "porta-aberta", "--rapido"])

        self.assertEqual(len(por_envio), 4)
        self.assertEqual(len(por_envio[0]), 0)
        self.assertEqual(len(por_envio[1]), 0)
        self.assertEqual(len(por_envio[2]), 0)
        self.assertEqual(len(por_envio[3]), 1)
        self.assertEqual(por_envio[3][0].tipo, "PORTA_ABERTA")
        self.assertEqual(por_envio[3][0].severidade, "MEDIA")

    def test_porta_aberta_logo_apos_seed_ou_porta_40s_tambem_abre_alerta(self) -> None:
        # Cenario critico: seed.sql rodou ha 3s, usuario roda porta-40s e em seguida porta-aberta!
        backend = SimuladorBackendFiel(datetime.now().astimezone() - timedelta(seconds=3))

        envio_40s = self._rodar_cenario(backend, ["--cenario", "porta-40s", "--rapido"])
        self.assertEqual(len(envio_40s), 2)
        self.assertTrue(all(len(c) == 0 for c in envio_40s))

        envio_aberta = self._rodar_cenario(backend, ["--cenario", "porta-aberta", "--rapido"])
        self.assertEqual(len(envio_aberta), 4)
        self.assertEqual(len(envio_aberta[0]), 0)
        self.assertEqual(len(envio_aberta[1]), 0)
        self.assertEqual(len(envio_aberta[2]), 0)
        self.assertEqual(len(envio_aberta[3]), 1)
        self.assertEqual(envio_aberta[3][0].tipo, "PORTA_ABERTA")


if __name__ == "__main__":
    unittest.main(verbosity=2)
