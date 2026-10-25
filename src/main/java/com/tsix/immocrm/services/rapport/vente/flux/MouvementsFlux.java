package com.tsix.immocrm.services.rapport.vente.flux;

import java.util.List;

/** Sortie de {@link FluxVenteMensuel#mouvements} : lignes datées au jour, avant agrégation, et anomalies du calcul. */
public record MouvementsFlux(List<MouvementFlux> lignes, AnomaliesFlux anomalies) {

    public MouvementsFlux {
        lignes = List.copyOf(lignes);
    }
}
