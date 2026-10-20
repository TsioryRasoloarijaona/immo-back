package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Plan d'étage d'une tranche ({@code StkHierarchyId} = la tranche). {@code PlanImage} (data-URI de plusieurs Mo) n'est
 * volontairement pas mappée : lue et écrite par {@code StkHierarchyPlanRepository} seulement, jamais avec les méta.
 */
@Getter
@Setter
@Entity
@Table(name = "StkHierarchyPlan")
public class StkHierarchyPlan extends AuditOmran {
    @Column(name = "StkHierarchyId")
    private UUID stkHierarchyId;

    @Column(name = "StkFloorId")
    private UUID stkFloorId;

    @Column(name = "width", precision = 26, scale = 6)
    private BigDecimal largeur;

    @Column(name = "height", precision = 26, scale = 6)
    private BigDecimal hauteur;

    @Column(name = "X", precision = 26, scale = 6)
    private BigDecimal x;

    @Column(name = "Y", precision = 26, scale = 6)
    private BigDecimal y;
}
