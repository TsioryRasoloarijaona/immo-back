package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Rapport déclaré dans OMRAN, en lecture ; {@code ReportFile} (modèle Stimulsoft, centaines de Ko) jamais mappé. */
@Getter
@Setter
@Entity
@Table(name = "AdmReportData")
public class AdmReportData extends CodeLibelleOmran {

    @Size(max = 1000)
    @Nationalized
    @Column(name = "\"Type\"")
    private String type;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "GroupName")
    private String groupName;
}
