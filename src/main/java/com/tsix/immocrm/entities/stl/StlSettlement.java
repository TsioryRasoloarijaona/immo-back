package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "StlSettlement")
public class StlSettlement {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "EcheanceDate")
    private LocalDateTime echeanceDate;

    @Column(name = "SettlementDate")
    private LocalDateTime settlementDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Column(name = "IsCredit")
    private Boolean isCredit;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Status")
    private String status;

    @Nationalized
    @Lob
    @Column(name = "Notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgBankId")
    private CfgTier cfgBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlSettlementTypeId")
    private StlSettlementType stlSettlementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TierId")
    private CfgTier tier;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TierType")
    private String tierType;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Category")
    private String category;

    @Column(name = "CategoryId")
    private UUID categoryId;

    @Nationalized
    @Lob
    @Column(name = "ItemNumber")
    private String itemNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Group1Label")
    private String group1Label;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Group2Label")
    private String group2Label;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Group3Label")
    private String group3Label;

    @Nationalized
    @Lob
    @Column(name = "ItemDescription")
    private String itemDescription;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ReceiptCode")
    private String receiptCode;

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
    @JoinColumn(name = "CfgTierOtherId")
    private CfgTier cfgTierOther;

    @Column(name = "IsDcoumentDeliveredToCenter")
    private Boolean isDcoumentDeliveredToCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgLawyerId")
    private CfgTier cfgLawyer;

    @Column(name = "DeliveredToCenterDate")
    private LocalDateTime deliveredToCenterDate;

    @Column(name = "AmountBase", precision = 26, scale = 6)
    private BigDecimal amountBase;

    @Column(name = "AmountRS", precision = 26, scale = 6)
    private BigDecimal amountRS;

    @Column(name = "RsRatio", precision = 26, scale = 6)
    private BigDecimal rsRatio;

    @Column(name = "RSAmountSettled")
    private Boolean rSAmountSettled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TierPayerId")
    private CfgTier tierPayer;

    @Column(name = "RSSettlementDate")
    private LocalDateTime rSSettlementDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "RSSheetCode")
    private String rSSheetCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "PropositionSheetCode")
    private String propositionSheetCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "SlipCode")
    private String slipCode;

    @Column(name = "SlipDate")
    private LocalDateTime slipDate;

    @Column(name = "IsAccounted")
    private Boolean isAccounted;

    @Column(name = "ConvertValue", precision = 26, scale = 6)
    private BigDecimal convertValue;

    @Column(name = "AmountBaseConverted", precision = 26, scale = 6)
    private BigDecimal amountBaseConverted;

    @Column(name = "AmountStamp", precision = 26, scale = 6)
    private BigDecimal amountStamp;

    @Column(name = "ConvertUnit")
    private Integer convertUnit;

    @Column(name = "BuySettlementId_old")
    private UUID buysettlementidOld;

    /** "Entité" — l'entité juridique (voir {@code EntitesJuridiquesActives}) sur laquelle le règlement a été saisi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    /** "Compte" — le compte bancaire de destination. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlAccountId")
    private StlAccount stlAccount;

    /** "Dépôt" — la catégorie de dépôt (FOURNISSEUR/CLIENT/BANQUE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlDepositId")
    private StlDeposit stlDeposit;
}