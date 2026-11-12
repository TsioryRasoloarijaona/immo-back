package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Demande interne d'achat (DIA) ; lue seulement (FEATURE-lot-fiche-parite.md E5), l'écriture est en L3. */
@Getter
@Entity
@Immutable
@Table(name = "BuySelection")
public class BuySelection extends CodeLibelleOmran {

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuySelectionKindId")
    private BuySelectionKind procedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;
}
