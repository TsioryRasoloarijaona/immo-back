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

/** Travail supplémentaire d'un décompte ; clé {@code Pkey} (k minuscule), Description en nvarchar(500). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjWorkingOutLineExtra")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
@AttributeOverride(name = "description", column = @Column(name = "Description", length = 500))
public class PrjWorkingOutLineExtra extends AuditOmran {
    @Column(name = "PrjWorkingOutId")
    private UUID prjWorkingOutId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
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
}
