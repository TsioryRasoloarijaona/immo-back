package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Ligne du bordereau estimatif (« estimation confidentielle ») d'un lot. */
@Entity
@Immutable
@Table(name = "PrjMarketLineEstimation")
public class PrjMarketLineEstimation extends LigneBordereauOmran {
}
