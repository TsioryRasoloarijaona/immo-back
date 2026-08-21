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

/**
 * dbo.AdmRole — a business role (e.g. "Commercial", "Administrateur", "Comptabilité"), assigned
 * to {@link AdmUser} accounts via {@link AdmUserRole} (many-to-many). Drives API authorization
 * ({@code hasRole(role.code)}). Only {@code Code}/{@code Description} are mapped; the fine-grained
 * {@code WithXXXRestriction} flags (edit/delete action-message, attachment, audit...) are legacy
 * desktop-app permissions left unmapped until an endpoint actually needs them.
 */
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
}
