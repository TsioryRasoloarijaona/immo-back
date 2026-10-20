package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Unité d'une étude ventilée sur une de ses tranches. {@code StkBaseUnitId} pointe la LIGNE d'unité de l'étude, pas
 * {@code StkFSBaseUnit} ; {@code FeasibilityStudyId} n'a pas de clé étrangère.
 */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyCfgTrancheStkBaseUnit")
public class StkFeasibilityStudyCfgTrancheStkBaseUnit extends LigneOmran {
    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "FeasibilityStudyId")
    private UUID feasibilityStudyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkBaseUnitId")
    private StkFSBaseUnitStkFeasibilityStudy ligneUnite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkFeasibilityStudyCfgTrancheId")
    private StkFeasibilityStudyCfgTranche tranche;

    @Column(name = "Weight", precision = 26, scale = 6)
    private BigDecimal weight;
}
