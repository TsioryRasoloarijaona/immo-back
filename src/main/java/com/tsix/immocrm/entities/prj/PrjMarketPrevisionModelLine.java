package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne d'un modèle de simulation lot : fractions de temps, d'avancement et de paiement. */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketPrevisionModelLine")
public class PrjMarketPrevisionModelLine {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "PointTime", precision = 26, scale = 6)
    private BigDecimal pointTime;

    @Column(name = "PointAchivement", precision = 26, scale = 6)
    private BigDecimal pointAchivement;

    @Column(name = "PointPayment", precision = 26, scale = 6)
    private BigDecimal pointPayment;

    @Column(name = "PrjMarketPrevisionModelId")
    private UUID prjMarketPrevisionModelId;
}
