package com.tsix.immocrm.services.rapport.vente.flux;

/** Prévisions ({@code PREVU_*}, {@code BASE_*}) d'un flux : FEATURE-tdb-vente-rapports-flux.md §3.6. */
public interface SourcePrevu {

    /** Mesures de famille {@code PREVU} de {@code demande} ; {@code anomalies().prevuNonSaisi()} si rien n'est saisi. */
    FluxMensuel prevu(DemandeFlux demande);
}
