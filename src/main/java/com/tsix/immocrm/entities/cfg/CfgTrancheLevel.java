package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.lnd.LndLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Onglet Étages de la tranche ; sans lien avec {@code StkItem.StkFloorId}. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheLevel")
public class CfgTrancheLevel extends LigneTrancheOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LndLevelId")
    private LndLevel lndLevel;
}
