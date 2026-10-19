package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Rubrique d'une tranche. */
@Getter
@Setter
@Entity
@Table(name = "StkTrancheHeading")
public class StkTrancheHeading extends CodeLibelleOmran {
    @Column(name = "Sorting")
    private Integer sorting;
}
