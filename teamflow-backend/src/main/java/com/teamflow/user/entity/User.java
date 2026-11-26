package com.teamflow.user.entity;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity 
@Table (name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Email de connexion. Son unicité est garantie par la base de donnée.
    @Column(name ="email", nullable = false, length = 255)
    private String email;

    // Mdp haché avec BCrypt. On ne stcocke et on ne manipule jamais le mdp en clair.
    @Column(name ="password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    //Permet de désactiver un compte sans le supprimer.
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User(){

    }

    public User(String email, String passwordHash, String firstName, String lastName){
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate 
    void onUpdate(){
        this.updatedAt = Instant.now();
    }

    public UUID getId(){
        return id;
    }

    public String getEmail(){
        return email;
    }

    public String getPasswordhash(){
        return passwordHash;
    }

    public String getFirstname(){
        return firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public Instant getCreatedAt(){
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
