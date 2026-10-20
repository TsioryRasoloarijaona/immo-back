package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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

/**
 * Désistement d'un dossier de vente, sans colonne statut : validé = {@code ValidityDate} posée, légalisé =
 * {@code LegalizationDate} posée. {@code TransfertExpenseAmount} est un <b>taux</b> malgré son nom.
 */
@Getter
@Setter
@Entity
@Table(name = "ComSaleWithdrawalCancellation")
public class ComSaleWithdrawalCancellation {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComSaleId")
    private ComFolder comFolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgOldCustomerId")
    private CfgTier cfgOldCustomer;

    @Column(name = "TotalToPay", precision = 26, scale = 6)
    private BigDecimal totalToPay;

    @Column(name = "TotalSettled", precision = 26, scale = 6)
    private BigDecimal totalSettled;

    @Column(name = "TotalRest", precision = 26, scale = 6)
    private BigDecimal totalRest;

    // « Reste frais à régler » chez OMRAN, malgré son nom.
    @Column(name = "NewStkItemTotalToPay", precision = 26, scale = 6)
    private BigDecimal newStkItemTotalToPay;

    @Column(name = "TransfertExpenseAmount", precision = 26, scale = 6)
    private BigDecimal transfertExpenseAmount;

    @Column(name = "Discount", precision = 26, scale = 6)
    private BigDecimal discount;

    @Column(name = "TransfertExpenseAmountAfterDiscount", precision = 26, scale = 6)
    private BigDecimal transfertExpenseAmountAfterDiscount;

    @Column(name = "ReimburseAmount", precision = 26, scale = 6)
    private BigDecimal reimburseAmount;

    @Column(name = "ClaimDate")
    private LocalDateTime claimDate;

    @Column(name = "DecisionDate")
    private LocalDateTime decisionDate;

    @Column(name = "ValidityDate")
    private LocalDateTime validityDate;

    @Column(name = "IsLegalized")
    private Boolean isLegalized;

    @Column(name = "LegalizationDate")
    private LocalDateTime legalizationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComSaleWithdrawalReasonId")
    private ComSaleWithdrawalReason motif;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlSettlementTypeId")
    private StlSettlementType stlSettlementType;

    // varchar, pas nvarchar.
    @Size(max = 255)
    @Column(name = "Reference")
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgBankId")
    private CfgTier cfgBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgContactId")
    private CfgTier cfgContact;

    @Column(name = "IsAccounted")
    private Boolean isAccounted;

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

    public boolean estValide() {
        return validityDate != null;
    }

    public boolean estLegalise() {
        return legalizationDate != null;
    }
}
