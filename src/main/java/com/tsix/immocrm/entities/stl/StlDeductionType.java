package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Type de retenue à la source (lecture seule). */
@Getter
@Setter
@Entity
@Table(name = "StlDeductionType")
public class StlDeductionType extends CodeLibelleOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "RASYear")
    private Integer rasYear;

    @Column(name = "RASValue", precision = 26, scale = 6)
    private BigDecimal rasValue;
}
