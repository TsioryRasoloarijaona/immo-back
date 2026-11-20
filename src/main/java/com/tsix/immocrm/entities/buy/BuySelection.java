package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.workflow.ObjetWorkflow;
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
import java.time.LocalDateTime;
import java.util.UUID;

/** Demande interne d'achat (DIA) ; écrite par FEATURE-n6-demandes-achat.md. */
@Getter
@Setter
@Entity
@Table(name = "BuySelection")
public class BuySelection extends AuditOmran implements ObjetWorkflow {

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Size(max = 500)
    @Nationalized
    @Column(name = "DescriptionLong")
    private String descriptionLong;

    @Nationalized
    @Column(name = "TextLetterSelection")
    private String textLetterSelection;

    @Column(name = "DefaultVat", precision = 26, scale = 6)
    private BigDecimal defaultVat;

    @Column(name = "PriceEstimatedHT", precision = 26, scale = 6)
    private BigDecimal priceEstimatedHT;

    @Column(name = "PriceEstimatedTTC", precision = 26, scale = 6)
    private BigDecimal priceEstimatedTTC;

    @Column(name = "LaunchOfTenderDate")
    private LocalDateTime launchOfTenderDate;

    @Column(name = "WithdrawalTenderDocumentsDate")
    private LocalDateTime withdrawalTenderDocumentsDate;

    @Column(name = "TenderMaturityDate")
    private LocalDateTime tenderMaturityDate;

    @Column(name = "TendersResortDate")
    private LocalDateTime tendersResortDate;

    @Column(name = "BidOpeningDate")
    private LocalDateTime bidOpeningDate;

    @Column(name = "TendersReportDate")
    private LocalDateTime tendersReportDate;

    @Column(name = "TenderDate")
    private LocalDateTime tenderDate;

    @Column(name = "ContractSignatureDate")
    private LocalDateTime contractSignatureDate;

    @Column(name = "IsTheLast")
    private Boolean isTheLast;

    @Column(name = "GroupId")
    private UUID groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuySelectionKindId")
    private BuySelectionKind procedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrEntityId")
    private StrEntity strEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    // Fournisseur retenu : posé par la sélection d'une offre (N6b), jamais par la saisie de l'en-tête.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    @Column(name = "CfgCompanyId")
    private UUID cfgCompanyId;

    @Column(name = "PrjProjectId")
    private UUID prjProjectId;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "PrjMarketTypeId")
    private UUID prjMarketTypeId;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "StlCurrencyId")
    private UUID stlCurrencyId;

    @Column(name = "StlSettlementTypeId")
    private UUID stlSettlementTypeId;

    @Column(name = "PaymentDelay")
    private Integer paymentDelay;

    @Column(name = "AdmModuleId")
    private UUID admModuleId;

    @Column(name = "CfgTierTriggerId")
    private UUID cfgTierTriggerId;
}
