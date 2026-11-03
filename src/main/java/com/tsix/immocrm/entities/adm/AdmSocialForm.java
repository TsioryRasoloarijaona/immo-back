package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Sorting et les colonnes d'audit existent en prod, pas dans yesRemsTest : non mappés, on ne les lit pas.
@Entity
@Table(name = "AdmSocialForm")
public class AdmSocialForm extends CodeLibelleOmran {
}
