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

/** Lien rapport ↔ rôle (droit d'un rôle restreint) ; doublons réels en base : toujours lire en DISTINCT. */
@Getter
@Setter
@Entity
@Table(name = "AdmReportDataRole")
public class AdmReportDataRole {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmRoleId")
    private AdmRole admRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmReportDataId")
    private AdmReportData admReportData;
}
