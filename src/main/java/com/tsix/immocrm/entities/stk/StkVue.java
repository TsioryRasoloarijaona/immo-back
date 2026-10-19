package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Vue d'un article (mer, jardin…), cases de l'onglet Autres de la fiche article. */
@Entity
@Table(name = "StkVue")
public class StkVue extends AuditOmran {
}
