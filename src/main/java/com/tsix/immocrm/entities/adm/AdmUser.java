package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compte de connexion du logiciel de bureau, réutilisé pour l'authentification web
 * ({@code AppUserCredential} porte le hash bcrypt). {@code Code} = identifiant.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmUser")
public class AdmUser {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Lob
    @Column(name = "Code", nullable = false)
    private String code;

    /** Chiffré par un algorithme que seul le logiciel de bureau connaît : jamais utilisé pour authentifier. */
    @Nationalized
    @Lob
    @Column(name = "Pass")
    private String pass;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Nationalized
    @Lob
    @Column(name = "Email")
    private String email;

    @Nationalized
    @Lob
    @Column(name = "Phone")
    private String phone;

    @Nationalized
    @Lob
    @Column(name = "Mobile")
    private String mobile;

    @Column(name = "Disabled")
    private Boolean disabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgEmployeeId")
    private CfgTier cfgEmployee;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
