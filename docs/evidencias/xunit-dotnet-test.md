# Diferencial — casos 5 e 6 automatizados em xUnit (`dotnet test` verde)

**O que prova:** os dois casos que o card manda automatizar rodam sozinhos, contra a API de verdade
(`WebApplicationFactory` = `TestServer`, com os repositórios em memória), em ~1 s, sem Postgres e sem
Docker. É o diferencial de desempate do edital.

**Ambiente:** .NET SDK 10.0.401 (windows x64), projeto `tests/FrioSolidario.Tests`, repositório
`CodeSquad-FIAP/Frio-Solidario-Global-Solution`, commit `a09b974`.

```console
$ dotnet test --nologo
  FrioSolidario.Tests -> tests/FrioSolidario.Tests/bin/Debug/net10.0/FrioSolidario.Tests.dll
Execução de teste para ...\FrioSolidario.Tests.dll (.NETCoreApp,Version=v10.0)
1 arquivos de teste no total corresponderam ao padrão especificado.

Aprovado!  – Com falha:     0, Aprovado:    37, Ignorado:     0, Total:    37, Duração: 1 s - FrioSolidario.Tests.dll (net10.0)
```

| Arquivo | Testes | O que cobre |
|---|---|---|
| `RegraDeAlertaTests.cs` | 25 | a regra pura: 1 ou 2 leituras fora da faixa não abrem alerta, a 3ª abre, a 4ª não duplica; limites exatos (-18 e -25 não contam); severidade por desvio (1,5 e 3,0); a faixa vem do equipamento |
| `LeituraServiceTests.cs` | 4 | o fluxo do `POST /api/leituras`: a 3ª leitura abre 1 alerta e a resposta o traz; a 4ª não duplica; equipamento com alerta aberto não ganha outro; resolvido o alerta, nova sequência abre outro |
| `ApiTests.cs` | 8 | **caso 5** (3,1 °C não abre alerta) e **caso 6** (7,8 °C três vezes abre 1 `TEMP_ALTA` `CRITICA`) **por HTTP, contra a API**; mais 401 sem token, rota `/instituicoes/{id}/equipamentos` (200 própria / 403 outra), filtro `?ativo=`, DELETE lógico e o CRUD de lotes |

**Resultado:** 37 aprovados, 0 falhas, 0 ignorados → **PASSOU**

> Como o card pede print: a captura de tela desta execução (`dotnet test` verde) ainda não está em
> `prints/`; o comando acima é a saída real desta rodada, colada integralmente.
