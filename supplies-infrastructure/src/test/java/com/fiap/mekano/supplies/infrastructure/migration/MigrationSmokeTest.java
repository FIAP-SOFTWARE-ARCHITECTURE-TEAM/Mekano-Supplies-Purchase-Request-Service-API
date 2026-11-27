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
 * Valida que as migrations V1/V2 aplicam do zero em banco vazio.
 *
 * <p>Usa H2 em modo PostgreSQL para nao depender de Docker no CI. A validacao
 * definitiva do dialeto continua sendo feita no PostgreSQL 16 via compose.</p>
 */
@DisplayName("Migrations Flyway")
class MigrationSmokeTest {

    @Test
    @DisplayName("aplica V1 e V2 do zero e cria as tabelas esperadas")
    void aplicaMigrationsDoZero() throws Exception {
        String url = "jdbc:h2:mem:migrations_smoke;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        var resultado = flyway.migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(2);
        assertThat(resultado.success).isTrue();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet tables = statement.executeQuery("""
                     select count(*)
                       from information_schema.tables
                      where table_name in ('requisicoes_compra', 'itens_requisicao_compra')
                     """)) {
            assertThat(tables.next()).isTrue();
            assertThat(tables.getInt(1)).isEqualTo(2);
        }
    }
}
