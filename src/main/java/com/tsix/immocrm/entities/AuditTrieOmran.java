package com.tsix.immocrm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/** {@link AuditOmran} plus l'ordre d'affichage OMRAN {@code Sorting}. */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditTrieOmran extends AuditOmran {
    @Column(name = "Sorting")
    private Integer sorting;
}
