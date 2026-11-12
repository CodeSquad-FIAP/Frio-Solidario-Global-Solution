# Caso 6 — Regra de faixa: 3 leituras seguidas abrem 1 alerta, sem duplicar

- **Verifica:** README, "Regra de alerta": TEMP_ALTA com 3 leituras, severidade por desvio, um alerta aberto por tipo
- **Executado em:** 2026-11-11 18:33:12 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (6/6 passos)


## Passo 1 — Leitura -16.0 C (#1 da sequencia)

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
    "id": 37,
    "equipamentoId": 4,
    "temperatura": -16.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.5127272+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 1:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 2 — Leitura -16.0 C (#2 da sequencia)

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
    "id": 38,
    "equipamentoId": 4,
    "temperatura": -16.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.5417365+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 2:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 3 — Leitura -16.0 C (#3, fecha a sequencia)

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
    "id": 39,
    "equipamentoId": 4,
    "temperatura": -16.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.5722899+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": [
    {
      "id": 6,
      "equipamentoId": 4,
      "equipamento": "Freezer de congelados",
      "leituraId": 39,
      "tipo": "TEMP_ALTA",
      "severidade": "ALTA",
      "status": "ABERTO",
      "criadoEm": "2026-11-11T21:33:12.5722899+00:00",
      "resolvidoEm": null,
      "resolvidoPorId": null
    }
  ]
}
```

**Resultado do passo 3:** esperado 201 e 1 alerta TEMP_ALTA/ALTA/ABERTO (desvio 2,0 C); obtido 201, 1 alerta(s): TEMP_ALTA/ALTA/ABERTO → **PASSOU**


## Passo 4 — Leitura -16.0 C (#4, alerta ja aberto)

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
    "id": 40,
    "equipamentoId": 4,
    "temperatura": -16.0,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.6513297+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 4:** esperado 201 e alertasAbertos vazio (sem duplicar); obtido 201, 0 alerta(s) → **PASSOU**


## Passo 5 — Equipamento 1 (ja tem o alerta 1 aberto) recebe 7.8 C

```http
POST /api/leituras
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>

{
  "equipamentoId": 1,
  "temperatura": 7.8,
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
    "id": 41,
    "equipamentoId": 1,
    "temperatura": 7.8,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:33:12.700126+00:00",
    "origem": "SIMULADO"
  },
  "alertasAbertos": []
}
```

**Resultado do passo 5:** esperado 201 e alertasAbertos vazio; obtido 201, 0 alerta(s) → **PASSOU**


## Passo 6 — Conferencia no banco

```sql
SELECT tipo, severidade, status FROM alerta WHERE equipamento_id = 4;
-- resultado:
TEMP_ALTA | ALTA | ABERTO
```

**Resultado do passo 6:** esperado 1 linha: TEMP_ALTA | ALTA | ABERTO; obtido TEMP_ALTA | ALTA | ABERTO → **PASSOU**

