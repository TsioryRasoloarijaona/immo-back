package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Total d'un nœud du bilan ventilé sur une tranche de l'étude ; dérivé, réécrit à chaque recalcul. */
@Getter
@Setter
@Entity
@Table(name = "StkFSPrevisionStkTranche")
public class StkFSPrevisionStkTranche extends LignePrevisionOmran {
    // Tranche de l'étude (StkFeasibilityStudyCfgTranche.PKey) ; supprimée en CASCADE avec elle.
    @Column(name = "StkTrancheId")
    private UUID stkTrancheId;
}
