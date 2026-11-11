package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
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

/** Décompte d'un lot ; plusieurs colonnes ont une minuscule initiale dans la base (casse recopiée). */
@Getter
@Setter
@Entity
@Table(name = "PrjWorkingOut")
public class PrjWorkingOut extends AuditOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketId")
    private PrjMarket prjMarket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    // Seul lien décompte → facture ; BuyDocument.PrjWorkingOutId ne sert qu'aux avances.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuyDocumentId")
    private BuyDocument buyDocument;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "FinalWorkingOut")
    private Boolean finalWorkingOut;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;

    @Column(name = "AmountVat", precision = 26, scale = 6)
    private BigDecimal amountVat;

    @Column(name = "AmountTTC", precision = 26, scale = 6)
    private BigDecimal amountTTC;

    @Column(name = "AmountToPay", precision = 26, scale = 6)
    private BigDecimal amountToPay;

    @Column(name = "AmountTTCAfterDeductions", precision = 26, scale = 6)
    private BigDecimal amountTTCAfterDeductions;

    @Column(name = "WorkingOutAmountTTC", precision = 26, scale = 6)
    private BigDecimal workingOutAmountTTC;

    @Column(name = "WarrantyAmount", precision = 26, scale = 6)
    private BigDecimal warrantyAmount;

    @Column(name = "WarrantyRatio", precision = 26, scale = 6)
    private BigDecimal warrantyRatio;

    @Column(name = "AdvanceAmount", precision = 26, scale = 6)
    private BigDecimal advanceAmount;

    @Column(name = "WorkingOutAdvanceAmount", precision = 26, scale = 6)
    private BigDecimal workingOutAdvanceAmount;

    @Column(name = "proportionAmount", precision = 26, scale = 6)
    private BigDecimal proportionAmount;

    @Column(name = "insuranceDECAmount", precision = 26, scale = 6)
    private BigDecimal insuranceDECAmount;

    @Column(name = "insuranceTRCAmount", precision = 26, scale = 6)
    private BigDecimal insuranceTRCAmount;

    @Column(name = "extraAmount", precision = 26, scale = 6)
    private BigDecimal extraAmount;

    @Column(name = "nonRefundableRetainer", precision = 26, scale = 6)
    private BigDecimal nonRefundableRetainer;

    @Column(name = "refundableRetainer", precision = 26, scale = 6)
    private BigDecimal refundableRetainer;

    @Column(name = "amountToBePaid", precision = 26, scale = 6)
    private BigDecimal amountToBePaid;

    @Column(name = "ProrataRefundAmount", precision = 26, scale = 6)
    private BigDecimal prorataRefundAmount;

    @Column(name = "StockAmountToPay", precision = 26, scale = 6)
    private BigDecimal stockAmountToPay;

    @Column(name = "Fluctuations", precision = 26, scale = 6)
    private BigDecimal fluctuations;

    @Column(name = "FluctuationsToPay", precision = 26, scale = 6)
    private BigDecimal fluctuationsToPay;

    @Nationalized
    @Size(max = 255)
    @Column(name = "DocRef")
    private String docRef;

    @Nationalized
    @Size(max = 1000)
    @Column(name = "Notes")
    private String notes;

    @Nationalized
    @Size(max = 255)
    @Column(name = "SupplierDescription")
    private String supplierDescription;

    @Column(name = "StlCurrencyId")
    private UUID stlCurrencyId;

    @Column(name = "AmountRegul", precision = 26, scale = 6)
    private BigDecimal amountRegul;

    @Column(name = "AdvanceRatio", precision = 26, scale = 6)
    private BigDecimal advanceRatio;

    @Column(name = "insuranceDECRatio", precision = 26, scale = 6)
    private BigDecimal insuranceDECRatio;

    @Column(name = "insuranceTRCRatio", precision = 26, scale = 6)
    private BigDecimal insuranceTRCRatio;

    @Column(name = "proportionRatio", precision = 26, scale = 6)
    private BigDecimal proportionRatio;

    @Column(name = "advanceRest", precision = 26, scale = 6)
    private BigDecimal advanceRest;

    @Column(name = "insuranceDECRest", precision = 26, scale = 6)
    private BigDecimal insuranceDECRest;

    @Column(name = "insuranceTRCRest", precision = 26, scale = 6)
    private BigDecimal insuranceTRCRest;

    @Column(name = "InsuranceDECAmountMax", precision = 26, scale = 6)
    private BigDecimal insuranceDECAmountMax;

    @Column(name = "InsuranceTRCAmountMax", precision = 26, scale = 6)
    private BigDecimal insuranceTRCAmountMax;

    @Column(name = "WorkingOutAdvanceAmountRestAtCreation", precision = 26, scale = 6)
    private BigDecimal workingOutAdvanceAmountRestAtCreation;

    @Column(name = "StockPileRatio", precision = 26, scale = 6)
    private BigDecimal stockPileRatio;

    @Column(name = "StockAmount", precision = 26, scale = 6)
    private BigDecimal stockAmount;

    @Column(name = "StockAmountHT", precision = 26, scale = 6)
    private BigDecimal stockAmountHT;

    @Column(name = "StockAmountOld", precision = 26, scale = 6)
    private BigDecimal stockAmountOld;

    @Column(name = "StockAmountOldHT", precision = 26, scale = 6)
    private BigDecimal stockAmountOldHT;

    @Column(name = "StockAmountToPayHT", precision = 26, scale = 6)
    private BigDecimal stockAmountToPayHT;

    @Column(name = "FluctuationsOld", precision = 26, scale = 6)
    private BigDecimal fluctuationsOld;

    @Column(name = "FluctuationsHT", precision = 26, scale = 6)
    private BigDecimal fluctuationsHT;

    @Column(name = "FluctuationsOldHT", precision = 26, scale = 6)
    private BigDecimal fluctuationsOldHT;

    @Column(name = "FluctuationsToPayHT", precision = 26, scale = 6)
    private BigDecimal fluctuationsToPayHT;

    @Column(name = "AdvanceBasedOnAmount")
    private Boolean advanceBasedOnAmount;

    @Column(name = "WarrantyBasedOnAmount")
    private Boolean warrantyBasedOnAmount;

    @Column(name = "DECBasedOnAmount")
    private Boolean decBasedOnAmount;

    @Column(name = "TRCBasedOnAmount")
    private Boolean trcBasedOnAmount;

    @Column(name = "ProrataBasedOnAmount")
    private Boolean prorataBasedOnAmount;
}
