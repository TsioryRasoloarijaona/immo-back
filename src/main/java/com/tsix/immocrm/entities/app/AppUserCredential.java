package com.tsix.immocrm.entities.app;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * dbo.AppUserCredential — app-owned table (not part of the legacy schema, see
 * {@code instructions/} auth discussion) holding what the new web API needs to authenticate an
 * {@link AdmUser} that the legacy schema has no room for: a fresh bcrypt hash (the legacy
 * {@code AdmUser.Pass} is encrypted with an algorithm/key only the old desktop app knows, so it
 * can't be reused), first-login gating, and JWT revocation.
 * <p>
 * One row per {@link AdmUser} that has ever logged into the web app — created on bootstrap/first
 * password reset, not eagerly for every legacy account.
 */
@Getter
@Setter
@Entity
@Table(name = "AppUserCredential")
public class AppUserCredential {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AdmUserId", nullable = false, unique = true)
    private AdmUser admUser;

    /** BCrypt hash of the web password — never the legacy {@code AdmUser.Pass}. */
    @NotNull
    @Size(max = 255)
    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Legacy onboarding flag, kept because the column exists. Blocks nothing since
     * FEATURE-acces-arpent-depuis-omran.md (an admin reset sets it to false).
     */
    @Column(name = "MustChangePassword", nullable = false)
    private Boolean mustChangePassword = true;

    /**
     * Legacy "première connexion" flag, kept because the column exists. Blocks nothing since
     * FEATURE-acces-arpent-depuis-omran.md (an admin reset sets it to true).
     */
    @Column(name = "ProfileCompleted", nullable = false)
    private Boolean profileCompleted = false;

    /**
     * Bumped on password change/admin reset/explicit logout-everywhere — every issued JWT embeds
     * the version it was signed with, so bumping this instantly invalidates older tokens without
     * needing a token blacklist.
     */
    @Column(name = "TokenVersion", nullable = false)
    private Integer tokenVersion = 0;

    @Column(name = "UserCreateDateTime", nullable = false)
    private LocalDateTime userCreateDateTime;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
