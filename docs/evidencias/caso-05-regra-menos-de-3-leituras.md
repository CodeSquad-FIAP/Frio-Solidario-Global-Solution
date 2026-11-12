# Caso 5 — Regra de faixa: 1 ou 2 leituras fora da faixa nao abrem alerta

- **Verifica:** README, "Regra de alerta": alerta so com 3 leituras seguidas fora da faixa; limite exato nao conta (equipamento 4: -25 a -18)
- **Executado em:** 2026-11-11 18:33:12 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (7/7 passos)


## Passo 1 — Leitura de -16.0 C

```http
POST /api/leituras
Authorization: Bearer <token de bruna.oliveira@bairrosolidario.example (VOLUNTARIO, instituicao 2)>

{
  "equipamentoId": 4,
  "temperatura": -16.0,
  "umidade": 75.0,
  "portaAberta": false,
  "origem": "SIMULADO"
}
```
```http
HTTP 201
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "leitura": {
    "id": 31,
    "equipamentoId": 4,
    "temperatura": -16.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.1736383+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 1:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 2 — Leitura de -16.0 C

```http
POST /api/leituras
Authorization: Bearer <token de bruna.oliveira@bairrosolidario.example (VOLUNTARIO, instituicao 2)>

{
  "equipamentoId": 4,
  "temperatura": -16.0,
  "umidade": 75.0,
  "portaAberta": false,
  "origem": "SIMULADO"
}
```
```http
HTTP 201
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "leitura": {
    "id": 32,
    "equipamentoId": 4,
    "temperatura": -16.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.2205964+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 2:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 3 — Leitura de -20.0 C

```http
POST /api/leituras
Authorization: Bearer <token de bruna.oliveira@bairrosolidario.example (VOLUNTARIO, instituicao 2)>

{
  "equipamentoId": 4,
  "temperatura": -20.0,
  "umidade": 75.0,
  "portaAberta": false,
  "origem": "SIMULADO"
}
```
```http
HTTP 201
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "leitura": {
    "id": 33,
    "equipamentoId": 4,
    "temperatura": -20.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.2470562+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 3:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 4 — Leitura de -18.0 C

```http
POST /api/leituras
Authorization: Bearer <token de bruna.oliveira@bairrosolidario.example (VOLUNTARIO, instituicao 2)>

{
  "equipamentoId": 4,
  "temperatura": -18.0,
  "umidade": 75.0,
  "portaAberta": false,
  "origem": "SIMULADO"
}
```
```http
HTTP 201
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "leitura": {
    "id": 34,
    "equipamentoId": 4,
    "temperatura": -18.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.2589102+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 4:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 5 — Leitura de -18.0 C

```http
POST /api/leituras
Authorization: Bearer <token de bruna.oliveira@bairrosolidario.example (VOLUNTARIO, instituicao 2)>

{
  "equipamentoId": 4,
  "temperatura": -18.0,
  "umidade": 75.0,
  "portaAberta": false,
  "origem": "SIMULADO"
}
```
```http
HTTP 201
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "leitura": {
    "id": 35,
    "equipamentoId": 4,
    "temperatura": -18.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.2720613+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 5:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 6 — Leitura de -18.0 C

```http
POST /api/leituras
Authorization: Bearer <token de bruna.oliveira@bairrosolidario.example (VOLUNTARIO, instituicao 2)>

{
  "equipamentoId": 4,
  "temperatura": -18.0,
  "umidade": 75.0,
  "portaAberta": false,
  "origem": "SIMULADO"
}
```
```http
HTTP 201
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "leitura": {
    "id": 36,
    "equipamentoId": 4,
    "temperatura": -18.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.3103554+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 6:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 7 — Conferencia no banco

```sql
SELECT count(*) FROM alerta WHERE equipamento_id = 4;
-- resultado:
0
```

**Resultado do passo 7:** esperado 0; obtido 0 → **PASSOU**

