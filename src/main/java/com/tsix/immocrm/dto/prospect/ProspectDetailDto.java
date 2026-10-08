package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProspectDetailDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        /** Préremplit le sélecteur en édition, sinon chaque réenregistrement ferait rechoisir le pays. */
        String indicatifTelephone,
        String telephone1,
        String telephone2,
        String mobile,
        /** Indicatif pays du mobile, même usage que {@link #indicatifTelephone}. */
        String indicatifMobile,
        String fax,
        String email,
        String siteWeb,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        LocalDateTime dateNaissance,
        String lieuNaissance,
        Integer nombreEnfants,
        String profession,
        String notes,
        String ice,
        Boolean estAncienClient,
        UUID typeId,
        String type,
        UUID kindId,
        String kind,
        UUID commercialId,
        String commercialNom,
        UUID titreId,
        String titre,
        UUID sexeId,
        String sexe,
        UUID paysResidenceId,
        String paysResidence,
        UUID paysId,
        String pays,
        UUID nationaliteId,
        String nationalite,
        UUID situationFamilialeId,
        String situationFamiliale,
        UUID typeResidenceId,
        String typeResidence,
        UUID regimeMatrimonialId,
        String regimeMatrimonial,
        UUID professionId,
        UUID salaireId,
        String salaire,
        String employeur,
        Integer anciennete,
        String filsDe,
        String etDe,
        PieceIdentiteDto pieceIdentite,

        // Piste d'audit et tranches d'intérêt : voir FEATURE-audit-tranches-prospect.md.

        String creePar,

        @Schema(description = "Date de création de la fiche. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 99,9 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 99,9 %.")
        LocalDateTime modifieLe,

        List<TrancheInteretDto> tranchesInteret
) {
}
