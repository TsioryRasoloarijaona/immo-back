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

/** Ligne de décompte : {@code Quantity} = période, {@code QuantityOld} = cumul antérieur. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjWorkingOutLine")
public class PrjWorkingOutLine extends LigneOmran {

    // « Pkey » (k minuscule) sur cette table.
    @Id
    @Column(name = "Pkey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjWorkingOutId")
    private PrjWorkingOut prjWorkingOut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketLineId")
    private PrjMarketLine prjMarketLine;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "QuantityOrigin", precision = 26, scale = 6)
    private BigDecimal quantityOrigin;

    @Column(name = "Coefficient", precision = 26, scale = 6)
    private BigDecimal coefficient;

    @Column(name = "QuantityOld", precision = 26, scale = 6)
    private BigDecimal quantityOld;
}
