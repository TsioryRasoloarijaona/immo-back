package com.tsix.immocrm.entities.buy;

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

/** Article d'achat ; pas de colonne famille : elle se lit sur la sous-famille. {@code CostFinacial*} (sic) en base. */
@Getter
@Setter
@Entity
@Table(name = "BuyItem")
public class BuyItem extends NomenclatureAchat {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuyItemUnderFamilyId")
    private BuyItemUnderFamily buyItemUnderFamily;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "FODECRatio", precision = 26, scale = 6)
    private BigDecimal fodecRatio;

    @Column(name = "CostLogisticHT", precision = 26, scale = 6)
    private BigDecimal costLogisticHT;

    @Column(name = "CostLogisticVatRatio", precision = 26, scale = 6)
    private BigDecimal costLogisticVatRatio;

    @Column(name = "CostIndirectHT", precision = 26, scale = 6)
    private BigDecimal costIndirectHT;

    @Column(name = "CostIndirectVatRatio", precision = 26, scale = 6)
    private BigDecimal costIndirectVatRatio;

    @Column(name = "CostFinacialHT", precision = 26, scale = 6)
    private BigDecimal costFinacialHT;

    @Column(name = "CostFinacialVatRatio", precision = 26, scale = 6)
    private BigDecimal costFinacialVatRatio;

    @Column(name = "SaleUnitPriceHT", precision = 26, scale = 6)
    private BigDecimal saleUnitPriceHT;

    @Column(name = "SaleVatRatio", precision = 26, scale = 6)
    private BigDecimal saleVatRatio;
}
