package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Groupe du bordereau d'une demande interne d'achat ; {@code ParentId} = groupe parent de la même demande, sans FK. */
@Getter
@Setter
@Entity
@Table(name = "BuySelectionGroup")
public class BuySelectionGroup extends AuditOmran {
    @Column(name = "BuySelectionId")
    private UUID buySelectionId;

    @Column(name = "ParentId")
    private UUID parentId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
