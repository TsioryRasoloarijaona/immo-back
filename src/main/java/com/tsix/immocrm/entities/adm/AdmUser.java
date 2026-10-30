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
import org.hibernate.annotations.Formula;
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

    // Code et Email sont des CLOB pour Hibernate : upper() les refuse, la recherche passe par ces colonnes calculées.
    @Formula("UPPER(Code)")
    private String codeMajuscule;

    @Formula("UPPER(Email)")
    private String emailMajuscule;

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

    @Column(name = "LinesPerPage")
    private Integer linesPerPage;

    @Column(name = "ConfirmDelete")
    private Boolean confirmDelete;

    @Column(name = "LockDelay")
    private Integer lockDelay;

    /** Ne donne aucun droit : seuls les {@code AdmUserRole} comptent. */
    @Column(name = "WebRoleId")
    private UUID webRoleId;

    @Column(name = "WebCultureId")
    private UUID webCultureId;

    @Column(name = "IsVisibleForChat")
    private Boolean visibleForChat;

    @Column(name = "CanDisapearNotification")
    private Boolean canDisapearNotification;

    @Column(name = "IsMobileUser")
    private Boolean mobileUser;

    @Size(max = 255)
    @Nationalized
    @Column(name = "EmailSMTPServer")
    private String emailSmtpServer;

    @Column(name = "EmailPortNumber")
    private Integer emailPortNumber;

    @Column(name = "EmailSSLActive")
    private Boolean emailSslActive;

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
