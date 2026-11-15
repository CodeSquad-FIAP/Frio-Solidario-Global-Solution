namespace FrioSolidario.Domain;

/// <summary>
/// Vocabularios fechados do modelo. Espelham, um a um, os CHECK do
/// <c>db/schema.sql</c>: se um valor novo entrar aqui, o script .sql muda no mesmo commit.
/// </summary>
public enum TipoInstituicao
{
    BANCO,
    MERCADO,
    PADARIA
}

public enum PapelUsuario
{
    ADMIN,
    VOLUNTARIO
}

public enum TipoEquipamento
{
    CAMARA,
    GELADEIRA,
    FREEZER
}

public enum OrigemLeitura
{
    SENSOR,
    SIMULADO
}

public enum TipoAlerta
{
    TEMP_ALTA,
    TEMP_BAIXA,
    PORTA_ABERTA,
    UMIDADE
}

/// <summary>Faixas de severidade ainda em aberto com a equipe (ver docs/der.md).</summary>
public enum SeveridadeAlerta
{
    BAIXA,
    MEDIA,
    ALTA,
    CRITICA
}

public enum StatusAlerta
{
    ABERTO,
    RESOLVIDO
}

/// <summary>
/// O que o voluntario fez ao resolver o alerta. Opcional: alertas resolvidos antes deste campo
/// existir (ou por um cliente que nao informa) ficam sem acao.
/// </summary>
public enum AcaoTomada
{
    AJUSTOU,
    TRANSFERIU_LOTE,
    DESCARTOU
}

public enum StatusLote
{
    DISPONIVEL,
    DISTRIBUIDO,
    PERDIDO
}
