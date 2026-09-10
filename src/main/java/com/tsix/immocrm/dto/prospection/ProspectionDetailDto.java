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

        // -- Bloc « qualification budgétaire » : MESURÉ MORT sur les 14 114 prospections réelles.
        //    Price est renseigné sur 4 lignes et vaut 0 — jamais null — partout ailleurs ; Area,
        //    AreaTo, PriceTo, AdvanceAmount et les trois financements : 0 ligne. Les champs sont
        //    conservés pour ne pas rompre le contrat existant, mais le front ne doit pas les
        //    afficher : ils annonceraient un budget de « 0,00 DH » sur tout le portefeuille.
        //    Retirés de la sélection par défaut de la liste (ColonneProspection).
        //    Voir instructions/INVENTAIRE-colonnes-prospection.md §1.
        @Schema(description = "Surface recherchée. **Mesurée morte : 0 ligne renseignée sur "
                + "14 114.** Conservée pour compatibilité — ne pas afficher.")
        BigDecimal surface,

        @Schema(description = "Borne haute de surface. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal surfaceJusqua,

        @Schema(description = "Budget déclaré. **Mesuré mort : 4 lignes sur 14 114**, et `0` — "
                + "jamais `null` — ailleurs. Conservé pour compatibilité ; ne pas afficher.")
        BigDecimal prix,

        @Schema(description = "Borne haute de budget. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal prixJusqua,

        @Schema(description = "Avance envisagée. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal avance,

        @Schema(description = "Financement sur fonds propres. **Mort : 0 ligne.** Ne pas afficher.")
        BigDecimal financementFondsPropres,

        @Schema(description = "Financement par crédit. **Mort : 0 ligne.** Ne pas afficher.")
        BigDecimal financementCredit,

        @Schema(description = "Autre financement. **Mort : 0 ligne.** Ne pas afficher.")
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

        // -- Type de document du légataire (StrEntity) : le discriminant réel entre prospection
        //    de vente, pré-prospection et prospection de location.
        //    Voir instructions/FEATURE-strentity-prospection.md §3.1.
        @Schema(description = "Identifiant du type de document légataire (`StrEntity`). `null` sur "
                + "les 4 prospections dont `StrEntityId` n'est pas renseigné.")
        UUID typeProspectionId,

        @Schema(description = "Code du type de document — **c'est ce champ qu'il faut tester**, "
                + "jamais le libellé. Valeurs observées : `ComProspection` (14 114 lignes), "
                + "`ComPreProspection` (3 817), `RntProspection` (4, prospection de location).\n\n"
                + "`null` signifie que la prospection ne porte aucun type : elle est alors "
                + "**invisible dans le logiciel de bureau**, dont toutes les vues font un "
                + "`INNER JOIN` sur `StrEntity`. Ce n'est pas une donnée manquante mais une "
                + "anomalie de création — 4 lignes en base réelle.",
                example = "ComProspection")
        String typeProspectionCode,

        @Schema(description = "Libellé d'affichage du type, traduit — « Prospection vente », "
                + "« Pré-Prospection ». Ne pas s'en servir pour tester le type.",
                example = "Prospection vente")
        String typeProspection,

        @Schema(description = "Bloc d'indicateurs d'activité calculés par la base légataire "
                + "(nombre d'interactions, inactivité, dernière communication, visites, relance "
                + "en cours, projet, et le lien vers le dossier issu de la prospection).\n\n"
                + "**Vaut `null` en entier sur une pré-prospection** : ces indicateurs viennent "
                + "de `ComSaleProspectionView`, qui exclut les pré-prospections. L'absence du "
                + "bloc signifie « non disponible », jamais « zéro ».")
        ActiviteProspectionDto activite
) {
}
