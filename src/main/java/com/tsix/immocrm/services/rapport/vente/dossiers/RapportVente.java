package com.tsix.immocrm.services.rapport.vente.dossiers;

import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;

import java.util.List;

/** Rapport du module Vente identifié par son code OMRAN ; paramètre par défaut : les tranches. */
public abstract class RapportVente implements Rapport {

    private final String code;

    protected RapportVente(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.tranches());
    }
}
