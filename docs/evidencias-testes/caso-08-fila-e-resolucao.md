# Caso 8 — Voluntario ve a fila de alertas e resolve no local

- **Verifica:** README, "Contrato para o app" (executado pela API; a lista do app nao esta neste repositorio)
- **Executado em:** 2026-11-11 18:33:12 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (7/7 passos)


## Passo 1 — Fila de abertos da Ana (instituicao 1)

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

**Resultado do passo 1:** esperado 200: alerta 1 CRITICA, depois alerta 2 ALTA, so da instituicao 1; obtido 200 [(1, 'CRITICA', 'Câmara fria de hortifrúti'), (2, 'ALTA', 'Freezer de proteínas')] → **PASSOU**


## Passo 2 — Resolver o alerta 2

```http
PATCH /api/alertas/2/resolver
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
  "equipamentoId": 2,
  "equipamento": "Freezer de proteínas",
  "leituraId": 10,
  "tipo": "TEMP_BAIXA",
  "severidade": "ALTA",
  "status": "RESOLVIDO",
  "criadoEm": "2026-11-11T21:32:31.88826+00:00",
  "resolvidoEm": "2026-11-11T21:33:12.9751197+00:00",
  "resolvidoPorId": 2
}
```

**Resultado do passo 2:** esperado 200 RESOLVIDO, resolvidoEm preenchido, resolvidoPorId 2; obtido 200 RESOLVIDO por 2 → **PASSOU**


## Passo 3 — Fila depois de resolver

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
  }
]
```

**Resultado do passo 3:** esperado 200 so com o alerta 1; obtido 200 ids=1 → **PASSOU**


## Passo 4 — Resolver de novo

```http
PATCH /api/alertas/2/resolver
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>
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
  "detail": "Alerta 2 ja foi resolvido.",
  "instance": "/api/alertas/2/resolver"
}
```

**Resultado do passo 4:** esperado 409 "Alerta 2 ja foi resolvido."; obtido 409 'Alerta 2 ja foi resolvido.' → **PASSOU**


## Passo 5 — Alerta de outra instituicao

```http
PATCH /api/alertas/3/resolver
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
  "detail": "Alerta 3 e de equipamento de outra instituicao.",
  "instance": "/api/alertas/3/resolver"
}
```
```sql
SELECT status FROM alerta WHERE id = 3;
-- resultado:
ABERTO
```

**Resultado do passo 5:** esperado 403 e o alerta 3 continua ABERTO; obtido 403 e ABERTO → **PASSOU**


## Passo 6 — Alerta inexistente

```http
PATCH /api/alertas/999/resolver
Authorization: Bearer <token de ana.rocha@caminhosdobem.example (VOLUNTARIO, instituicao 1)>
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
  "detail": "Alerta 999 nao encontrado.",
  "instance": "/api/alertas/999/resolver"
}
```

**Resultado do passo 6:** esperado 404; obtido 404 → **PASSOU**


## Passo 7 — Conferencia no banco

```sql
SELECT status, resolvido_em IS NOT NULL AS tem_data, resolvido_por_id FROM alerta WHERE id = 2;
-- resultado:
RESOLVIDO | t | 2
```

**Resultado do passo 7:** esperado RESOLVIDO | t | 2; obtido RESOLVIDO | t | 2 → **PASSOU**

