package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne du bordereau estimatif (« estimation confidentielle ») d'un lot ; seule sa somme par lot est lue. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLineEstimation")
public class PrjMarketLineEstimation extends AuditOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "UnitPriceTTC", precision = 26, scale = 6)
    private BigDecimal unitPriceTTC;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "IsStock")
    private Boolean isStock;
}
