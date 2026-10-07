package com.tsix.immocrm.dto.prospection;

import java.time.LocalDate;

/**
 * Données du PDF « NDA AYAD PRIVATE » ({@code instructions/templates/NDA AYAD PRIVATE.pdf}) ; le
 * texte fixe du formulaire est dans {@code NdaPdfRenderer}. Pas exposé en JSON.
 */
public record NdaData(
        String nom,
        String prenom,
        String nationalite,
        String profession,
        LocalDate dateNaissance,
        String lieuNaissance,
        /** "M"/"F" (AdmSexType.Code) : coche la case « Male »/« Female » du formulaire. */
        String sexe,
        String adresse,
        String codePostal,
        String ville,
        String pays,
        String telephone,
        String telephoneAlternatif,
        String email,
        LocalDate dateEdition
) {
}
