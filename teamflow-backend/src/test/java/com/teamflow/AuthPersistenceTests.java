package com.teamflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.teamflow.auth.entity.RefreshToken;
import com.teamflow.auth.repository.RefreshTokenRepository;
import com.teamflow.user.entity.User;
import com.teamflow.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Vérifie que les <b>entités Java</b> ({@link User}, {@link RefreshToken}) et leurs dépôts
 * fonctionnent avec le schéma réel : enregistrement, relecture, relation entre les deux.
 *
 * <p>Complète {@link RefreshTokenSchemaTests}, qui teste le schéma en SQL pur. Ici, on teste
 * le chemin que le code de l'application utilisera vraiment : objets Java, puis Hibernate, puis
 * PostgreSQL. Si une entité ne correspond pas au schéma, l'application ne démarre même pas
 * ({@code ddl-auto: validate}) et tous ces tests échouent.
 */

@SpringBootTest 
@Import(TestcontainersConfiguration.class)
@Transactional 
public class AuthPersistenceTests {

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository tokens;

    @Autowired 
    private EntityManager entityManager;

    @Test
    void savesUserWithNormalizedEmailAndTimestamps() {
        // saveAndFlush force l'écriture immédiate en base (sinon Hibernate attendrait la fin du test)
        User saved = users.saveAndFlush(
            new User("  Marie@Example.COM", "x".repeat(60), "Marie", "Ducarré"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("marie@example.com");// espaces et majuscules retirés
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());// pas encore modifié
    }

    @Test
    void findUserByEmailAndCheckExistence(){
        users.saveAndFlush(new User("bob@example.com", "x".repeat(60),"Bob", "Dumesnil"));

        assertThat(users.findByEmail("bob@example.com")).isPresent();
        assertThat(users.existsByEmail("bob@example.com")).isTrue();
        assertThat(users.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    void savesRefreshToeknLinkedToItsUser(){
        User user = users.saveAndFlush(new User("carol@example.com", "x".repeat(60), "Carol", "Petit"));
        Instant now = Instant.now();
        UUID family = UUID.randomUUID();
        tokens.saveAndFlush(new RefreshToken(user, "c".repeat(64), family, now, now.plus(Duration.ofDays(7))));

        entityManager.clear();

        RefreshToken found = tokens.findByTokenHash("c".repeat(64)).orElseThrow();

        assertThat(found.getFamilyId()).isEqualTo(family);
        assertThat(found.isActive(Instant.now())).isTrue();
        assertThat(found.getUser().getEmail()).isEqualTo("carol@example.com");
    }  
}
