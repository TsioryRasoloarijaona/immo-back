package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aucune validation bean : aucune colonne CfgTier n'est NOT NULL ; le service exige seulement
 * nom ou raisonSociale.
 */
public record CreerProspectRequest(
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        /** Sans le {@code +} ; requis dès que {@code telephone} est fourni (FEATURE-indicatif-telephone.md §5). */
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
        PieceIdentiteRequest pieceIdentite,
        @Schema(defaultValue = "false")
        Boolean ignorerDoublons
) {
}
