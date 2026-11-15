using FrioSolidario.Domain;
using FrioSolidario.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace FrioSolidario.Infrastructure.Persistence;

/// <summary>
/// Acesso ao banco desenhado pelo André em <c>db/schema.sql</c>. O mapeamento fica aqui
/// (a Domain nao conhece tabela, coluna nem tipo do Postgres) e o schema .sql continua sendo
/// a fonte da verdade: este contexto le e grava, nao cria nem altera tabela.
/// Os relacionamentos e as regras de integridade (FK composta do alerta, CHECKs) vivem no .sql.
/// </summary>
public sealed class FrioSolidarioDbContext(DbContextOptions<FrioSolidarioDbContext> options)
    : DbContext(options)
{
    public DbSet<Instituicao> Instituicoes => Set<Instituicao>();

    public DbSet<Usuario> Usuarios => Set<Usuario>();

    public DbSet<Equipamento> Equipamentos => Set<Equipamento>();

    public DbSet<Leitura> Leituras => Set<Leitura>();

    public DbSet<Alerta> Alertas => Set<Alerta>();

    public DbSet<Lote> Lotes => Set<Lote>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Instituicao>(map =>
        {
            map.ToTable("instituicao");
            map.HasKey(i => i.Id).HasName("pk_instituicao");
            map.Property(i => i.Id).HasColumnName("id");
            map.Property(i => i.Nome).HasColumnName("nome").HasMaxLength(150).IsRequired();
            map.Property(i => i.Cnpj).HasColumnName("cnpj").HasColumnType("char(14)").IsRequired();
            map.Property(i => i.Tipo).HasColumnName("tipo").HasMaxLength(10).HasConversion<string>().IsRequired();
            map.Property(i => i.Cidade).HasColumnName("cidade").HasMaxLength(120).IsRequired();
            map.Property(i => i.Endereco).HasColumnName("endereco").HasMaxLength(255).IsRequired();
        });

        modelBuilder.Entity<Usuario>(map =>
        {
            map.ToTable("usuario");
            map.HasKey(u => u.Id).HasName("pk_usuario");
            map.Property(u => u.Id).HasColumnName("id");
            map.Property(u => u.Nome).HasColumnName("nome").HasMaxLength(150).IsRequired();
            map.Property(u => u.Email).HasColumnName("email").HasMaxLength(254).IsRequired();
            map.Property(u => u.SenhaHash).HasColumnName("senha_hash").HasMaxLength(100).IsRequired();
            map.Property(u => u.Papel).HasColumnName("papel").HasMaxLength(12).HasConversion<string>().IsRequired();
            map.Property(u => u.InstituicaoId).HasColumnName("instituicao_id").IsRequired();
        });

        modelBuilder.Entity<Equipamento>(map =>
        {
            map.ToTable("equipamento");
            map.HasKey(e => e.Id).HasName("pk_equipamento");
            map.Property(e => e.Id).HasColumnName("id");
            map.Property(e => e.InstituicaoId).HasColumnName("instituicao_id").IsRequired();
            map.Property(e => e.Nome).HasColumnName("nome").HasMaxLength(150).IsRequired();
            map.Property(e => e.Tipo).HasColumnName("tipo").HasMaxLength(12).HasConversion<string>().IsRequired();
            map.Property(e => e.TempMin).HasColumnName("temp_min").HasPrecision(5, 2).IsRequired();
            map.Property(e => e.TempMax).HasColumnName("temp_max").HasPrecision(5, 2).IsRequired();
            map.Property(e => e.UmidMax).HasColumnName("umid_max").HasPrecision(5, 2).IsRequired();
            map.Property(e => e.Ativo).HasColumnName("ativo").IsRequired();
        });

        modelBuilder.Entity<Leitura>(map =>
        {
            map.ToTable("leitura");
            map.HasKey(l => l.Id).HasName("pk_leitura");
            map.Property(l => l.Id).HasColumnName("id");
            map.Property(l => l.EquipamentoId).HasColumnName("equipamento_id").IsRequired();
            map.Property(l => l.Temperatura).HasColumnName("temperatura").HasPrecision(5, 2).IsRequired();
            map.Property(l => l.Umidade).HasColumnName("umidade").HasPrecision(5, 2).IsRequired();
            map.Property(l => l.PortaAberta).HasColumnName("porta_aberta").IsRequired();
            map.Property(l => l.RegistradaEm).HasColumnName("registrada_em").IsRequired();
            map.Property(l => l.Origem).HasColumnName("origem").HasMaxLength(10).HasConversion<string>().IsRequired();
        });

        modelBuilder.Entity<Alerta>(map =>
        {
            map.ToTable("alerta");
            map.HasKey(a => a.Id).HasName("pk_alerta");
            map.Property(a => a.Id).HasColumnName("id");
            map.Property(a => a.EquipamentoId).HasColumnName("equipamento_id").IsRequired();
            map.Property(a => a.LeituraId).HasColumnName("leitura_id").IsRequired();
            map.Property(a => a.Tipo).HasColumnName("tipo").HasMaxLength(14).HasConversion<string>().IsRequired();
            map.Property(a => a.Severidade).HasColumnName("severidade").HasMaxLength(10).HasConversion<string>().IsRequired();
            map.Property(a => a.Status).HasColumnName("status").HasMaxLength(10).HasConversion<string>().IsRequired();
            map.Property(a => a.CriadoEm).HasColumnName("criado_em").IsRequired();
            map.Property(a => a.ResolvidoEm).HasColumnName("resolvido_em");
            map.Property(a => a.ResolvidoPorId).HasColumnName("resolvido_por_id");
            map.Property(a => a.AcaoTomada).HasColumnName("acao_tomada").HasMaxLength(16).HasConversion<string>();
        });

        modelBuilder.Entity<Lote>(map =>
        {
            map.ToTable("lote");
            map.HasKey(l => l.Id).HasName("pk_lote");
            map.Property(l => l.Id).HasColumnName("id");
            map.Property(l => l.InstituicaoId).HasColumnName("instituicao_id").IsRequired();
            map.Property(l => l.Descricao).HasColumnName("descricao").HasMaxLength(200).IsRequired();
            map.Property(l => l.Categoria).HasColumnName("categoria").HasMaxLength(80).IsRequired();
            map.Property(l => l.Kg).HasColumnName("kg").HasPrecision(10, 2).IsRequired();
            map.Property(l => l.Validade).HasColumnName("validade").IsRequired();
            map.Property(l => l.Status).HasColumnName("status").HasMaxLength(12).HasConversion<string>().IsRequired();
            map.Property(l => l.RetiradoEm).HasColumnName("retirado_em");
        });
    }
}
