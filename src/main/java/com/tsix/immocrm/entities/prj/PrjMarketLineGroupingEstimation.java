package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Regroupement des lignes du bordereau estimatif d'un lot. */
@Entity
@Immutable
@Table(name = "PrjMarketLineGroupingEstimation")
public class PrjMarketLineGroupingEstimation extends GroupeBordereauOmran {
}
