package com.fiap.mekano.supplies.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida que as migrations V1-V6 aplicam do zero em banco vazio.
 *
 * <p>Usa H2 em modo PostgreSQL para nao depender de Docker no CI. A validacao
 * definitiva do dialeto continua sendo feita no PostgreSQL 16 via compose.</p>
 */
@DisplayName("Migrations Flyway")
class MigrationSmokeTest {

    @Test
    @DisplayName("aplica V1-V6 do zero, cria as tabelas e semeia o estoque")
    void aplicaMigrationsDoZero() throws Exception {
        String url = "jdbc:h2:mem:migrations_smoke;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        var resultado = flyway.migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(6);
        assertThat(resultado.success).isTrue();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet tables = statement.executeQuery("""
                     select count(*)
                       from information_schema.tables
                      where table_name in (
                          'requisicoes_compra',
                          'itens_requisicao_compra',
                          'estoque',
                          'movimentos_estoque',
                          'nf_entradas'
                      )
                     """)) {
            assertThat(tables.next()).isTrue();
            assertThat(tables.getInt(1)).isEqualTo(5);
        }

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet seed = statement.executeQuery("""
                     select peca_uuid, saldo_atual, estoque_minimo, created_by
                       from estoque
                      order by peca_uuid
                     """)) {
            assertThat(seed.next()).isTrue();
            assertThat(seed.getString(1)).isEqualTo("55555555-5555-5555-5555-555555555555");
            assertThat(seed.getLong(2)).isEqualTo(10);
            assertThat(seed.getLong(3)).isEqualTo(2);
            assertThat(seed.getString(4)).isEqualTo("SEED");

            assertThat(seed.next()).isTrue();
            assertThat(seed.getString(1)).isEqualTo("66666666-6666-6666-6666-666666666666");
            assertThat(seed.getLong(2)).isEqualTo(5);
            assertThat(seed.getLong(3)).isEqualTo(3);
            assertThat(seed.getString(4)).isEqualTo("SEED");
        }
    }
}
