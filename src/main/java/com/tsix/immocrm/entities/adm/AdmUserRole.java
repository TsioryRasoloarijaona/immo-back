package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** dbo.AdmUserRole — join row assigning one {@link AdmRole} to one {@link AdmUser}. */
@Getter
@Setter
@Entity
@Table(name = "AdmUserRole")
public class AdmUserRole {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmUserId")
    private AdmUser admUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmRoleId")
    private AdmRole admRole;
}
