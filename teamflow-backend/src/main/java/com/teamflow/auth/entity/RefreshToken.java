package com.teamflow.auth.entity;

import java.time.Instant;
import java.util.UUID;

import com.teamflow.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Refresh token : un "badge de renouvellement" valable 7 jours.
 *
 * <p>Le token d'accès (JWT) ne vit que 15 minutes. Quand il expire, le navigateur présente son
 * refresh token pour en obtenir un nouveau, sans que l'utilisateur ressaisisse son mot de passe.
 *
 * <p>Trois principes de sécurité structurent cette classe :
 * <ul>
 *   <li><b>on ne stocke jamais le token lui-même</b>, seulement son empreinte SHA-256 : une fuite
 *       de la base ne permet pas de se connecter ;</li>
 *   <li><b>rotation</b> : à chaque renouvellement, le token utilisé est révoqué et remplacé par
 *       un nouveau, de sorte qu'un token ne sert qu'une seule fois ;</li>
 *   <li><b>famille</b> : tous les tokens issus d'une même connexion partagent un identifiant de
 *       famille. Si un token déjà révoqué est présenté à nouveau (signe de vol), toute la famille
 *       est révoquée et l'utilisateur doit se reconnecter.</li>
 * </ul>
 *
 * <p>La rotation sera faite par une requête SQL atomique (voir étape 2a-3), pas par JPA : cette
 * entité sert à créer et à lire les tokens.
 */

@Entity 
@Table(name ="refresh_token")
public class RefreshToken {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name ="user_id", nullable = false, updatable = false )
    private User user;

    @Column(name = "token_hash", nullable = false, length= 64, updatable = false)
    private String tokenHash;

    @Column(name ="family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken(){

    }

    /**
     * Les dates sont fournies par l'appelant plutôt que lues avec {@code Instant.now()} ici :
     * le futur service utilisera une horloge injectée, ce qui permettra de tester l'expiration
     * avec des dates choisies au lieu d'attendre réellement 7 jours.
     */

    public RefreshToken(User user, String tokenHash, UUID familyId, Instant createdAt, Instant expiresAt){
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(Instant now){
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public UUID getId(){
        return id;
    }

    public User getUser(){
        return user;
    }

    public String getTokenHash(){
        return tokenHash;
    }

     public UUID getFamilyId() {
        return familyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
