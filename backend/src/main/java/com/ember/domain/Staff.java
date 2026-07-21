package com.ember.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A staff member. Two credentials, either optional: a numeric <b>PIN</b> for quick
 * sign-in at the POS/kitchen stations, and a <b>password</b> for the admin
 * back-office. Both are stored only as BCrypt hashes.
 */
@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole role;

    /** BCrypt hash of the station PIN, or null if this member can't sign in at a station. */
    @Column(name = "pin_hash")
    private String pinHash;

    /** BCrypt hash of the admin password, or null if this member has no back-office access. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Staff() { }

    public Staff(String username, String displayName, StaffRole role) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public StaffRole getRole() { return role; }
    public void setRole(StaffRole role) { this.role = role; }

    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
}
