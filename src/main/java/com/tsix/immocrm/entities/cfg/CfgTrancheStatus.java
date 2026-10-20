package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.com.ComFolderStatusPaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "CfgTrancheStatus")
public class CfgTrancheStatus extends LigneOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    @Column(name = "ThresholdOperator")
    private Integer thresholdOperator;

    @Column(name = "ThresholdRatio", precision = 26, scale = 6)
    private BigDecimal thresholdRatio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComFolderStatusPaymentTypeId")
    private ComFolderStatusPaymentType comFolderStatusPaymentType;

    /** Statut dont c'est le seuil de versement ; {@code CfgTrancheId} nul = seuil global. */
    @Column(name = "StrStatusId")
    private UUID strStatusId;

    @Column(name = "StrFolderEntityId")
    private UUID strFolderEntityId;
}
