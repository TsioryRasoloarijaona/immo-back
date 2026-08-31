package com.tsix.immocrm.dto.prospection;

import java.time.LocalDate;

/**
 * All fields needed to render the "NDA AYAD PRIVATE" document (PDF) — the "Expression of
 * Interest Form / Non-Disclosure Agreement" a prospect fills at the start of a prospection,
 * modeled on the legacy template ({@code instructions/templates/NDA AYAD PRIVATE.pdf}). The
 * form's boilerplate/letterhead (AYAD Private branding, confidentiality notice, footer) is
 * static — always the same regardless of which project the prospection eventually targets — so
 * it's baked into the renderer rather than sourced from data. Not exposed as JSON — feeds
 * {@code NdaPdfRenderer} only.
 */
public record NdaData(
        String nom,
        String prenom,
        String nationalite,
        String profession,
        LocalDate dateNaissance,
        String lieuNaissance,
        /** "M"/"F" (AdmSexType.Code) — legacy form checks the corresponding "Male"/"Female" box. */
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
