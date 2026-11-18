package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Statut de réserve d'une décision ; la colonne réelle s'écrit {@code IsColsed}. */
@Getter
@Setter
@Entity
@Table(name = "BulObservationStatus")
public class BulObservationStatus extends AuditOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "IsColsed")
    private Boolean isClosed;
}
