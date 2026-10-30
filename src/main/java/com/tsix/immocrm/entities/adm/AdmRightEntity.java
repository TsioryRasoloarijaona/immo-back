package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/** Arbre des droits, jamais écrit par Arpent. Un code {@link AdmRight} peut y être absent (droit hérité d'OMRAN). */
@Getter
@Entity
@Immutable
@Table(name = "AdmRightEntity")
public class AdmRightEntity extends CodeLibelleOmran {

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "Depth")
    private Integer depth;

    @Column(name = "Kind")
    private String kind;
}
