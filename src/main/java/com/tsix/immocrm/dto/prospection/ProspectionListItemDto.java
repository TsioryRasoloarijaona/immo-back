package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/prospections} and {@code GET /api/pre-prospections}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComProspection}. Adjust once a spec is available.
 */
public record ProspectionListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String tempDescription,
        UUID prospectId,
        String prospectNom,
        UUID commercialId,
        String commercialNom,
        String origine,
        String etape,
        Boolean fermee,
        BigDecimal prix,
        BigDecimal surface,
        UUID trancheId,
        // -- Libellé de la tranche principale (CfgTranche.Code/Description), pour affichage sans
        // second appel à GET /api/tranches/{trancheId}.
        String trancheCode,
        String trancheDescription,

        // -- Contact et relance, pour qu'un commercial puisse appeler depuis la liste sans
        // ouvrir chaque fiche. Voir instructions/FEATURE-prospection-contact-alerte.md.
        @Schema(description = "Téléphone du prospect : celui de sa fiche (mobile, à défaut fixe) "
                + "et, à défaut, le numéro saisi lors du premier contact. Le repli n'est pas "
                + "décoratif — mesuré sur les 8 613 prospections ouvertes : 5 034 joignables par "
                + "la fiche, 7 596 par le champ de premier contact, **8 591 par l'une ou "
                + "l'autre**.",
                example = "0612345678")
        String telephone,

        @Schema(description = "Email du prospect, même règle de repli que `telephone`. Renseigné "
                + "sur 4 752 des 8 613 prospections ouvertes (55 %) — `null` est donc un cas "
                + "courant, pas une anomalie.",
                example = "client@exemple.ma")
        String email,

        @Schema(description = "Date de validité de la dernière alerte de relance posée sur cette "
                + "prospection, `null` s'il n'y en a aucune (3 236 des 8 613 ouvertes en ont "
                + "une). L'alerte provient toujours d'une **interaction** : la colonne d'alerte "
                + "portée par la prospection elle-même est morte en base — 0 ligne sur 8 613.")
        LocalDateTime derniereAlerteDate,

        @Schema(description = "Message de cette dernière alerte — en pratique la note du "
                + "commercial sur l'interaction qui l'a posée. `null` si aucune alerte.",
                example = "rappeler la cliente lundi matin")
        String derniereAlerteMessage
) {
}
