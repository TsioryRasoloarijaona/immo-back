package com.tsix.immocrm.exceptions;

import com.tsix.immocrm.dto.prospect.ProspectDoublonDto;

import java.util.List;

/**
 * Création de prospect refusée parce que des fiches existantes portent le même nom, téléphone ou
 * email et que l'appelant n'a pas confirmé ({@code ignorerDoublons}). 409 avec la liste en
 * propriété {@code doublons} — voir {@code instructions/FEATURE-doublons-prospect-projet.md} §2.
 */
public class DoublonsProspectException extends ConflictException {

    private final List<ProspectDoublonDto> doublons;

    public DoublonsProspectException(List<ProspectDoublonDto> doublons) {
        super(doublons.size() + (doublons.size() > 1 ? " fiches" : " fiche")
                + " prospect ou client " + (doublons.size() > 1 ? "portent" : "porte")
                + " le même nom, téléphone ou email");
        this.doublons = doublons;
    }

    public List<ProspectDoublonDto> getDoublons() {
        return doublons;
    }
}
