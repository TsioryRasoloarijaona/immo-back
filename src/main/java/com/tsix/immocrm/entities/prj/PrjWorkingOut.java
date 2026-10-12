package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Décompte : montants persistés puis figés par OMRAN, lus tels quels. Plusieurs colonnes sont en
 * minuscule en base ({@code extraAmount}, {@code insuranceDECAmount}…) : casse recopiée.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjWorkingOut")
public class PrjWorkingOut {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketId")
    private PrjMarket prjMarket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuyDocumentId")
    private BuyDocument buyDocument;

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

    @Column(name = "WarrantyAmount", precision = 26, scale = 6)
    private BigDecimal warrantyAmount;

    @Column(name = "WarrantyRatio", precision = 26, scale = 6)
    private BigDecimal warrantyRatio;

    @Column(name = "WorkingOutAmountTTC", precision = 26, scale = 6)
    private BigDecimal workingOutAmountTTC;

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

    @Column(name = "nonRefundableRetainer", precision = 26, scale = 6)
    private BigDecimal nonRefundableRetainer;

    @Column(name = "refundableRetainer", precision = 26, scale = 6)
    private BigDecimal refundableRetainer;

    @Column(name = "extraAmount", precision = 26, scale = 6)
    private BigDecimal extraAmount;

    @Column(name = "amountToBePaid", precision = 26, scale = 6)
    private BigDecimal amountToBePaid;

    @Column(name = "Fluctuations", precision = 26, scale = 6)
    private BigDecimal fluctuations;

    @Column(name = "FluctuationsToPay", precision = 26, scale = 6)
    private BigDecimal fluctuationsToPay;

    @Column(name = "FinalWorkingOut")
    private Boolean finalWorkingOut;

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
}
