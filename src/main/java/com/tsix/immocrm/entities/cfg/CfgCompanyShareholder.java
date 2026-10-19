package com.tsix.immocrm.entities.cfg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Ligne de répartition des parts d'une entité juridique. */
@Getter
@Setter
@Entity
@Table(name = "CfgCompanyShareholder")
public class CfgCompanyShareholder extends LigneActionnaireOmran {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    /** Nombre de parts (entier). */
    @Column(name = "SharingNumber")
    private Integer sharingNumber;
}
