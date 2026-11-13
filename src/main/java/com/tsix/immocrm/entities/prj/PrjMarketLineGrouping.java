package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Regroupement des lignes de bordereau d'un lot (étage, appartement…). */
@Entity
@Table(name = "PrjMarketLineGrouping")
public class PrjMarketLineGrouping extends GroupeBordereauOmran {
}
