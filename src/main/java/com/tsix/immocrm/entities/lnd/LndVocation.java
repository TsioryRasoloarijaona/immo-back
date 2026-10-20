package com.tsix.immocrm.entities.lnd;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Vocation d'un terrain (prospection de terrains). */
@Entity
@Table(name = "LndVocation")
public class LndVocation extends AuditOmran {
}
