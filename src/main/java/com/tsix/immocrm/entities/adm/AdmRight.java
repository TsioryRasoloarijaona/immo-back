package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Droit d'OMRAN accordé à un rôle ; {@code Code} = clé du droit (ex. {@code …Monitoring.toolVerifyThreshold}). */
@Getter
@Setter
@Entity
@Table(name = "AdmRight")
public class AdmRight {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "AdmRoleId")
    private UUID admRoleId;
}
