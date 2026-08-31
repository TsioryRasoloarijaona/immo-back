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
 * dbo.AdmUser — a login account for the legacy desktop app, reused as-is for the new API's
 * authentication (see {@code AppUserCredential}). {@code Code} is the username; {@code Pass} is
 * the legacy account's password, encrypted with an algorithm/key only the old desktop app knows
 * — it is mapped read-only for reference but never used to authenticate (see
 * {@code AppUserCredential.passwordHash}, a fresh bcrypt hash set on first web login). Only the
 * columns relevant to web auth are mapped; {@code WebRoleId}, {@code WebCultureId},
 * {@code WebAlertDefinitionId}, {@code EmailPassword}/{@code EmailPortNumber}/
 * {@code EmailSMTPServer}/{@code EmailSSLActive}, {@code OpenBeeCode}/{@code OpenBeePass},
 * {@code WebPass}, {@code LinesPerPage}, {@code ConfirmDelete}, {@code LockDelay},
 * {@code IsVisibleForChat}, {@code CanDisapearNotification} and {@code IsMobileUser} are left
 * unmapped since no dedicated feature reads them yet.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmUser")
public class AdmUser {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /** Username. */
    @Lob
    @Column(name = "Code", nullable = false)
    private String code;

    /** Legacy encrypted password — informational only, never used to authenticate. See class doc. */
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

    /** The employee/commercial (CfgTier) this account belongs to. */
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
