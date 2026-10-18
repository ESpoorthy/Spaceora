package com.spaceora.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Flyway migration chain applies cleanly against a real
 * PostgreSQL engine. This test does NOT start the Spring application context —
 * it uses a bare Flyway instance directly against a Testcontainers PostgreSQL
 * container, keeping it fast while still exercising the actual SQL scripts.
 *
 * <p>This addresses the review finding: "Integration tests bypass Flyway migrations".
 * The test is automatically skipped (via JUnit assumption) when Docker is not
 * reachable, so the build remains green in environments without a Docker daemon.
 * When Docker is present the full migration chain must apply without errors.
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
                // To run: start Docker Desktop, then: cd backend && bash mvnw test -Dtest=FlywayMigrationIntegrationTest
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
                .cleanDisabled(false)
                .load();
    }

    @Test
    void allMigrationsApplyCleanly_schemaMatchesV1Spec() throws Exception {
        buildFlyway().clean();
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

        assertThat(versions).contains("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

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

            Map<String, String> seededRates = new HashMap<>();
            ResultSet rates = conn.createStatement().executeQuery(
                    "SELECT name, hourly_rate FROM spaces ORDER BY name");
            while (rates.next()) {
                seededRates.put(rates.getString("name"), rates.getBigDecimal("hourly_rate").toPlainString());
            }
            assertThat(seededRates)
                    .containsEntry("Canary Wharf Focus Room", "1200.00")
                    .containsEntry("Shoreditch Hot Desk", "250.00");

            Map<String, String> seededImages = new HashMap<>();
            ResultSet images = conn.createStatement().executeQuery(
                    "SELECT s.name, si.image_url FROM spaces AS s JOIN space_images AS si ON si.space_id = s.id");
            while (images.next()) {
                seededImages.put(images.getString("name"), images.getString("image_url"));
            }
            assertThat(seededImages)
                    .containsEntry("Canary Wharf Focus Room", "/images/space-13.jpeg")
                    .containsEntry("Shoreditch Hot Desk", "/images/space-02.png");

            ResultSet demoCities = conn.createStatement().executeQuery(
                    "SELECT city, COUNT(*) AS space_count FROM spaces " +
                    "WHERE name IN ('HITEC City Collaboration Studio', 'Jubilee Hills Meeting Suite', " +
                    "'Indiranagar Daylight Workspace', 'Koramangala Team Room', " +
                    "'Baner Quiet Study Room', 'Viman Nagar Workshop Loft') " +
                    "GROUP BY city");
            Map<String, Integer> demoSpaceCounts = new HashMap<>();
            while (demoCities.next()) {
                demoSpaceCounts.put(demoCities.getString("city"), demoCities.getInt("space_count"));
            }
            assertThat(demoSpaceCounts)
                    .containsEntry("Hyderabad", 2)
                    .containsEntry("Bangalore", 2)
                    .containsEntry("Pune", 2);

            ResultSet demoAvailability = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM availability_schedules AS schedule " +
                    "JOIN spaces AS space ON space.id = schedule.space_id " +
                    "WHERE space.name IN ('HITEC City Collaboration Studio', 'Jubilee Hills Meeting Suite', " +
                    "'Indiranagar Daylight Workspace', 'Koramangala Team Room', " +
                    "'Baner Quiet Study Room', 'Viman Nagar Workshop Loft')");
            assertThat(demoAvailability.next()).isTrue();
            assertThat(demoAvailability.getInt(1)).isEqualTo(42);

            try (var migration = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/V10__seed_multi_city_demo_spaces.sql")) {
                assertThat(migration).isNotNull();
                String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
                try (Statement statement = conn.createStatement()) {
                    for (String command : sql.split(";")) {
                        if (!command.isBlank()) {
                            statement.execute(command);
                        }
                    }
                }
            }

            ResultSet rerunCount = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM spaces WHERE name IN " +
                    "('HITEC City Collaboration Studio', 'Jubilee Hills Meeting Suite', " +
                    "'Indiranagar Daylight Workspace', 'Koramangala Team Room', " +
                    "'Baner Quiet Study Room', 'Viman Nagar Workshop Loft')");
            assertThat(rerunCount.next()).isTrue();
            assertThat(rerunCount.getInt(1)).isEqualTo(6);
        }
    }

    @Test
    void usersTable_roleColumn_acceptsExpectedValues() throws Exception {
        buildFlyway().clean();
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

    @Test
    void hourlyRateMigration_updatesOnlyUnchangedSeedRecordsAfterV7() throws Exception {
        buildFlyway().clean();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("7")
                .load()
                .migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            conn.createStatement().executeUpdate(
                    "UPDATE spaces SET hourly_rate = 1555.55 WHERE name = 'Canary Wharf Focus Room'");
            conn.createStatement().executeUpdate(
                    "INSERT INTO spaces (owner_id, name, description, type, city, address, hourly_rate, capacity, is_active) " +
                    "SELECT id, 'Owner Custom Space', 'Owner-configured listing', 'COWORKING', 'Pune', " +
                    "'Custom address', 777.77, 6, TRUE FROM users WHERE email = 'owner@spaceora.dev'");
        }

        buildFlyway().migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            Map<String, String> rates = new HashMap<>();
            ResultSet rows = conn.createStatement().executeQuery(
                    "SELECT name, hourly_rate FROM spaces ORDER BY name");
            while (rows.next()) {
                rates.put(rows.getString("name"), rows.getBigDecimal("hourly_rate").toPlainString());
            }

            assertThat(rates)
                    .containsEntry("Canary Wharf Focus Room", "1555.55")
                    .containsEntry("Shoreditch Hot Desk", "250.00")
                    .containsEntry("Owner Custom Space", "777.77");

            Map<String, String> images = new HashMap<>();
            ResultSet imageRows = conn.createStatement().executeQuery(
                    "SELECT s.name, si.image_url FROM spaces AS s JOIN space_images AS si ON si.space_id = s.id");
            while (imageRows.next()) {
                images.put(imageRows.getString("name"), imageRows.getString("image_url"));
            }
            assertThat(images)
                    .doesNotContainKey("Canary Wharf Focus Room")
                    .containsEntry("Shoreditch Hot Desk", "/images/space-02.png")
                    .doesNotContainKey("Owner Custom Space");
        }
    }
}
