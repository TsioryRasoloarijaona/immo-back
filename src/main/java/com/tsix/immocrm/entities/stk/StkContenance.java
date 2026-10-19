package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Contenance (chambre, salon…) et ses trois options : option vide = grisée, quantité seulement si {@code IsOptionNWithQuantity}. */
@Getter
@Setter
@Entity
@Table(name = "StkContenance")
public class StkContenance extends AuditOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Option1")
    private String option1;

    @Column(name = "IsOption1WithQuantity")
    private Boolean isOption1WithQuantity;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Option2")
    private String option2;

    @Column(name = "IsOption2WithQuantity")
    private Boolean isOption2WithQuantity;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Option3")
    private String option3;

    @Column(name = "IsOption3WithQuantity")
    private Boolean isOption3WithQuantity;
}
