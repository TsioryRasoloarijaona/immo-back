package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/prospections/{id}}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComProspection}. Adjust once a spec is available.
 */
public record ProspectionDetailDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String tempDescription,
        String tempTel,
        String tempEmail,
        UUID prospectId,
        String prospectNom,
        UUID commercialId,
        String commercialNom,
        UUID proposeurId,
        String proposeurNom,
        UUID origineId,
        String origine,
        UUID origineSupportId,
        String origineSupport,
        UUID etapeId,
        String etape,
        UUID vocationId,
        String vocation,
        UUID typeContactId,
        String typeContact,
        UUID actionCommercialeId,
        String actionCommerciale,
        String notes,
        BigDecimal surface,
        BigDecimal surfaceJusqua,
        BigDecimal prix,
        BigDecimal prixJusqua,
        BigDecimal avance,
        BigDecimal financementFondsPropres,
        BigDecimal financementCredit,
        BigDecimal financementAutre,
        Boolean fermee,
        LocalDateTime dateFermeture,
        UUID raisonFermetureId,
        String raisonFermeture,
        String raisonFermetureTexte,
        Boolean ancienClient,
        // -- Tranche principale et article principal (ComProspection.StkHierarchyId/StkItemId).
        // Toujours cohérents entre eux quand articleId est renseigné (l'article appartient
        // forcément à trancheId) — garanti par PATCH /api/prospections/{id}/tranche-principale,
        // le seul endpoint qui les modifie. Ne pas confondre avec les "autres tranches"
        // (GET/POST /api/prospections/{id}/autres-tranches) qui sont des intérêts secondaires
        // indépendants, sur d'autres tranches/articles.
        UUID trancheId,
        String trancheCode,
        String trancheDescription,
        UUID articleId,
        String articleCode,
        String articleDescription,

        @Schema(description = "Bloc d'indicateurs d'activité calculés par la base légataire "
                + "(nombre d'interactions, inactivité, dernière communication, visites, relance "
                + "en cours, projet, et le lien vers le dossier issu de la prospection).\n\n"
                + "**Vaut `null` en entier sur une pré-prospection** : ces indicateurs viennent "
                + "de `ComSaleProspectionView`, qui exclut les pré-prospections. L'absence du "
                + "bloc signifie « non disponible », jamais « zéro ».")
        ActiviteProspectionDto activite
) {
}
