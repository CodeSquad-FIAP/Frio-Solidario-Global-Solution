# Caso 7 — Seguranca: token, SQL Injection, XSS e isolamento entre instituicoes

- **Verifica:** README, "Seguranca": JWT em toda rota, consultas parametrizadas, entrada sem marcacao, instituicao do token
- **Executado em:** 2026-11-11 18:33:13 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (13/13 passos)


## Passo 1 — Sem token

```http
GET /api/equipamentos
Authorization: (nenhum)
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

**Resultado do passo 1:** esperado 401 com WWW-Authenticate: Bearer; obtido 401 Bearer → **PASSOU**


## Passo 2 — Token adulterado (ultimo caractere da assinatura trocado)

```http
GET /api/alertas
Authorization: Bearer <token de mariana com a assinatura adulterada>
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
  "instance": "/api/alertas"
}
```

**Resultado do passo 2:** esperado 401; obtido 401 → **PASSOU**


## Passo 3 — SQL Injection no e-mail

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "' OR 1=1 --",
  "senha": "qualquer1"
}
```
```http
HTTP 400
Content-Type: application/problem+json; charset=utf-8
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
    "Email": [
      "The Email field is not a valid e-mail address."
    ]
  },
  "traceId": "00-6cf511e1bb9b8597974cde06af118fc4-7664f28b82875999-00"
}
```

**Resultado do passo 3:** esperado 400; obtido 400 → **PASSOU**


## Passo 4 — SQL Injection na senha

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "mariana.costa@caminhosdobem.example",
  "senha": "' OR '1'='1"
}
```
```http
HTTP 401
Content-Type: application/json; charset=utf-8
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store

{
  "title": "Credenciais invalidas",
  "status": 401,
  "detail": "E-mail ou senha invalidos.",
  "instance": "/api/auth/login"
}
```

**Resultado do passo 4:** esperado 401 sem token; obtido 401 token=nao → **PASSOU**


## Passo 5 — SQL Injection no filtro

```http
GET /api/equipamentos?instituicaoId=1%20OR%201=1
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
  "type": "https://tools.ietf.org/html/rfc9110#section-15.5.1",
  "title": "One or more validation errors occurred.",
  "status": 400,
  "errors": {
    "instituicaoId": [
      "The value '1 OR 1=1' is not valid."
    ]
  },
  "traceId": "00-1470baad5313f2262062341bece3470a-b36c26c93f996b91-00"
}
```

**Resultado do passo 5:** esperado 400; obtido 400 → **PASSOU**


## Passo 6 — XSS no nome do equipamento

```sql
SELECT count(*) FROM equipamento;
-- resultado:
6
```
```http
POST /api/equipamentos
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>

{
  "instituicaoId": 1,
  "nome": "<script>alert(1)</script>",
  "tipo": "FREEZER",
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
    "Nome": [
      "Nome nao pode conter os caracteres < ou > nem caracteres de controle."
    ]
  },
  "traceId": "00-3a8623892d577835c4298d850d579c23-3eaf9d6c1b1ecb86-00"
}
```
```sql
SELECT count(*) FROM equipamento;
-- resultado:
6
```

**Resultado do passo 6:** esperado 400 e nada gravado; obtido 400, linhas 6 -> 6 → **PASSOU**


## Passo 7 — Equipamento de outra instituicao

```http
GET /api/equipamentos/3
Authorization: Bearer <token de mariana.costa@caminhosdobem.example (ADMIN, instituicao 1)>
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
  "detail": "Equipamento 3 pertence a outra instituicao.",
  "instance": "/api/equipamentos/3"
}
```

**Resultado do passo 7:** esperado 403; obtido 403 'Equipamento 3 pertence a outra instituicao.' → **PASSOU**


## Passo 8 — Lista so da propria instituicao

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
  }
]
```

**Resultado do passo 8:** esperado 200 com ids 1,2; obtido 200 ids=1,2 → **PASSOU**


## Passo 9 — Cabecalhos de seguranca

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
  }
]
```

**Resultado do passo 9:** esperado nosniff, CSP default-src 'none', no-store e sem Server; obtido 200 → **PASSOU**


## Passo 10 — Limite de tentativas de login (aguarda 61 s para zerar a janela de 1 minuto)

_As 11 chamadas abaixo usam corpo vazio; so o status de cada uma e relevante._

Status das 11 chamadas: `[400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 429]`; cabecalho `Retry-After` da ultima: `60`; corpo da ultima: `{"title": "Tentativas demais", "status": 429, "detail": "Limite de tentativas de login atingido. Espere um minuto e tente de novo.", "instance": "/api/auth/login"}`


**Resultado do passo 10:** esperado 10 primeiras 400 e a 11a 429 com Retry-After 60; obtido [400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 429] → **PASSOU**


## Extras (fora dos 8 casos do plano)

_Verificacoes adicionais do card de seguranca. O token forjado usa a chave de desenvolvimento do `appsettings.json`; o primeiro, com validade futura, e o controle que prova que a unica diferenca do segundo e a expiracao._


## Passo 11 — Controle: token assinado com a chave certa e validade futura

```http
GET /api/alertas
Authorization: Bearer <JWT forjado com a chave de desenvolvimento, valido por 1 h>
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
    "status": "RESOLVIDO",
    "criadoEm": "2026-11-11T21:32:31.88826+00:00",
    "resolvidoEm": "2026-11-11T21:33:12.975119+00:00",
    "resolvidoPorId": 2
  }
]
```

**Resultado do passo 11:** esperado 200; obtido 200 → **PASSOU**


## Passo 12 — Token expirado

```http
GET /api/alertas
Authorization: Bearer <JWT forjado com a chave de desenvolvimento, expirado ha 1 h>
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
  "detail": "Token expirado. Faca login novamente.",
  "instance": "/api/alertas"
}
```

**Resultado do passo 12:** esperado 401 "Token expirado. Faca login novamente."; obtido 401 'Token expirado. Faca login novamente.' → **PASSOU**

**Reexecução em 11/11/2026, 23:58 (API + PostgreSQL 17.11 no ar):** o mesmo token real do login foi
reassinado com a chave de desenvolvimento e `exp` 3 h no passado (20:58:59Z, com o relógio em
23:58:59Z). Resposta idêntica: `401`, `WWW-Authenticate: Bearer`,
`{"title":"Nao autenticado","status":401,"detail":"Token expirado. Faca login novamente.","instance":"/api/alertas"}`.
Controle na mesma rodada: o token do login, intacto, devolveu `200` com os 2 alertas abertos da
instituição 1.


## Passo 13 — Token com alg none

```http
GET /api/alertas
Authorization: Bearer <JWT sem assinatura (alg none)>
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
  "instance": "/api/alertas"
}
```

**Resultado do passo 13:** esperado 401; obtido 401 → **PASSOU**

