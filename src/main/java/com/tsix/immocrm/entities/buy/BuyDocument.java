package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Facture fournisseur du module Achat d'OMRAN, mappée pour la seule facture de frais de terrain : les autres colonnes
 * restent NULL comme la ligne mesurée (FEATURE-patrimoine-terrains.md §3.2).
 */
@Getter
@Setter
@Entity
@Table(name = "BuyDocument")
public class BuyDocument extends AuditOmran {
    @Column(name = "DocDate")
    private LocalDateTime docDate;
    @Column(name = "MaturityDate")
    private LocalDateTime maturityDate;
    @Column(name = "PrevisionDeliveryDate")
    private LocalDateTime previsionDeliveryDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "DocType")
    private String docType;
    @Size(max = 255)
    @Nationalized
    @Column(name = "ReferenceType")
    private String referenceType;
    @Size(max = 255)
    @Nationalized
    @Column(name = "SupplierDescription")
    private String supplierDescription;
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;
    @Size(max = 255)
    @Nationalized
    @Column(name = "DocRef")
    private String docRef;
    @Size(max = 255)
    @Nationalized
    @Column(name = "PersonToContact")
    private String personToContact;
    @Nationalized
    @Column(name = "Notes2")
    private String notes2;
    @Size(max = 255)
    @Nationalized
    @Column(name = "OfficeOrderRef")
    private String officeOrderRef;

    @Column(name = "StkExpenseId")
    private UUID stkExpenseId;
    @Column(name = "CfgTierId")
    private UUID cfgTierId;
    @Column(name = "CfgCompanyId")
    private UUID cfgCompanyId;
    @Column(name = "PrjMarketTypeId")
    private UUID prjMarketTypeId;
    @Column(name = "AdmModuleId")
    private UUID admModuleId;
    @Column(name = "StlCurrencyId")
    private UUID stlCurrencyId;
    @Column(name = "StrEntityId")
    private UUID strEntityId;
    @Column(name = "BuyDocumentStatusId")
    private UUID buyDocumentStatusId;
    @Column(name = "CfgTierTriggerId")
    private UUID cfgTierTriggerId;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;
    @Column(name = "AmountVat", precision = 26, scale = 6)
    private BigDecimal amountVat;
    @Column(name = "AmountTTC", precision = 26, scale = 6)
    private BigDecimal amountTTC;
    @Column(name = "AmountToPay", precision = 26, scale = 6)
    private BigDecimal amountToPay;
    @Column(name = "FiscalStamp", precision = 26, scale = 6)
    private BigDecimal fiscalStamp;
    @Column(name = "AmountRegul", precision = 26, scale = 6)
    private BigDecimal amountRegul;
    @Column(name = "StockAmountToPay", precision = 26, scale = 6)
    private BigDecimal stockAmountToPay;
    @Column(name = "StockAmountToPayHT", precision = 26, scale = 6)
    private BigDecimal stockAmountToPayHT;
    @Column(name = "WorkingOutAdvanceAmount", precision = 26, scale = 6)
    private BigDecimal workingOutAdvanceAmount;
    @Column(name = "RetainedAmount", precision = 26, scale = 6)
    private BigDecimal retainedAmount;
    @Column(name = "ProrataRefundAmount", precision = 26, scale = 6)
    private BigDecimal prorataRefundAmount;
    @Column(name = "FluctuationsToPay", precision = 26, scale = 6)
    private BigDecimal fluctuationsToPay;
    @Column(name = "FluctuationsToPayHT", precision = 26, scale = 6)
    private BigDecimal fluctuationsToPayHT;

    // DEFAULT(0) en production et en CI, aucun DEFAULT dans yesRemsTest : écrites explicitement.
    @Column(name = "AdvanceRatio", precision = 26, scale = 6)
    private BigDecimal advanceRatio;
    @Column(name = "AdvanceAmount", precision = 26, scale = 6)
    private BigDecimal advanceAmount;
    @Column(name = "AdvanceRest", precision = 26, scale = 6)
    private BigDecimal advanceRest;
    @Column(name = "WarrantyRatio", precision = 26, scale = 6)
    private BigDecimal warrantyRatio;
    @Column(name = "WarrantyAmount", precision = 26, scale = 6)
    private BigDecimal warrantyAmount;
    @Column(name = "ProportionRatio", precision = 26, scale = 6)
    private BigDecimal proportionRatio;
    @Column(name = "ProportionAmount", precision = 26, scale = 6)
    private BigDecimal proportionAmount;
    @Column(name = "InsuranceDECRatio", precision = 26, scale = 6)
    private BigDecimal insuranceDECRatio;
    @Column(name = "InsuranceDECAmount", precision = 26, scale = 6)
    private BigDecimal insuranceDECAmount;
    @Column(name = "InsuranceDECRest", precision = 26, scale = 6)
    private BigDecimal insuranceDECRest;
    @Column(name = "InsuranceTRCRatio", precision = 26, scale = 6)
    private BigDecimal insuranceTRCRatio;
    @Column(name = "InsuranceTRCAmount", precision = 26, scale = 6)
    private BigDecimal insuranceTRCAmount;
    @Column(name = "InsuranceTRCRest", precision = 26, scale = 6)
    private BigDecimal insuranceTRCRest;

    @Column(name = "IsSelected")
    private Boolean isSelected;
    @Column(name = "IsClosed")
    private Boolean isClosed;
    @Column(name = "CanBePayed")
    private Boolean canBePayed;
    @Column(name = "IsPropotionCharged")
    private Boolean isPropotionCharged;
    @Column(name = "IsAccounted")
    private Boolean isAccounted;
    @Column(name = "LinesCount")
    private Integer linesCount;
}
