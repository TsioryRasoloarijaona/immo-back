package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Regroupement des lignes du bordereau estimatif d'un lot. */
@Entity
@Table(name = "PrjMarketLineGroupingEstimation")
public class PrjMarketLineGroupingEstimation extends GroupeBordereauOmran {
}
