package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Retenue forfaitaire d'un décompte (entre dans {@code nonRefundableRetainer}). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjWorkingOutRetainedLine")
public class PrjWorkingOutRetainedLine {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PrjWorkingOutId", nullable = false)
    private PrjWorkingOut prjWorkingOut;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;
}
