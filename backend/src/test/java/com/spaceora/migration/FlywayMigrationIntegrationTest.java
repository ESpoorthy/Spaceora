package com.spaceora.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the V1–V6 Flyway migration chain applies cleanly against a real
 * PostgreSQL engine. This test does NOT start the Spring application context —
 * it uses a bare Flyway instance directly against a Testcontainers PostgreSQL
 * container, keeping it fast while still exercising the actual SQL scripts.
 *
 * <p>This addresses the review finding: "Integration tests bypass Flyway migrations".
 * The test is automatically skipped (via JUnit assumption) when Docker is not
 * reachable, so the build remains green in environments without a Docker daemon.
 * When Docker is present the full V1–V6 chain must apply without errors.
 */
@SuppressWarnings({"resource", "SqlNoDataSourceInspection"})
class FlywayMigrationIntegrationTest {

    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startContainer() {
        // Skip gracefully when Docker is not reachable (e.g. no Docker Desktop running).
        boolean dockerAvailable;
        try {
            DockerClientFactory.instance().client();
            dockerAvailable = true;
        } catch (Exception e) {
            dockerAvailable = false;
        }
        Assumptions.assumeTrue(dockerAvailable,
                "Skipping Flyway migration test — Docker daemon is not reachable");

        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("spaceora_test")
                .withUsername("spaceora")
                .withPassword("spaceora_test_pw");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    private Flyway buildFlyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
    }

    @Test
    void allMigrationsApplyCleanly_schemaMatchesV1Spec() throws Exception {
        buildFlyway().migrate();

        MigrationInfo[] applied = buildFlyway().info().applied();
        assertThat(applied).isNotEmpty();

        List<String> versions = new ArrayList<>();
        for (MigrationInfo info : applied) {
            assertThat(info.getState().isApplied())
                    .as("Migration %s should be applied but state is %s",
                            info.getVersion(), info.getState())
                    .isTrue();
            if (info.getVersion() != null) {
                versions.add(info.getVersion().toString());
            }
        }

        // All six versioned migrations must be present.
        assertThat(versions).contains("1", "2", "3", "4", "5", "6");

        // Spot-check: users table must have all columns declared in V1.
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            ResultSet columns = conn.getMetaData().getColumns(null, null, "users", null);
            List<String> columnNames = new ArrayList<>();
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME").toLowerCase());
            }
            assertThat(columnNames).contains(
                    "id", "email", "password_hash", "first_name", "last_name",
                    "phone", "role", "created_at", "updated_at"
            );

            // Verify the spaces table exists (created by V2).
            ResultSet spaceTables = conn.getMetaData().getTables(null, null, "spaces", new String[]{"TABLE"});
            assertThat(spaceTables.next()).as("spaces table should exist after V2 migration").isTrue();

            // Verify the reservations table exists (created by V4).
            ResultSet resTables = conn.getMetaData().getTables(null, null, "reservations", new String[]{"TABLE"});
            assertThat(resTables.next()).as("reservations table should exist after V4 migration").isTrue();
        }
    }

    @Test
    void usersTable_roleColumn_acceptsExpectedValues() throws Exception {
        // Flyway is already applied from the previous test (idempotent — no harm calling again).
        buildFlyway().migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            conn.createStatement().execute(
                    "INSERT INTO users (email, password_hash, first_name, last_name, role) " +
                    "VALUES ('migtest@example.com', 'hash', 'Test', 'User', 'CUSTOMER') " +
                    "ON CONFLICT (email) DO NOTHING"
            );

            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT role FROM users WHERE email = 'migtest@example.com'"
            );
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("role")).isEqualTo("CUSTOMER");
        }
    }
}
