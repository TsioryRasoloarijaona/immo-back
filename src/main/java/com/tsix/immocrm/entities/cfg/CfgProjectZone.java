package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.CodeLibelleOmran;
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
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * Zone d'un programme (bouton « Zones » d'OMRAN), arbre par {@code ParentId} sans clé étrangère. Sert au SAV et à la
 * GRH. {@code ImageData} (image) n'est pas mappée : seule sa présence est lue.
 */
@Getter
@Setter
@Entity
@Table(name = "CfgProjectZone")
public class CfgProjectZone extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProjectId")
    private CfgProject cfgProject;

    @Column(name = "ParentId")
    private UUID parentId;

    /** Profondeur : 0 à la racine, parent + 1 sinon (OMRAN {@code ProjectZones}). */
    @Column(name = "Leveling")
    private Integer leveling;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Formula("CASE WHEN ImageData IS NULL THEN 0 ELSE 1 END")
    private Boolean aUneImage;
}
