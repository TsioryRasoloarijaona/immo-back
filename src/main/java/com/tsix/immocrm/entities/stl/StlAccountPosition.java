package com.tsix.immocrm.entities.stl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Rapprochement d'un compte bancaire à une date ({@code PositionDate}) ; le CRM ne fait que le lire. */
@Getter
@Setter
@Entity
@Table(name = "StlAccountPosition")
public class StlAccountPosition {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "PositionDate", nullable = false)
    private LocalDateTime positionDate;

    @NotNull
    @Column(name = "Position", nullable = false, precision = 26, scale = 6)
    private BigDecimal position;

    @NotNull
    @Column(name = "StlAccountId", nullable = false)
    private UUID stlAccountId;
}
