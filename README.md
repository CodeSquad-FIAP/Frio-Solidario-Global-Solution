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




## Banco de Dados — Frio Solidário

O projeto utiliza PostgreSQL 17 como sistema gerenciador
de banco de dados relacional.

### Modelo de dados

O banco possui 6 entidades:

- instituicao
- usuario
- equipamento
- leitura
- alerta
- lote

As tabelas possuem chaves primárias (PK), estrangeiras (FK),
restrições NOT NULL, UNIQUE e CHECK para garantir a
integridade dos dados.

### Arquivos SQL

- `db/schema.sql`: criação das tabelas, constraints e índices.
- `db/seed.sql`: carga inicial do banco de dados.
- `db/verificar.sql`: validação dos registros e constraints.
- `db/testar.ps1`: execução automatizada dos testes.

### Carga inicial

A carga inicial contém:

- 3 instituições
- 6 usuários
- 6 equipamentos
- 30 leituras de sensores
- 5 alertas
- 12 lotes de alimentos

Os dados são fictícios e simulam situações reais de
funcionamento, incluindo variações de temperatura,
umidade, abertura de porta e resolução de alertas.

### Índices obrigatórios

- `leitura(equipamento_id, registrada_em)`
- `alerta(status)`

### Como testar

Requisitos:
- PostgreSQL 17
- PowerShell
- Ferramentas PostgreSQL disponíveis no PATH

Na raiz do projeto, executar:

```powershell
powershell -ExecutionPolicy Bypass -File .\db\testar.ps1
```

O script cria um banco temporário, executa os arquivos SQL,
verifica os dados e gera o log de evidências.

### Evidências

O teste foi executado com sucesso em PostgreSQL 17.11.

Log de execução:
`db/evidencias/teste_postgresql.log`

Resultado: SUCESSO.

### Diagrama ER

O modelo entidade-relacionamento está previsto nos arquivos:

- `docs/der.md`
- `docs/der.png`





## Consultas SQL — Indicadores de Impacto

O arquivo `db/consultas.sql` contém cinco consultas
utilizando JOIN, funções de agregação e agrupamento.

### Consultas implementadas

1. **Quilos doados por instituição nos últimos 30 dias**
   - JOIN entre instituicao e lote.
   - Utiliza SUM e GROUP BY.
   - Apoia o acompanhamento do volume de doações.

2. **Equipamentos com mais alertas críticos**
   - JOIN entre equipamento e alerta.
   - Utiliza COUNT, GROUP BY e ORDER BY.
   - Apoia a priorização de manutenção.

3. **Tempo médio de resolução dos alertas**
   - JOIN entre instituicao, equipamento e alerta.
   - Utiliza AVG e cálculo de diferença entre datas.
   - Apoia a avaliação da eficiência das equipes.

4. **Percentual de leituras fora da faixa**
   - JOIN entre equipamento e leitura.
   - Utiliza CASE e agregação condicional.
   - Apoia a identificação de equipamentos com falhas.

5. **Taxa de perda por categoria de alimento**
   - Utiliza SUM, CASE, GROUP BY e HAVING.
   - Calcula a porcentagem de lotes perdidos.
   - Apoia decisões para reduzir o desperdício.

### Validação

As cinco consultas foram executadas no PostgreSQL 17.11,
utilizando a carga inicial do projeto.

Todas retornaram resultados.

As evidências estão registradas no arquivo:

`db/evidencias/teste_postgresql.log`
