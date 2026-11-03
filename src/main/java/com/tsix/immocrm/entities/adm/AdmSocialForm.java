package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Sorting et les colonnes d'audit existent en prod, pas dans yesRemsTest : non mappés, on ne les lit pas.
@Getter
@Setter
@Entity
@Table(name = "AdmSocialForm")
public class AdmSocialForm extends CodeLibelleOmran {
    @Column(name = "IsPerson")
    private Boolean isPerson;
}
