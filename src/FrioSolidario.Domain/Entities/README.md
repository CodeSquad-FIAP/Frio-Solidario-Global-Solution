# Entidades do dominio

Dono do modelo de dados: **André** (card "ER das 6 entidades" → "Script .sql").
As classes abaixo espelham o `db/schema.sql`, que é a fonte da verdade.

| Arquivo | Entidade | Tabela |
|---|---|---|
| `Usuario.cs` | `Usuario` | `usuario` |
| `Instituicao.cs` | `Instituicao` | `instituicao` |
| `Equipamento.cs` | `Equipamento` | `equipamento` |
| `Leitura.cs` | `Leitura` | `leitura` |
| `Alerta.cs` | `Alerta` | `alerta` |
| `Lote.cs` | `Lote` | `lote` |

Os vocabulários fechados (papel, tipo, origem, severidade, status) ficam em `../Enums.cs` e têm
conversão para texto no mapeamento, então o valor gravado é o mesmo do CHECK do banco.

Nada de atributo de banco (coluna, tabela, índice) aqui: mapeamento fica na Infrastructure
(`src/FrioSolidario.Infrastructure/Persistence/FrioSolidarioDbContext.cs`), junto com os nomes de
coluna e as precisões de `NUMERIC`.
