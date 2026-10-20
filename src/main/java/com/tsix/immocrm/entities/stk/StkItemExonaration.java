package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
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

import java.math.BigDecimal;

/** Quote-part exonérée d'une tranche pour une année (écran OMRAN Autres › Quote part exonérée). */
@Getter
@Setter
@Entity
@Table(name = "StkItemExonaration")
public class StkItemExonaration extends LigneOmran {
    @Column(name = "DocYear")
    private Integer docYear;

    @Column(name = "UnitCost", precision = 26, scale = 6)
    private BigDecimal unitCost;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;
}
