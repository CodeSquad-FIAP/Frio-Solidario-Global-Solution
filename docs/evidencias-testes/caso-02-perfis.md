# Caso 2 — Perfis: ADMIN escreve, VOLUNTARIO nao

- **Verifica:** README, "Endpoints": escritas do CRUD exigem ADMIN (401 sem token, 403 para VOLUNTARIO)
- **Executado em:** 2026-11-11 18:33:11 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (7/7 passos)


## Passo 1 — POST sem token

```http
POST /api/equipamentos
Authorization: (nenhum)

{
  "instituicaoId": 1,
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": -22,
  "tempMax": -15,
  "umidMax": 80
}
```
```http
HTTP 401
Content-Type: application/json; charset=utf-8
WWW-Authenticate: Bearer
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Nao autenticado",
  "status": 401,
  "detail": "Token ausente ou invalido. Envie Authorization: Bearer <token>.",
  "instance": "/api/equipamentos"
}
```

**Resultado do passo 1:** esperado 401; obtido 401 → **PASSOU**


## Passo 2 — POST com VOLUNTARIO

```http
POST /api/equipamentos
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": -22,
  "tempMax": -15,
  "umidMax": 80
}
```
```http
HTTP 403
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Acesso negado",
  "status": 403,
  "detail": "O perfil do usuario nao permite esta operacao.",
  "instance": "/api/equipamentos"
}
```

**Resultado do passo 2:** esperado 403 "O perfil do usuario nao permite esta operacao."; obtido 403 'O perfil do usuario nao permite esta operacao.' → **PASSOU**


## Passo 3 — DELETE com VOLUNTARIO

```http
DELETE /api/equipamentos/2
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>
```
```http
HTTP 403
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Acesso negado",
  "status": 403,
  "detail": "O perfil do usuario nao permite esta operacao.",
  "instance": "/api/equipamentos/2"
}
```
```http
GET /api/equipamentos/2
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>
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
  "id": 2,
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem",
  "nome": "Freezer de proteínas",
  "tipo": "FREEZER",
  "tempMin": -25.0,
  "tempMax": -18.0,
  "umidMax": 90.0,
  "ativo": true
}
```

**Resultado do passo 3:** esperado 403 e depois GET 200; obtido 403 e GET 200 → **PASSOU**


## Passo 4 — PUT instituicao com VOLUNTARIO

```http
PUT /api/instituicoes/1
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>

{
  "nome": "x"
}
```
```http
HTTP 403
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Acesso negado",
  "status": 403,
  "detail": "O perfil do usuario nao permite esta operacao.",
  "instance": "/api/instituicoes/1"
}
```

**Resultado do passo 4:** esperado 403; obtido 403 → **PASSOU**


## Passo 5 — VOLUNTARIO le a fila de alertas

```http
GET /api/alertas?status=ABERTO
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>
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
    "equipamento": "Câmara fria de hortifrúti",
    "leituraId": 5,
    "tipo": "TEMP_ALTA",
    "severidade": "CRITICA",
    "status": "ABERTO",
    "criadoEm": "2026-11-11T21:32:31.88826+00:00",
    "resolvidoEm": null,
    "resolvidoPorId": null
  },
  {
    "id": 2,
    "equipamentoId": 2,
    "equipamento": "Freezer de proteínas",
    "leituraId": 10,
    "tipo": "TEMP_BAIXA",
    "severidade": "ALTA",
    "status": "ABERTO",
    "criadoEm": "2026-11-11T21:32:31.88826+00:00",
    "resolvidoEm": null,
    "resolvidoPorId": null
  }
]
```

**Resultado do passo 5:** esperado 200; obtido 200 → **PASSOU**


## Passo 6 — VOLUNTARIO consulta equipamentos

```http
GET /api/equipamentos
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>
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
  }
]
```

**Resultado do passo 6:** esperado 200; obtido 200 → **PASSOU**


## Passo 7 — ADMIN grava os mesmos dados do equipamento 2 (nao altera nada)

```http
GET /api/equipamentos/2
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
  "id": 2,
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem",
  "nome": "Freezer de proteínas",
  "tipo": "FREEZER",
  "tempMin": -25.0,
  "tempMax": -18.0,
  "umidMax": 90.0,
  "ativo": true
}
```
```http
PUT /api/equipamentos/2
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "Freezer de proteínas",
  "tipo": "FREEZER",
  "tempMin": -25.0,
  "tempMax": -18.0,
  "umidMax": 90.0
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
  "id": 2,
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem",
  "nome": "Freezer de proteínas",
  "tipo": "FREEZER",
  "tempMin": -25.0,
  "tempMax": -18.0,
  "umidMax": 90.0,
  "ativo": true
}
```

**Resultado do passo 7:** esperado 200; obtido 200 → **PASSOU**

