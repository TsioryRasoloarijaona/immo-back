package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ComFolder")
public class ComFolder {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StkVocationId", nullable = false)
    private StkVocation stkVocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCustomerId")
    private CfgTier cfgCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCommercialId")
    private CfgTier cfgCommercial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Column(name = "UnitCost", precision = 26, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "ItemPrice", precision = 26, scale = 6)
    private BigDecimal itemPrice;

    @Column(name = "UnderItemsPrice", precision = 26, scale = 6)
    private BigDecimal underItemsPrice;

    @Column(name = "StatusValidityDate")
    private LocalDateTime statusValidityDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComPaymentMethodId")
    private ComPaymentMethod comPaymentMethod;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Contact")
    private String contact;

    @Size(max = 255)
    @Nationalized
    @Column(name = "\"Usage\"")
    private String usage;

    @Column(name = "CancelDate")
    private LocalDateTime cancelDate;

    @Column(name = "ModificationsAmount", precision = 26, scale = 6)
    private BigDecimal modificationsAmount;

    @Size(max = 255)
    @Nationalized
    @Column(name = "PenalityDescription")
    private String penalityDescription;

    @Column(name = "PriceBeforeDiscount", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscount;

    @Column(name = "Discount", precision = 26, scale = 6)
    private BigDecimal discount;

    @Column(name = "DiscountAmount", precision = 26, scale = 6)
    private BigDecimal discountAmount;

    @Column(name = "PriceAfterDiscount", precision = 26, scale = 6)
    private BigDecimal priceAfterDiscount;

    @Column(name = "Price", precision = 26, scale = 6)
    private BigDecimal price;

    @Column(name = "VATRatio", precision = 26, scale = 6)
    private BigDecimal vATRatio;

    @Column(name = "PriceHT", precision = 26, scale = 6)
    private BigDecimal priceHT;

    @Column(name = "TotalToPay", precision = 26, scale = 6)
    private BigDecimal totalToPay;

    @Column(name = "DeliveryDateReal")
    private LocalDateTime deliveryDateReal;

    @Column(name = "DeliveryRatio", precision = 26, scale = 6)
    private BigDecimal deliveryRatio;

    @Column(name = "DeliveryMaxNumber")
    private Integer deliveryMaxNumber;

    @Column(name = "DeliveryNumber")
    private Integer deliveryNumber;

    @Column(name = "DeliveryAmount", precision = 26, scale = 6)
    private BigDecimal deliveryAmount;

    @Column(name = "AgreementDate")
    private LocalDateTime agreementDate;

    @Column(name = "AgreementSavingDate")
    private LocalDateTime agreementSavingDate;

    @Column(name = "OwnershipCertificateDate")
    private LocalDateTime ownershipCertificateDate;

    @Column(name = "ReceptionDate")
    private LocalDateTime receptionDate;

    @Column(name = "PriceWithModification")
    private Boolean priceWithModification;

    @Column(name = "CreditApprovalDate")
    private LocalDateTime creditApprovalDate;

    @Column(name = "CreditPossessionDate")
    private LocalDateTime creditPossessionDate;

    @Column(name = "AdvanceDate")
    private LocalDateTime advanceDate;

    @Column(name = "AgreementAmount", precision = 26, scale = 6)
    private BigDecimal agreementAmount;

    @Column(name = "AgreementPrice", precision = 26, scale = 6)
    private BigDecimal agreementPrice;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionId")
    private ComProspection comProspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgLawyerId")
    private CfgTier cfgLawyer;

    @Column(name = "PartPercent", precision = 26, scale = 6)
    private BigDecimal partPercent;

    @Column(name = "Revival1")
    private LocalDateTime revival1;

    @Column(name = "Revival2")
    private LocalDateTime revival2;

    @Column(name = "Revival1ValidityDate")
    private LocalDateTime revival1ValidityDate;

    @Column(name = "Revival2ValidityDate")
    private LocalDateTime revival2ValidityDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CreditAgency")
    private String creditAgency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreditBankId")
    private CfgTier creditBank;

    @Column(name = "InsuranceDate")
    private LocalDateTime insuranceDate;

    @Column(name = "BankRecordDate")
    private LocalDateTime bankRecordDate;

    @Column(name = "InterventionDate")
    private LocalDateTime interventionDate;

    @Column(name = "AgreementDatePrevisionel")
    private LocalDateTime agreementDatePrevisionel;

    @Column(name = "IsConfirmedAgreementDate")
    private Boolean isConfirmedAgreementDate;

    @Column(name = "IsOldCustomer")
    private Boolean isOldCustomer;

    @Column(name = "DiscountChangeDate")
    private LocalDateTime discountChangeDate;

    @Column(name = "loanCompanySendDate")
    private LocalDateTime loanCompanySendDate;

    @Column(name = "loanCompanyDecisionDate")
    private LocalDateTime loanCompanyDecisionDate;

    // DB column is legacy SQL Server "text" (ANSI), not "ntext" — LONGVARCHAR (not
    // LONGNVARCHAR/@Nationalized) matches it. Using the Unicode variant makes the driver try to
    // read/bind it as NVARCHAR, which SQL Server rejects with "La conversion de text en NCHAR
    // n'est pas prise en charge" on any query touching ComFolder (e.g. GET /api/dossiers).
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "ComDocumentDetail")
    private String comDocumentDetail;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "CancelDescription", length = 1000)
    private String cancelDescription;

    @Column(name = "ReservationDate")
    private LocalDateTime reservationDate;

    @Column(name = "IsWithGiftChoiceLater")
    private Boolean isWithGiftChoiceLater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSaleManagerId")
    private CfgTier cfgSaleManager;

    @Column(name = "AgreementRegistryDate")
    private LocalDateTime agreementRegistryDate;

    @Column(name = "UnitCostHT", precision = 26, scale = 6)
    private BigDecimal unitCostHT;

    @Column(name = "PrecisionActDate")
    private LocalDateTime precisionActDate;

    @Column(name = "ModificationsAmountHT", precision = 26, scale = 6)
    private BigDecimal modificationsAmountHT;

    @Column(name = "ItemPriceHT", precision = 26, scale = 6)
    private BigDecimal itemPriceHT;

    @Column(name = "UnderItemsPriceHT", precision = 26, scale = 6)
    private BigDecimal underItemsPriceHT;

    @Column(name = "PriceBeforeDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscountHT;

    @Column(name = "ItemPriceAfterHT", precision = 26, scale = 6)
    private BigDecimal itemPriceAfterHT;

    @Column(name = "UnderItemsPriceAfterHT", precision = 26, scale = 6)
    private BigDecimal underItemsPriceAfterHT;

    @Column(name = "PriceAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceAfterDiscountHT;

    @Column(name = "ItemPriceAfter", precision = 26, scale = 6)
    private BigDecimal itemPriceAfter;

    @Column(name = "UnderItemsPriceAfter", precision = 26, scale = 6)
    private BigDecimal underItemsPriceAfter;

    @Column(name = "GlobalArea", precision = 26, scale = 6)
    private BigDecimal globalArea;

    @Column(name = "IsAccounted")
    private Boolean isAccounted;

    @Column(name = "ReservationDateCustomer")
    private LocalDateTime reservationDateCustomer;

    @Column(name = "AgreementSavingDateCustomer")
    private LocalDateTime agreementSavingDateCustomer;

    @Column(name = "DeliveryDateRealCustomer")
    private LocalDateTime deliveryDateRealCustomer;

    @Column(name = "PrecisionActDateCustomer")
    private LocalDateTime precisionActDateCustomer;

    @Column(name = "ReservationRegistryDate")
    private LocalDateTime reservationRegistryDate;

    @Column(name = "DeliveryRegistryDate")
    private LocalDateTime deliveryRegistryDate;

    @Column(name = "PrecisionActRegistryDate")
    private LocalDateTime precisionActRegistryDate;

    @Column(name = "ContractPrintDate")
    private LocalDateTime contractPrintDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSaleManager2Id")
    private CfgTier cfgSaleManager2Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgOwnerId")
    private CfgTier cfgOwner;

    @Column(name = "CommissionRate", precision = 26, scale = 6)
    private BigDecimal commissionRate;

    @Column(name = "ProbabilityOfFinalisation", precision = 26, scale = 6)
    private BigDecimal probabilityOfFinalisation;

    @Column(name = "RomInterest", precision = 26, scale = 6)
    private BigDecimal romInterest;

    @Column(name = "IsPenaltyDisabled")
    private Boolean isPenaltyDisabled;

    @Column(name = "PrecisionActDateLegalization")
    private LocalDateTime precisionActDateLegalization;

    @Column(name = "PrecisionActDateLawyer")
    private LocalDateTime precisionActDateLawyer;

    @Column(name = "AgreementSavingDateLawyer")
    private LocalDateTime agreementSavingDateLawyer;

    @Column(name = "AgreementSavingDateForLegalization")
    private LocalDateTime agreementSavingDateForLegalization;

    @Column(name = "AgreementSavingDateLegalization")
    private LocalDateTime agreementSavingDateLegalization;

    @Column(name = "AgreementSavingDateForRegistry")
    private LocalDateTime agreementSavingDateForRegistry;

    @Column(name = "PrecisionActDateForLegalization")
    private LocalDateTime precisionActDateForLegalization;

    @Column(name = "PrecisionActDateForRegistry")
    private LocalDateTime precisionActDateForRegistry;

    @Column(name = "FinalTitleDateDeposit")
    private LocalDateTime finalTitleDateDeposit;

    @Column(name = "FinalTitleDateWithdrawal")
    private LocalDateTime finalTitleDateWithdrawal;

    @Column(name = "FinalTitleDate")
    private LocalDateTime finalTitleDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FinalTitleNumber")
    private String finalTitleNumber;

    // ------------------------------------------------------------------
    // Délais de cycle de vente — recopiés de ComFolderView, qui les calcule en DATEDIFF.
    //
    // En @Formula et non calculés en Java pour une raison précise : ce sont alors des propriétés
    // ordinaires, donc utilisables comme clé de ?groupBy= et comme colonne de ?colonnes= sans que
    // le moteur de groupement ait à connaître la notion de « champ calculé ». Un calcul côté Java
    // aurait produit une colonne affichable mais NON groupable — une incohérence de plus à
    // expliquer dans le catalogue.
    //
    // Coût mesuré sur yesRemsProd : page de 20 dossiers, 1 ms avec les trois formules contre
    // 0-1 ms sans ; 6 ms sur les 358 dossiers. La sous-requête corrélée de delaiConcretisation
    // ne change pas l'ordre de grandeur.
    //
    // `DelayCredit` de la vue n'est PAS repris : il dépend de CreditPossessionDate, mesurée à
    // 0 ligne sur 358. Il vaudrait null partout.
    // ------------------------------------------------------------------

    /**
     * Jours entre l'origine du dossier et sa concrétisation. Calculable sur 152 des 358 dossiers.
     *
     * <p>L'origine est la date de la <strong>prospection</strong> quand il y en a une, sinon celle
     * du dossier — c'est le {@code IIF(ComProspectionId is null, …)} de la vue, rendu ici par un
     * {@code COALESCE} qui lui est équivalent : sans prospection, la sous-requête ne rend rien.
     */
    @Formula("DATEDIFF(day, COALESCE((SELECT p.DocDate FROM ComProspection p "
            + "WHERE p.PKey = ComProspectionId), DocDate), AdvanceDate)")
    private Integer delaiConcretisation;

    /** Jours entre la concrétisation et le compromis. Calculable sur 73 dossiers. */
    @Formula("DATEDIFF(day, AdvanceDate, AgreementDate)")
    private Integer delaiNotaire;

    /** Jours entre le compromis et la livraison réelle. Calculable sur 63 dossiers. */
    @Formula("DATEDIFF(day, AgreementDate, DeliveryDateReal)")
    private Integer delaiLivraison;
}