# Caso 4 — Manter o equipamento: editar, remover e consultar a serie

- **Verifica:** README, "Endpoints": PUT, DELETE (409 com leituras) e serie de leituras
- **Executado em:** 2026-11-11 18:33:11 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (7/7 passos)


## Passo 1 — Editar a faixa

```http
PUT /api/equipamentos/7
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": -25,
  "tempMax": -18,
  "umidMax": 80
}
```
```http
HTTP 200
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "id": 7,
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem",
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": -25,
  "tempMax": -18,
  "umidMax": 80,
  "ativo": true
}
```

**Resultado do passo 1:** esperado 200 com -25 e -18; obtido 200 → **PASSOU**


## Passo 2 — Consultar o equipamento editado

```http
GET /api/equipamentos/7
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 200
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "id": 7,
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem",
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": -25.0,
  "tempMax": -18.0,
  "umidMax": 80.0,
  "ativo": true
}
```

**Resultado do passo 2:** esperado 200 com a faixa nova; obtido 200 → **PASSOU**


## Passo 3 — Serie de 24 h do equipamento 1

```http
GET /api/equipamentos/1/leituras?periodo=24h
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 200
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

[
  {
    "id": 1,
    "equipamentoId": 1,
    "temperatura": 3.1,
    "umidade": 71.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:24:31.88826+00:00",
    "origem": "SENSOR"
  },
  {
    "id": 2,
    "equipamentoId": 1,
    "temperatura": 3.3,
    "umidade": 72.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:26:31.88826+00:00",
    "origem": "SENSOR"
  },
  {
    "id": 3,
    "equipamentoId": 1,
    "temperatura": 7.0,
    "umidade": 73.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:28:31.88826+00:00",
    "origem": "SENSOR"
  },
  {
    "id": 4,
    "equipamentoId": 1,
    "temperatura": 7.3,
    "umidade": 74.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:30:31.88826+00:00",
    "origem": "SENSOR"
  },
  {
    "id": 5,
    "equipamentoId": 1,
    "temperatura": 7.6,
    "umidade": 75.0,
    "portaAberta": false,
    "registradaEm": "2026-11-11T21:32:31.88826+00:00",
    "origem": "SENSOR"
  }
]
```

**Resultado do passo 3:** esperado 200, 5 leituras em ordem cronologica; obtido 200, 5 leituras, ordenadas=True → **PASSOU**


## Passo 4 — Periodo invalido

```http
GET /api/equipamentos/1/leituras?periodo=1h
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 400
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Regra de negocio violada",
  "status": 400,
  "detail": "periodo aceita apenas 24h ou 7d.",
  "instance": "/api/equipamentos/1/leituras"
}
```

**Resultado do passo 4:** esperado 400 "periodo aceita apenas 24h ou 7d."; obtido 400 'periodo aceita apenas 24h ou 7d.' → **PASSOU**


## Passo 5 — Remover equipamento com leituras

```http
DELETE /api/equipamentos/1
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 409
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Conflito com os dados atuais",
  "status": 409,
  "detail": "Equipamento possui leituras ou alertas vinculados e nao pode ser removido.",
  "instance": "/api/equipamentos/1"
}
```
```http
GET /api/equipamentos
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 200
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

[
  {
    "id": 1,
    "instituicaoId": 1,
    "instituicao": "Banco de Alimentos Caminhos do Bem",
    "nome": "Câmara fria de hortifrúti",
    "tipo": "CAMARA",
    "tempMin": 0.0,
    "tempMax": 4.0,
    "umidMax": 85.0,
    "ativo": true
  },
  {
    "id": 2,
    "instituicaoId": 1,
    "instituicao": "Banco de Alimentos Caminhos do Bem",
    "nome": "Freezer de proteínas",
    "tipo": "FREEZER",
    "tempMin": -25.0,
    "tempMax": -18.0,
    "umidMax": 90.0,
    "ativo": true
  },
  {
    "id": 7,
    "instituicaoId": 1,
    "instituicao": "Banco de Alimentos Caminhos do Bem",
    "nome": "Freezer de teste",
    "tipo": "FREEZER",
    "tempMin": -25.0,
    "tempMax": -18.0,
    "umidMax": 80.0,
    "ativo": true
  }
]
```

**Resultado do passo 5:** esperado 409 e ids 1,2,7; obtido 409 e ids=1,2,7 → **PASSOU**


## Passo 6 — Remover equipamento sem leituras

```http
DELETE /api/equipamentos/7
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 204
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

(sem corpo)
```

**Resultado do passo 6:** esperado 204; obtido 204 → **PASSOU**


## Passo 7 — Consultar o removido

```http
GET /api/equipamentos/7
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
```
```http
HTTP 404
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Recurso nao encontrado",
  "status": 404,
  "detail": "Equipamento 7 nao encontrado.",
  "instance": "/api/equipamentos/7"
}
```

**Resultado do passo 7:** esperado 404; obtido 404 → **PASSOU**

