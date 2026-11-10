# FRIO SOLIDARIO - GLOBAL SOLUTION FIAP
# Script de validacao do banco PostgreSQL

$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
$env:PGCLIENTENCODING = "UTF8"

$Banco = "frio_solidario_validacao"
$Usuario = "postgres"
$Pasta = $PSScriptRoot

$PastaEvidencias = Join-Path $Pasta "evidencias"
$ArquivoLog = Join-Path $PastaEvidencias "teste_postgresql.log"

# Verificar ferramentas instaladas
foreach ($Comando in @("psql", "createdb", "dropdb")) {
    if (-not (Get-Command $Comando -ErrorAction SilentlyContinue)) {
        throw "Comando nao encontrado: $Comando. Verifique a instalacao do PostgreSQL."
    }
}

# Criar pasta das evidencias
New-Item -ItemType Directory -Force -Path $PastaEvidencias | Out-Null

"FRIO SOLIDARIO - TESTE DO BANCO" | Set-Content -Encoding UTF8 $ArquivoLog
"Data: $(Get-Date)" | Add-Content $ArquivoLog

# Verificar se o banco de validacao ja existe
$Existe = & psql -U $Usuario -d postgres -X -At `
    -v ON_ERROR_STOP=1 `
    -c "SELECT 1 FROM pg_database WHERE datname = '$Banco';"

if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel conectar ao PostgreSQL."
}

if ($Existe -contains "1") {
    throw "O banco de validacao ja existe. Nenhum dado foi apagado."
}

# Criar banco temporario
& createdb -U $Usuario $Banco

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao criar o banco de validacao."
}

try {
    # Executar os arquivos na ordem correta
    foreach ($Arquivo in @(
    "schema.sql",
    "seed.sql",
    "verificar.sql",
    "consultas.sql"
    )) {
        Write-Host "Executando $Arquivo"

        "Executando $Arquivo" | Add-Content $ArquivoLog

        $Caminho = Join-Path $Pasta $Arquivo

        # Capturar saida do PostgreSQL
        $Saida = & psql -U $Usuario -d $Banco -X `
            -v ON_ERROR_STOP=1 -f $Caminho

        $Codigo = $LASTEXITCODE

        $Saida | Out-String | Add-Content $ArquivoLog
        $Saida | Out-Host

        if ($Codigo -ne 0) {
            throw "Erro ao executar $Arquivo. Consulte o log."
        }
    }

    "RESULTADO FINAL: SUCESSO" | Add-Content $ArquivoLog
    Write-Host "Testes concluidos com sucesso!" -ForegroundColor Green
}
finally {
    # Remover somente o banco temporario criado pelo teste
    & dropdb -U $Usuario $Banco

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Nao foi possivel remover o banco temporario."
    }
}

Write-Host "Log salvo em: $ArquivoLog"
