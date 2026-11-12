package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Retenue d'un décompte. */
@Getter
@Setter
@Entity
@Table(name = "PrjWorkingOutRetainedLine")
public class PrjWorkingOutRetainedLine {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "PrjWorkingOutId", nullable = false)
    private UUID prjWorkingOutId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;
}
