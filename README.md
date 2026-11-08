# Frio Solidário — Global Solution 2026/2 (FIAP 3ESOA)

Monitoramento da cadeia de frio em bancos de alimentos e comércios doadores: sensores medem
temperatura, umidade e abertura de porta, a API compara com a faixa de cada equipamento e o app
avisa o voluntário antes de o lote ser perdido.

Projeto em construção — Global Solution 2026/2, entrega em 19/11/2026.

## Estrutura (camadas separadas)

```
src/
├── FrioSolidario.Domain/          # entidades do negócio (dono: André)
├── FrioSolidario.Application/     # serviços e regras de negócio + interfaces dos repositórios
├── FrioSolidario.Infrastructure/  # implementação dos repositórios (banco, MQTT)
└── FrioSolidario.Api/             # controllers, Swagger, injeção de dependência
```

O fluxo é sempre **Controller → Service → Repository**: o controller não conhece banco, o serviço não
conhece HTTP, e o repositório não devolve DTO de tela. As interfaces de repositório ficam em
`Application/Repositories` e as implementações em `Infrastructure/Repositories`.

## Como rodar (API)

Pré-requisitos: .NET SDK 10.

```bash
dotnet restore
dotnet run --project src/FrioSolidario.Api
```

- Swagger: http://localhost:5199/swagger
- Health check: http://localhost:5199/api/health

`GET /api/health` responde no formato:

```json
{"servico":"Frio Solidario API","status":"ok","versao":"1.0.0","ambiente":"Development",
 "verificadoEm":"2026-11-07T21:10:49-03:00","origemDosDados":"sem banco conectado (schema em construcao)"}
```

## Estado atual

| Parte | Situação |
|---|---|
| API em 3 camadas + Swagger | pronto (estrutura, injeção de dependência e health check) |
| Banco de dados | em construção — ER/schema com o André; `HealthRepository` ainda em memória |
| Login/JWT, CRUD, leituras, alertas | próximos cards (dependem do schema e do hash do Arthur) |
| App mobile | Kevin |
| IoT (MQTT) | Roger |

## Uso de IA

Ferramentas de IA foram usadas como apoio de pesquisa, revisão de texto e explicação de erros, com
revisão humana do conteúdo. A declaração completa, com as ferramentas e o que cada uma fez, entra
neste README até o dia 17/11.
