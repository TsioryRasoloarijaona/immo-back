package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.AttributeOverride;
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

/** Ligne de décompte ; clé {@code Pkey} (k minuscule) dans cette table. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjWorkingOutLine")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
public class PrjWorkingOutLine extends AuditOmran {
    @Column(name = "PrjWorkingOutId")
    private UUID prjWorkingOutId;

    @Column(name = "PrjMarketLineId")
    private UUID prjMarketLineId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "QuantityOld", precision = 26, scale = 6)
    private BigDecimal quantityOld;

    @Column(name = "Coefficient", precision = 26, scale = 6)
    private BigDecimal coefficient;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;
}
