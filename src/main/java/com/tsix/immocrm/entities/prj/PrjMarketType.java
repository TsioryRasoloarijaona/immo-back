package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Type de marché, choisi par un type de frais. */
@Entity
@Table(name = "PrjMarketType")
public class PrjMarketType extends CodeLibelleOmran {
}
