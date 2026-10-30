package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Rôle métier, attribué via {@link AdmUserRole} ; {@code Code} alimente {@code hasRole}. */
@Getter
@Setter
@Entity
@Table(name = "AdmRole")
public class AdmRole {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

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

    /** 0 = rôle libre : ses comptes voient tous les rapports ; NULL vaut 1 (FEATURE-tdb-vente-rapports-socle.md §2). */
    @Column(name = "WithReportRestriction")
    private Boolean withReportRestriction;

    @Column(name = "WithAddAttachementRestriction")
    private Boolean withAddAttachementRestriction;

    @Column(name = "WithEditAttachementRestriction")
    private Boolean withEditAttachementRestriction;

    @Column(name = "WithDeleteAttachementRestriction")
    private Boolean withDeleteAttachementRestriction;

    @Column(name = "WithAddActionMessageRestriction")
    private Boolean withAddActionMessageRestriction;

    @Column(name = "WithEditActionMessageRestriction")
    private Boolean withEditActionMessageRestriction;

    @Column(name = "WithDeleteActionMessageRestriction")
    private Boolean withDeleteActionMessageRestriction;

    @Column(name = "WithAuditRestriction")
    private Boolean withAuditRestriction;

    @Column(name = "WithSpecificFieldsRestriction")
    private Boolean withSpecificFieldsRestriction;

    @Column(name = "WithSequenceFreeRestriction")
    private Boolean withSequenceFreeRestriction;
}
