package com.tsix.immocrm.entities.act;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Compte du plan comptable (« Plan compt. » de la tranche). */
@Entity
@Table(name = "ActAccount")
public class ActAccount extends CodeLibelleOmran {
}
