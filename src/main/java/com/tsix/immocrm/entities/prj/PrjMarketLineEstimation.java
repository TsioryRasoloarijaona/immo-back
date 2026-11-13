package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Ligne du bordereau estimatif (« estimation confidentielle ») d'un lot. */
@Entity
@Table(name = "PrjMarketLineEstimation")
public class PrjMarketLineEstimation extends LigneBordereauOmran {
}
