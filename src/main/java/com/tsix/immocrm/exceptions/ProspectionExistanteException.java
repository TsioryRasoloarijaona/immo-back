package com.tsix.immocrm.exceptions;

import com.tsix.immocrm.dto.prospection.ProspectionExistanteDto;

/**
 * Une seule prospection ouverte par prospect et par projet → 409 avec {@code prospectionExistante}
 * (FEATURE-doublons-prospect-projet.md §3).
 */
public class ProspectionExistanteException extends ConflictException {

    private final transient ProspectionExistanteDto prospectionExistante;

    public ProspectionExistanteException(String prospectNom, ProspectionExistanteDto existante) {
        super("Le prospect " + prospectNom + " a déjà une prospection ouverte sur le projet "
                + existante.projetCode() + " (" + existante.code() + ") : ouvrez-la plutôt que d'en créer une seconde.");
        this.prospectionExistante = existante;
    }

    public ProspectionExistanteDto getProspectionExistante() {
        return prospectionExistante;
    }
}
