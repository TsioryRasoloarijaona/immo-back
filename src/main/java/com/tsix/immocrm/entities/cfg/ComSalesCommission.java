package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Onglet Commission vendeur de la tranche (OMRAN {@code Yes.Cfg.SalesCommission}) ; six blocs base / % / montant. */
@Getter
@Setter
@Entity
@Table(name = "ComSalesCommission")
public class ComSalesCommission extends LigneTrancheOmran {
    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Column(name = "SaleCommissionBasedOnAmount")
    private Boolean saleCommissionBasedOnAmount;

    @Column(name = "SaleCommissionPercent", precision = 26, scale = 6)
    private BigDecimal saleCommissionPercent;

    @Column(name = "SaleCommissionAmount", precision = 26, scale = 16)
    private BigDecimal saleCommissionAmount;

    @Column(name = "SaleCommissionExternBasedOnAmount")
    private Boolean saleCommissionExternBasedOnAmount;

    @Column(name = "SaleCommissionPercentExtern", precision = 26, scale = 6)
    private BigDecimal saleCommissionPercentExtern;

    @Column(name = "SaleCommissionAmountExtern", precision = 26, scale = 16)
    private BigDecimal saleCommissionAmountExtern;

    @Column(name = "DeliveryCommissionBasedOnAmount")
    private Boolean deliveryCommissionBasedOnAmount;

    @Column(name = "DeliveryCommissionPercent", precision = 26, scale = 6)
    private BigDecimal deliveryCommissionPercent;

    @Column(name = "DeliveryCommissionAmount", precision = 26, scale = 16)
    private BigDecimal deliveryCommissionAmount;

    @Column(name = "DeliveryCommissionExternBasedOnAmount")
    private Boolean deliveryCommissionExternBasedOnAmount;

    @Column(name = "DeliveryCommissionPercentExtern", precision = 26, scale = 6)
    private BigDecimal deliveryCommissionPercentExtern;

    @Column(name = "DeliveryCommissionAmountExtern", precision = 26, scale = 16)
    private BigDecimal deliveryCommissionAmountExtern;

    @Column(name = "DeliveryCommission2BasedOnAmount")
    private Boolean deliveryCommission2BasedOnAmount;

    @Column(name = "DeliveryCommissionPercent2", precision = 26, scale = 6)
    private BigDecimal deliveryCommissionPercent2;

    @Column(name = "DeliveryCommissionAmount2", precision = 26, scale = 6)
    private BigDecimal deliveryCommissionAmount2;

    @Column(name = "DeliveryCommissionExtern2BasedOnAmount")
    private Boolean deliveryCommissionExtern2BasedOnAmount;

    @Column(name = "DeliveryCommissionPercentExtern2", precision = 26, scale = 6)
    private BigDecimal deliveryCommissionPercentExtern2;

    @Column(name = "DeliveryCommissionAmountExtern2", precision = 26, scale = 6)
    private BigDecimal deliveryCommissionAmountExtern2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    /** Personnel : un tiers de type Employé, pas une table d'employés. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComEmployeeId")
    private CfgTier comEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionId")
    private ComAction comAction;
}
