package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;

import java.util.List;
import java.util.Objects;

/** Rapport du module achat (FEATURE-n7-etats-achat.md N7b) : une section, sans avertissement. */
abstract class RapportAchat implements Rapport {

    @Override
    public ModuleRapport module() {
        return ModuleRapport.ACHAT;
    }

    static ResultatRapport resultat(SectionRapport section) {
        return new ResultatRapport(List.of(section), List.of());
    }

    static int annee(ContexteRapport contexte) {
        return Objects.requireNonNull(contexte.annee(LignesMensuelles.ANNEE), "Paramètre annee absent");
    }
}
