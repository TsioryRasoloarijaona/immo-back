package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.stl.StlCategory;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Colonnes communes de la nomenclature d'achat (famille, sous-famille, article) ; {@code VatRatio}, morte, non mappée. */
@Getter
@Setter
@MappedSuperclass
public abstract class NomenclatureAchat extends AuditOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlCategoryId")
    private StlCategory stlCategory;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
