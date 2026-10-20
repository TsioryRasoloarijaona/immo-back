package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Immeuble : {@code Code} = libellé court des documents ("IMM01"), {@code Description} = forme longue. */
@Entity
@Table(name = "StkBuilding")
public class StkBuilding extends AuditTrieOmran {
}
