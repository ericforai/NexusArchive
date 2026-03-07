package com.nexusarchive.modules.signature.infra;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class SignatureVerificationMigrationTest {

    @Test
    void migration_should_create_archive_linked_table_that_allows_multiple_records() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:sigver_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                "sa",
                "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE acc_archive (id VARCHAR(64) PRIMARY KEY)");
                statement.execute("INSERT INTO acc_archive(id) VALUES ('archive-1')");
            }

            try (InputStreamReader reader = new InputStreamReader(
                    new ClassPathResource("db/migration/V109__create_signature_verification_records.sql")
                            .getInputStream(),
                    StandardCharsets.UTF_8)) {
                RunScript.execute(connection, reader);
            }

            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO arc_signature_verification (
                        id,
                        archive_id,
                        file_id,
                        file_name,
                        document_type,
                        trigger_source,
                        verification_status,
                        signature_count,
                        valid_signature_count,
                        invalid_signature_count,
                        verified_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """)) {
                insert.setString(1, "sig-1");
                insert.setString(2, "archive-1");
                insert.setString(3, "file-1");
                insert.setString(4, "invoice-a.pdf");
                insert.setString(5, "PDF");
                insert.setString(6, "MANUAL");
                insert.setString(7, "PASSED");
                insert.setInt(8, 1);
                insert.setInt(9, 1);
                insert.setInt(10, 0);
                insert.executeUpdate();

                insert.setString(1, "sig-2");
                insert.setString(2, "archive-1");
                insert.setString(3, "file-2");
                insert.setString(4, "invoice-b.pdf");
                insert.setString(5, "PDF");
                insert.setString(6, "MANUAL");
                insert.setString(7, "FAILED");
                insert.setInt(8, 2);
                insert.setInt(9, 1);
                insert.setInt(10, 1);
                insert.executeUpdate();
            }

            assertEquals(1, tableCount(connection));
            assertEquals(2, recordCount(connection));

            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM acc_archive WHERE id = 'archive-1'");
            }

            assertEquals(0, recordCount(connection));
        }
    }

    private int tableCount(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_name = 'arc_signature_verification'
                """);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int recordCount(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM arc_signature_verification
                """);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
