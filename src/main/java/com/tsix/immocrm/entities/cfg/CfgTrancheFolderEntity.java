package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Onglet Finalisations : statuts du type de dossier où s'appliquent les commissions de finalisation. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheFolderEntity")
public class CfgTrancheFolderEntity extends LigneTrancheOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrFolderEntityId")
    private StrEntity strFolderEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusDeliveryId1")
    private StrStatus strStatusDelivery1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusDeliveryId2")
    private StrStatus strStatusDelivery2;
}
