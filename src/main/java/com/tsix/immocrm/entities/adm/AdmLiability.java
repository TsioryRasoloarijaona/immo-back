package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "AdmLiability")
public class AdmLiability extends CodeLibelleOmran {
    @Column(name = "Sorting")
    private Integer sorting;
}
