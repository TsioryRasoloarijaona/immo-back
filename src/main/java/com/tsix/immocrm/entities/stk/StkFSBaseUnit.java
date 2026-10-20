package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Unité de base des études de faisabilité : Code / Description en varchar et aucune colonne d'audit (≠ CodeLibelleOmran). */
@Getter
@Setter
@Entity
@Table(name = "StkFSBaseUnit")
public class StkFSBaseUnit {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Column(name = "Description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;

    @Column(name = "IsArea")
    private Boolean isArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SpecificFunctionId")
    private SpecificFunction specificFunction;
}
