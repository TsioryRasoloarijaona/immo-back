package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Avancement d'un critère du projet à un niveau, relevé à une réunion ; {@code Achievement} en fraction. */
@Getter
@Setter
@Entity
@Table(name = "BulMeetingPrjProjectProgressCriteriaLine")
public class BulMeetingPrjProjectProgressCriteriaLine extends LigneOmran {
    @Column(name = "Achievement", precision = 26, scale = 6)
    private BigDecimal achievement;

    @Column(name = "BulMeetingId")
    private UUID bulMeetingId;

    @Column(name = "PrjProjectProgressCriteriaLineId")
    private UUID prjProjectProgressCriteriaLineId;

    // « d » minuscule dans le nom réel de la colonne.
    @Column(name = "CfgTrancheLevelingdefinitionId")
    private UUID cfgTrancheLevelingDefinitionId;
}
