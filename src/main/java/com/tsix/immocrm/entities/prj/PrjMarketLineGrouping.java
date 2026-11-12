package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Regroupement des lignes de bordereau d'un lot (étage, appartement…). */
@Entity
@Immutable
@Table(name = "PrjMarketLineGrouping")
public class PrjMarketLineGrouping extends GroupeBordereauOmran {
}
