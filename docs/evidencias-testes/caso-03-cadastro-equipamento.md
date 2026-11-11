# Caso 3 — Cadastro de equipamento com faixa de temperatura

- **Verifica:** README, "Endpoints" e EquipamentoRequest (executado pela API; a tela do app nao esta neste repositorio)
- **Executado em:** 2026-11-11 18:33:11 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (6/6 passos)


## Passo 1 — Cadastro valido

```http
POST /api/equipamentos
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

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
HTTP 201
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
  "tempMin": -22,
  "tempMax": -15,
  "umidMax": 80,
  "ativo": true
}
```

**Resultado do passo 1:** esperado 201, id 7, instituicao Caminhos do Bem, ativo; obtido 201 id=7 → **PASSOU**


## Passo 2 — Lista da instituicao 1

```http
GET /api/equipamentos?instituicaoId=1
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
    "tempMin": -22.0,
    "tempMax": -15.0,
    "umidMax": 80.0,
    "ativo": true
  }
]
```

**Resultado do passo 2:** esperado 200 com ids 1,2,7; obtido 200 ids=1,2,7 → **PASSOU**


## Passo 3 — Faixa invertida

```http
POST /api/equipamentos
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": 8,
  "tempMax": 2,
  "umidMax": 80
}
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
  "detail": "Temperatura minima (8) deve ser menor que a maxima (2).",
  "instance": "/api/equipamentos"
}
```

**Resultado do passo 3:** esperado 400 "Temperatura minima (8) deve ser menor que a maxima (2)."; obtido 400 'Temperatura minima (8) deve ser menor que a maxima (2).' → **PASSOU**


## Passo 4 — Umidade acima de 100

```http
POST /api/equipamentos
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "Freezer de teste",
  "tipo": "FREEZER",
  "tempMin": -22,
  "tempMax": -15,
  "umidMax": 101
}
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
  "type": "https://tools.ietf.org/html/rfc9110#section-15.5.1",
  "title": "One or more validation errors occurred.",
  "status": 400,
  "errors": {
    "UmidMax": [
      "Umidade maxima deve ficar entre 0,01 e 100."
    ]
  },
  "traceId": "00-bde495fd6618804ccfa6e9cc0547b25c-d5f3f92d1db52cd2-00"
}
```

**Resultado do passo 4:** esperado 400 de validacao; obtido 400 → **PASSOU**


## Passo 5 — Tipo fora do vocabulario

```http
POST /api/equipamentos
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "Freezer de teste",
  "tipo": "AQUARIO",
  "tempMin": -22,
  "tempMax": -15,
  "umidMax": 80
}
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
  "type": "https://tools.ietf.org/html/rfc9110#section-15.5.1",
  "title": "One or more validation errors occurred.",
  "status": 400,
  "errors": {
    "request": [
      "The request field is required."
    ],
    "$.tipo": [
      "The JSON value could not be converted to FrioSolidario.Application.DTOs.EquipamentoRequest. Path: $.tipo | LineNumber: 0 | BytePositionInLine: 66."
    ]
  },
  "traceId": "00-9ebad659b15afb1a7cf547e4027a5e7e-fe54db3fff61c26d-00"
}
```

**Resultado do passo 5:** esperado 400; obtido 400 → **PASSOU**


## Passo 6 — Conferencia no banco

```sql
SELECT nome, temp_min, temp_max FROM equipamento WHERE id = 7;
-- resultado:
Freezer de teste | -22.00 | -15.00
```

**Resultado do passo 6:** esperado Freezer de teste | -22.00 | -15.00; obtido Freezer de teste | -22.00 | -15.00 → **PASSOU**

