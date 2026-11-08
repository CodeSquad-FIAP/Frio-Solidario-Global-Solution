# Entidades do dominio

Dono: **André** (card "ER das 6 entidades" → "Script .sql").

Quando o diagrama ER fechar, as classes de entidade entram **nesta pasta**, uma por arquivo:

| Arquivo | Entidade |
|---|---|
| `Usuario.cs` | usuario |
| `Instituicao.cs` | instituicao |
| `Equipamento.cs` | equipamento |
| `Leitura.cs` | leitura |
| `Alerta.cs` | alerta |
| `Lote.cs` | lote |

Nada de atributo de banco (coluna, tabela, índice) aqui: mapeamento fica na Infrastructure.
