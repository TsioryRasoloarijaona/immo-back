package com.tsix.immocrm.exceptions;

import com.tsix.immocrm.dto.prospection.ProspectionExistanteDto;

/**
 * Un prospect a déjà une prospection <b>ouverte</b> sur le projet visé : on n'en crée pas une
 * seconde, on ajoute des dossiers à celle qui existe. 409 avec la prospection en propriété
 * {@code prospectionExistante} — voir {@code instructions/FEATURE-doublons-prospect-projet.md} §3.
 */
public class ProspectionExistanteException extends ConflictException {

    private final ProspectionExistanteDto prospectionExistante;

    public ProspectionExistanteException(String prospectNom, ProspectionExistanteDto existante) {
        super("Le prospect " + prospectNom + " a déjà une prospection ouverte sur le projet "
                + existante.projetCode() + " (" + existante.code() + ") : ouvrez-la plutôt que d'en créer une seconde.");
        this.prospectionExistante = existante;
    }

    public ProspectionExistanteDto getProspectionExistante() {
        return prospectionExistante;
    }
}
