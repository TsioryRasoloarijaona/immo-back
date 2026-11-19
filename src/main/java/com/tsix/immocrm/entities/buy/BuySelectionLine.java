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
import java.util.UUID;

/** Ligne du bordereau d'une demande interne d'achat ; taux en fraction (0,2 = 20 %). */
@Getter
@Setter
@Entity
@Table(name = "BuySelectionLine")
public class BuySelectionLine extends AuditOmran {
    @Column(name = "BuySelectionId")
    private UUID buySelectionId;

    @Column(name = "BuySelectionGroupId")
    private UUID buySelectionGroupId;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "BuyItemId")
    private UUID buyItemId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "UnitPriceTTC", precision = 26, scale = 6)
    private BigDecimal unitPriceTTC;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "FODECRatio", precision = 26, scale = 16)
    private BigDecimal fodecRatio;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
