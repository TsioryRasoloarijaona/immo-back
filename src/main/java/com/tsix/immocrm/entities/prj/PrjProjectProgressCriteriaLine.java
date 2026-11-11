package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Critère d'avancement d'un projet ; {@code Sorting} = « O.D.T. » d'OMRAN. */
@Getter
@Setter
@Entity
@Table(name = "PrjProjectProgressCriteriaLine")
public class PrjProjectProgressCriteriaLine extends LigneOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "PrjProjectId")
    private UUID prjProjectId;
}
