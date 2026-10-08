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
 * Table applicative : hash bcrypt et révocation JWT d'un {@link AdmUser}. Une ligne par compte
 * passé par le web, créée à l'amorçage ou à la première réinitialisation, pas pour chaque compte.
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

    @NotNull
    @Size(max = 255)
    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    /** Ne bloque rien (voir FEATURE-acces-arpent-depuis-omran.md) ; une réinitialisation admin le met à false. */
    @Column(name = "MustChangePassword", nullable = false)
    private Boolean mustChangePassword = true;

    /** Ne bloque rien (voir FEATURE-acces-arpent-depuis-omran.md) ; une réinitialisation admin le met à true. */
    @Column(name = "ProfileCompleted", nullable = false)
    private Boolean profileCompleted = false;

    /** Embarquée dans chaque JWT : l'incrémenter révoque tous les jetons antérieurs, sans liste noire. */
    @Column(name = "TokenVersion", nullable = false)
    private Integer tokenVersion = 0;

    @Column(name = "UserCreateDateTime", nullable = false)
    private LocalDateTime userCreateDateTime;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
