package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.stl.StlCategory;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Type de frais d'un article ; la clé de la table s'appelle {@code Pkey}, pas {@code PKey}. */
@Getter
@Setter
@Entity
@Table(name = "StkExpenseType")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
public class StkExpenseType extends AuditOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlCategoryId")
    private StlCategory stlCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketTypeId")
    private PrjMarketType prjMarketType;
}
