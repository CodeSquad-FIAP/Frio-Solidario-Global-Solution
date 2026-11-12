# Caso 1 — Login com senha protegida por BCrypt

- **Verifica:** README, "Login (demo)": BCrypt, mesmo 401 para e-mail inexistente e senha errada, token com papel e instituicao
- **Executado em:** 2026-11-11 18:33:10 -0300
- **API:** `http://localhost:5312` · **Banco:** PostgreSQL 17 (seed recarregado)

- **Veredito do caso:** **PASSOU** (5/5 passos)


## Passo 1 — Hashes no banco

```sql
SELECT email, left(senha_hash,7) AS prefixo, length(senha_hash) AS tamanho FROM usuario ORDER BY id;
-- resultado:
mariana.costa@caminhosdobem.example | $2a$10$ | 60
ana.rocha@caminhosdobem.example | $2a$10$ | 60
rafael.nunes@bairrosolidario.example | $2a$10$ | 60
bruna.oliveira@bairrosolidario.example | $2a$10$ | 60
carla.mendes@saborepartilha.example | $2a$10$ | 60
pedro.lima@saborepartilha.example | $2a$10$ | 60
```

**Resultado do passo 1:** esperado 6 linhas com prefixo $2a$10$ e tamanho 60; obtido 6 linhas → **PASSOU**


## Passo 2 — Login com a senha certa (ADMIN)

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "mariana.costa@caminhosdobem.example",
  "senha": "Frio@2026"
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
  "token": "<JWT de 547 caracteres; payload {\"sub\": \"1\", \"email\": \"mariana.costa@caminhosdobem.example\", \"jti\": \"d86f3a12-3361-4151-8d7b-7159ebe6c5eb\", \"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\": \"Mariana Costa\", \"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\": \"ADMIN\", \"instituicao\": \"1\", \"nbf\": 1794432790, \"exp\": 1794461590, \"iss\": \"FrioSolidario\", \"aud\": \"FrioSolidarioApp\"}>",
  "expiraEm": "2026-11-12T05:33:10.7331369+00:00",
  "nome": "Mariana Costa",
  "papel": "ADMIN",
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem"
}
```

**Resultado do passo 2:** esperado 200 com papel ADMIN, instituicao 1; obtido 200 → **PASSOU**


## Passo 3 — Senha errada

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "mariana.costa@caminhosdobem.example",
  "senha": "errada123"
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

**Resultado do passo 3:** esperado 401 "E-mail ou senha invalidos."; obtido 401 'E-mail ou senha invalidos.' → **PASSOU**


## Passo 4 — E-mail inexistente

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "ninguem@x.example",
  "senha": "Frio@2026"
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

**Resultado do passo 4:** esperado 401 com a mesma mensagem do passo 3; obtido 401 'E-mail ou senha invalidos.' → **PASSOU**


## Passo 5 — E-mail em formato invalido

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "nao-e-email",
  "senha": "Frio@2026"
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
  "traceId": "00-a384341def6a12fdc12be1d39dfafbd3-550e531df5655725-00"
}
```

**Resultado do passo 5:** esperado 400 de validacao; obtido 400 → **PASSOU**

```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "ana.rocha@caminhosdobem.example",
  "senha": "Frio@2026"
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
  "token": "<JWT de 543 caracteres; payload {\"sub\": \"2\", \"email\": \"ana.rocha@caminhosdobem.example\", \"jti\": \"b82a6822-5ae4-4606-8ea7-b2448b7f4e6b\", \"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\": \"Ana Rocha\", \"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\": \"VOLUNTARIO\", \"instituicao\": \"1\", \"nbf\": 1794432791, \"exp\": 1794461591, \"iss\": \"FrioSolidario\", \"aud\": \"FrioSolidarioApp\"}>",
  "expiraEm": "2026-11-12T05:33:11.0164779+00:00",
  "nome": "Ana Rocha",
  "papel": "VOLUNTARIO",
  "instituicaoId": 1,
  "instituicao": "Banco de Alimentos Caminhos do Bem"
}
```
```http
POST /api/auth/login
Authorization: (nenhum)

{
  "email": "bruna.oliveira@bairrosolidario.example",
  "senha": "Frio@2026"
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
  "token": "<JWT de 559 caracteres; payload {\"sub\": \"4\", \"email\": \"bruna.oliveira@bairrosolidario.example\", \"jti\": \"e61ca6ff-aac0-4a49-915c-86973887401e\", \"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\": \"Bruna Oliveira\", \"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\": \"VOLUNTARIO\", \"instituicao\": \"2\", \"nbf\": 1794432791, \"exp\": 1794461591, \"iss\": \"FrioSolidario\", \"aud\": \"FrioSolidarioApp\"}>",
  "expiraEm": "2026-11-12T05:33:11.1227868+00:00",
  "nome": "Bruna Oliveira",
  "papel": "VOLUNTARIO",
  "instituicaoId": 2,
  "instituicao": "Mercado Bairro Solidário"
}
```
