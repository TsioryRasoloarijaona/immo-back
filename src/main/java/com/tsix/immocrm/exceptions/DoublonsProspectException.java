package com.tsix.immocrm.exceptions;

import com.tsix.immocrm.dto.prospect.ProspectDoublonDto;

import java.util.List;

/**
 * Fiches de même nom, téléphone ou email sans confirmation {@code ignorerDoublons} → 409 avec {@code doublons}
 * (FEATURE-doublons-prospect-projet.md §2).
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
