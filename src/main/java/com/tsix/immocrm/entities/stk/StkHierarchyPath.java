package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Tracé de plan d'un article ou d'un étage ; mappé pour sa suppression avec l'article ({@code SvgPath} non lu). */
@Getter
@Setter
@Entity
@Table(name = "StkHierarchyPath")
public class StkHierarchyPath {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StkItemId")
    private UUID stkItemId;

    @Column(name = "StkFloorId")
    private UUID stkFloorId;
}
