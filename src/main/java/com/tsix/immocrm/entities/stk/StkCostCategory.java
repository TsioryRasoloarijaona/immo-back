package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Catégorie de charge d'un article. */
@Entity
@Table(name = "StkCostCategory")
public class StkCostCategory extends AuditOmran {
}
