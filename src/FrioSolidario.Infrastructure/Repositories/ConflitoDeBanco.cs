using FrioSolidario.Application.Common;
using Microsoft.EntityFrameworkCore;
using Npgsql;

namespace FrioSolidario.Infrastructure.Repositories;

/// <summary>
/// Traduz violacao de constraint do Postgres em erro de negocio. Fica aqui, e nao na Api, para a
/// camada de cima nao depender do driver nem do codigo SQLSTATE.
/// </summary>
internal static class ConflitoDeBanco
{
    public static Exception Traduzir(Exception ex, string valorDuplicado, string vinculo, string regra)
    {
        // O EF envolve falha de comando em DbUpdateException, mas o provider tambem deixa passar
        // o PostgresException cru em alguns caminhos (delete em lote): olha a excecao e o encadeamento.
        var postgres = ex as PostgresException ?? ex.InnerException as PostgresException;

        return postgres?.SqlState switch
        {
            PostgresErrorCodes.UniqueViolation => new ConflitoException(valorDuplicado),
            PostgresErrorCodes.ForeignKeyViolation => new ConflitoException(vinculo),
            PostgresErrorCodes.CheckViolation => new RegraDeNegocioException(regra),
            _ => ex
        };
    }
}
