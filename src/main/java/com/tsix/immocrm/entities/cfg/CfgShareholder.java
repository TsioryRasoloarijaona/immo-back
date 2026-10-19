package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Actionnaire (écran OMRAN « ACTIONNAIRES »), cité par les répartitions et les groupes d'actionnaires. */
@Getter
@Setter
@Entity
@Table(name = "CfgShareholder")
public class CfgShareholder extends AuditOmran {

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;
}
