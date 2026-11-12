package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Colonnes communes aux lignes des bordereaux définitif ({@code PrjMarketLine}) et estimatif ({@code PrjMarketLineEstimation}). */
@Getter
@Setter
@MappedSuperclass
public abstract class LigneBordereauOmran extends AuditOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    // Aucune FK en base : joindre sur l'égalité d'id.
    @Column(name = "PrjMarketLineGroupingId")
    private UUID prjMarketLineGroupingId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 16)
    private BigDecimal unitPriceHT;

    @Column(name = "UnitPriceTTC", precision = 26, scale = 6)
    private BigDecimal unitPriceTTC;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "IsStock")
    private Boolean isStock;

    @Column(name = "FODECRatio", precision = 26, scale = 16)
    private BigDecimal fodecRatio;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "WeightingRatio", precision = 26, scale = 6)
    private BigDecimal weightingRatio;

    @Column(name = "BuyItemId")
    private UUID buyItemId;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;
}
