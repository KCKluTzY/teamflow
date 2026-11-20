package com.teamflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AppUserSchemaTests {

    private static final String INSERT_USER = """
            INSERT INTO app_user (email, password_hash, first_name, last_name)
            VALUES (?, ?, ?, ?)
            """;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayHasAppliedV1() {
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Integer.class);

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void insertedUserGetsGeneratedIdAndDefaults() {
        jdbc.update(INSERT_USER, "alice@example.com", "x".repeat(60), "Alice", "Martin");

        var row = jdbc.queryForMap(
                "SELECT id, enabled, created_at, updated_at FROM app_user WHERE email = ?",
                "alice@example.com");

        assertThat(row.get("id")).isInstanceOf(UUID.class);
        assertThat(row.get("enabled")).isEqualTo(true);
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void rejectsEmailContainingUppercase() {
        assertThatThrownBy(() ->
                jdbc.update(INSERT_USER, "Alice@Example.com", "x".repeat(60), "Alice", "Martin"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_app_user_email_lower");
    }

    @Test
    void rejectsDuplicateEmail() {
        jdbc.update(INSERT_USER, "bob@example.com", "x".repeat(60), "Bob", "Durand");

        assertThatThrownBy(() ->
                jdbc.update(INSERT_USER, "bob@example.com", "y".repeat(60), "Bobby", "Durand"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_app_user_email");
    }
}