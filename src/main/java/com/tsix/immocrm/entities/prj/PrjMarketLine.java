package com.tsix.immocrm.entities.prj;

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
import java.util.UUID;

/** Ligne du bordereau définitif ; les totaux HT/TTC d'une ligne ne sont pas stockés (colonnes de vue). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLine")
public class PrjMarketLine {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketId")
    private PrjMarket prjMarket;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    // decimal(26,16) en base, contrairement aux autres prix.
    @Column(name = "UnitPriceHT", precision = 26, scale = 16)
    private BigDecimal unitPriceHT;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "UnitPriceTTC", precision = 26, scale = 6)
    private BigDecimal unitPriceTTC;

    @Column(name = "IsStock")
    private Boolean isStock;

    // Sans FK en base : id brut, jointure JPQL sur l'égalité d'id.
    @Column(name = "PrjMarketLineGroupingId")
    private UUID prjMarketLineGroupingId;
}
