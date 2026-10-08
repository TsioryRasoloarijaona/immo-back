package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mise à jour partielle : seuls les champs non null sont appliqués. */
public record ModifierProspectRequest(
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        /** Requis dès que {@code telephone} est fourni, jamais repris de la base : le pays peut changer. */
        String indicatifTelephone,
        String mobile,
        /** Indicatif pays du mobile, mêmes règles que {@link #indicatifTelephone}. */
        String indicatifMobile,
        String email,
        String adresse,
        String ville,
        String codePostal,
        LocalDateTime dateNaissance,
        String lieuNaissance,
        Integer nombreEnfants,
        UUID typeId,
        UUID kindId,
        UUID commercialId,
        UUID titreId,
        UUID sexeId,
        UUID paysResidenceId,
        UUID paysId,
        UUID nationaliteId,
        UUID situationFamilialeId,
        UUID typeResidenceId,
        UUID regimeMatrimonialId,
        UUID professionId,
        UUID salaireId,
        String employeur,
        Integer anciennete,
        String filsDe,
        String etDe,
        String adresse2,
        String adresse3,
        PieceIdentiteRequest pieceIdentite
) {
}
