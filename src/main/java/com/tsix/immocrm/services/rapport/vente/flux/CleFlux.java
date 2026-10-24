package com.tsix.immocrm.services.rapport.vente.flux;

import java.util.UUID;

/** Cellule du flux : tranche × année × mois (1–12). */
public record CleFlux(UUID trancheId, int annee, int mois) {
}
