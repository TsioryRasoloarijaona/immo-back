package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Ligne de contenance (d'un article ou d'un type de présentation) : nombre et trois options. */
@Getter
@Setter
@MappedSuperclass
public abstract class LigneContenanceOmran extends LigneOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkContenanceId")
    private StkContenance stkContenance;

    // Prod : « itemsNumber » sur StkItemContenance ; la collation French_CI_AS ignore la casse.
    @Column(name = "ItemsNumber")
    private Integer itemsNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Option1")
    private String option1;

    @Column(name = "IsWithOption1")
    private Boolean isWithOption1;

    @Column(name = "QuantityOption1")
    private Integer quantityOption1;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Option2")
    private String option2;

    @Column(name = "IsWithOption2")
    private Boolean isWithOption2;

    @Column(name = "QuantityOption2")
    private Integer quantityOption2;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Option3")
    private String option3;

    @Column(name = "IsWithOption3")
    private Boolean isWithOption3;

    @Column(name = "QuantityOption3")
    private Integer quantityOption3;
}
