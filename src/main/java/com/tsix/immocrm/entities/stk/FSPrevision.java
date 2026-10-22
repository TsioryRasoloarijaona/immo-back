package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne du bilan d'une étude, clé fonctionnelle (étude, catégorie) ; montants PROPRES (hors enfants). */
@Getter
@Setter
@Entity
@Table(name = "FSPrevision")
public class FSPrevision extends LignePrevisionOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "Note")
    private String note;

    @Column(name = "GlobalRatio", precision = 26, scale = 6)
    private BigDecimal globalRatio;

    // Ligne d'unité de l'étude (StkFSBaseUnitStkFeasibilityStudy.PKey), sans clé étrangère en base.
    @Column(name = "UnityId")
    private UUID unityId;

    // Catégorie de la ligne de référence (mode pourcentage), pas le parent d'arbre.
    @Column(name = "ParentId")
    private UUID parentId;

    @Column(name = "ApplyOnDate")
    private LocalDateTime applyOnDate;

    @Column(name = "IsForProject")
    private Boolean isForProject;

    @Column(name = "PrjMarketPrevisionModelId")
    private UUID prjMarketPrevisionModelId;
}
