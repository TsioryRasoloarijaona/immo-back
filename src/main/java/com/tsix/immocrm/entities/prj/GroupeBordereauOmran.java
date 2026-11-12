package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Colonnes communes aux regroupements des bordereaux définitif et estimatif (étage, appartement…). */
@Getter
@Setter
@MappedSuperclass
public abstract class GroupeBordereauOmran extends AuditOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    // Sous-groupe : regroupement parent du même bordereau, sans FK en base.
    @Column(name = "ParentId")
    private UUID parentId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "IsStock")
    private Boolean isStock;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
