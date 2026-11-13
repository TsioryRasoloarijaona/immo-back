package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Ligne du bordereau définitif d'un lot ; ses totaux ne sont pas stockés (colonnes de vue). */
@Entity
@Table(name = "PrjMarketLine")
public class PrjMarketLine extends LigneBordereauOmran {
}
