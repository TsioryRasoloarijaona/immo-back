package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** « Type Présentation » de la grille Articles : table propre, propre à chaque tranche ({@code StkHierarchyId}). */
@Getter
@Setter
@Entity
@Table(name = "StkPresentationType")
public class StkPresentationType extends AuditOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkHierarchyId")
    private CfgTranche stkHierarchy;
}
