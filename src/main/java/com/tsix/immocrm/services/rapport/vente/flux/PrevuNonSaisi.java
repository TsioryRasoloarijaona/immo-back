package com.tsix.immocrm.services.rapport.vente.flux;

import org.springframework.stereotype.Component;

import java.util.List;

/** {@code ComPrevision} et {@code CfgPrevision} sont vides en prod (décision Q3) : prévu = 0, signalé « non saisi ». */
@Component
class PrevuNonSaisi implements SourcePrevu {

    private static final AnomaliesFlux NON_SAISI = new AnomaliesFlux(0, null, List.of(), true);

    @Override
    public FluxMensuel prevu(DemandeFlux demande) {
        return FluxMensuel.vide(NON_SAISI);
    }
}
