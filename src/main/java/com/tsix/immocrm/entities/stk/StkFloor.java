package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Étage : {@code Code} = libellé court ("RDC"), {@code Description} = forme longue ("Rez de chaussée"). */
@Entity
@Table(name = "StkFloor")
public class StkFloor extends AuditTrieOmran {
}
