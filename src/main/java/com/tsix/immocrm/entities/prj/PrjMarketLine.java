package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne du bordereau définitif d'un lot ; ses totaux ne sont pas stockés (colonnes de vue). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLine")
public class PrjMarketLine extends AuditOmran {
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
}
