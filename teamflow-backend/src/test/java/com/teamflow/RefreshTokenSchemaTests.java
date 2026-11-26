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

/**
 * Vérifie que la <b>base de données elle-même</b> garantit les règles de la table
 * {@code refresh_token}, indépendamment du code Java : si une règle est cassée, c'est PostgreSQL
 * qui refuse la ligne.
 *
 * <p>Les tests tournent sur un vrai PostgreSQL 17 jetable (Testcontainers), avec les migrations
 * Flyway appliquées. On utilise du SQL direct ({@link JdbcTemplate}) pour tester le schéma tel quel,
 * sans passer par les entités.
 *
 * <p>{@code @Transactional} : chaque test est annulé (rollback) à la fin, les tests ne se
 * polluent donc pas entre eux. Après une erreur SQL, PostgreSQL abandonne la transaction en cours :
 * c'est pourquoi chaque test ne provoque <b>qu'une seule</b> violation, en dernière instruction.
 */

@SpringBootTest 
@Import(TestcontainersConfiguration.class)
@Transactional 
public class RefreshTokenSchemaTests {

    private static final String HASH_A = "a".repeat(64);

    private static final String INSERT_USER = """
        INSERT INTO app_user(email, password_hash, first_name, last_name)
        VALUES ( ?, ?, ?, ?)
        RETURNING id
        """;
    
    private static final String INSERT_TOKEN = """
        INSERT INTO refresh_token (user_id, token_hash, family_id, expires_at)
        VALUES (?, ?, gen_random_uuid(), now() + interval '7 days')
        """;

    @Autowired
    private JdbcTemplate jdbc;

    /** Crée un utilisateur (un token a besoin d'un propriétaire) et renvoie son identifiant. */
    private UUID insertUser(String email) {
        return jdbc.queryForObject(INSERT_USER, UUID.class, email, "x".repeat(60), "Alice", "Martin");
    }

    @Test
    void insertedTokenIsActiveByDefault() {
        UUID userId = insertUser("alice@example.com");
        jdbc.update(INSERT_TOKEN, userId, HASH_A);

        var row = jdbc.queryForMap(
                "SELECT revoked_at, created_at FROM refresh_token WHERE token_hash = ?", HASH_A);

        assertThat(row.get("revoked_at")).isNull();
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void rejectsMalformedHash() {
        UUID userId = insertUser("alice@example.com");

        // On vérifie le nom de la contrainte dans le message : le service s'en servira plus tard  pour traduire une erreur de base en erreur métier.
        assertThatThrownBy(() -> jdbc.update(INSERT_TOKEN, userId, "NOT-A-SHA256-HASH"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_refresh_token_hash_format");
    }

    @Test
    void rejectsDuplicateHash() {
        UUID userId = insertUser("alice@example.com");
        jdbc.update(INSERT_TOKEN, userId, HASH_A);

        assertThatThrownBy(() -> jdbc.update(INSERT_TOKEN, userId, HASH_A))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_refresh_token_hash");
    }

    @Test
    void rejectsExpiryNotAfterCreation() {
        UUID userId = insertUser("alice@example.com");

        // Dans une transaction, now() renvoie toujours le même instant : création = expiration.
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO refresh_token (user_id, token_hash, family_id, created_at, expires_at)
                VALUES (?, ?, gen_random_uuid(), now(), now())
                """, userId, HASH_A))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_refresh_token_expiry");
    }

    @Test
    void rejectsUnknownUser() {
        // Un token ne peut pas appartenir à un utilisateur qui n'existe pas (clé étrangère)
        assertThatThrownBy(() -> jdbc.update(INSERT_TOKEN, UUID.randomUUID(), HASH_A))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_refresh_token_user");
    }

    @Test
    void deletingUserDeletesItsTokens() {
        UUID userId = insertUser("alice@example.com");
        jdbc.update(INSERT_TOKEN, userId, HASH_A);

        jdbc.update("DELETE FROM app_user WHERE id = ?", userId);

        // ON DELETE CASCADE : les tokens de l'utilisateur supprimé ont disparu avec lui
        Integer remaining = jdbc.queryForObject("SELECT count(*) FROM refresh_token", Integer.class);
        assertThat(remaining).isZero();
    }
}
