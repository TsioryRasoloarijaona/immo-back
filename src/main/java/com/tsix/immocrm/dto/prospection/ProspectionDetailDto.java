package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hors {@code activite}, tout vient des associations de {@code ComProspection}, jamais de
 * {@code ComSaleProspectionView} (l'élagage SQL de la fiche en dépend) : ces champs restent donc
 * renseignés sur une pré-prospection. Voir {@code FEATURE-enrichissement-fiche-prospection.md}.
 */
public record ProspectionDetailDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String tempDescription,
        String tempTel,
        String tempEmail,

        // -- Prospect (CfgTier) : les colonnes que la vue légataire affiche sur la fiche.
        UUID prospectId,
        String prospectNom,

        @Schema(description = "Code du prospect dans le référentiel tiers. Renseigné sur "
                + "**100 %** des prospections réelles.", example = "26/14520")
        String prospectCode,

        @Schema(example = "6 61 83 60 90")
        String mobileProspect,

        @Schema(description = "Indicatif pays du mobile, **stocké sans `+`** — 100 %. Composer "
                + "l'affichage côté front (`+212 6 61 83 60 90`).", example = "212")
        String indicatifMobile,

        @Schema(description = "Email porté par la fiche tiers — à distinguer de `tempEmail`, saisi "
                + "sur la prospection elle-même. 59,1 %.")
        String emailFiche,

        @Schema(description = "Identifiant du type de tiers. 100 %.")
        UUID typeProspectId,

        @Schema(example = "PRO")
        String typeProspect,

        @Schema(description = "Libellé du type de tiers. 100 %.")
        String typeProspectLibelle,

        UUID commercialId,
        String commercialNom,

        @Schema(description = "Code du commercial dans le référentiel tiers. 99,3 %.")
        String commercialCode,

        @Schema(description = "Identifiant du proposeur. **Mesuré mort : 1 ligne sur 14 118.** "
                + "Conservé pour compatibilité — ne pas afficher.")
        UUID proposeurId,

        @Schema(description = "Nom du proposeur. **Mort : 1 ligne sur 14 118.** Ne pas afficher.")
        String proposeurNom,

        UUID origineId,
        String origine,

        @Schema(description = "Code de l'origine. 100 %.")
        String origineCode,

        UUID origineSupportId,
        String origineSupport,

        @Schema(description = "Code du support d'origine. 50,2 %.")
        String origineSupportCode,

        UUID etapeId,
        String etape,

        @Schema(description = "Code de l'étape de prospection. 100 %.", example = "3 QUALIFIÉ")
        String etapeCode,

        UUID vocationId,
        String vocation,

        @Schema(description = "Code de la vocation. 100 %.", example = "VENTE")
        String vocationCode,

        UUID typeContactId,
        String typeContact,

        @Schema(description = "Code du type de contact. 100 %.")
        String typeContactCode,

        UUID actionCommercialeId,
        String actionCommerciale,

        @Schema(description = "Code de l'action commerciale (campagne). 40,2 %.")
        String actionCommercialeCode,

        String notes,

        // -- Qualification budgétaire : colonnes mortes dans les données réelles (Price vaut 0, pas
        //    null). Gardées pour le contrat, à ne pas afficher (« 0,00 DH » partout).
        //    Voir instructions/INVENTAIRE-colonnes-prospection.md §1.
        @Schema(description = "Surface recherchée. **Mesurée morte : 0 ligne renseignée sur "
                + "14 114.** Conservée pour compatibilité — ne pas afficher.")
        BigDecimal surface,

        @Schema(description = "Borne haute de surface. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal surfaceJusqua,

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

        @Schema(description = "Date du dernier archivage. **Conservée après désarchivage** — ne pas "
                + "s'en servir pour déduire `fermee`. 62,9 %.")
        LocalDateTime dateFermeture,

        @Schema(description = "Id du motif de clôture (`ComProspectionClosingReason`). Conservé "
                + "après désarchivage.")
        UUID raisonFermetureId,

        @Schema(description = "Libellé du motif de clôture. 60,5 %. Conservé après désarchivage.",
                example = "Hors budget")
        String raisonFermeture,

        @Schema(description = "Code du motif de clôture. 60,5 % — ne vaut quelque chose que sur "
                + "une prospection fermée.")
        String raisonFermetureCode,

        String raisonFermetureTexte,
        Boolean ancienClient,
        // -- Tranche/article principaux (StkHierarchyId/StkItemId), cohérents : seul PATCH
        //    .../tranche-principale les modifie. Distincts des « autres tranches » (intérêts
        //    secondaires, /autres-tranches).
        UUID trancheId,
        String trancheCode,
        String trancheDescription,
        UUID articleId,
        String articleCode,
        String articleDescription,

        String articleCategorie,

        @Schema(description = "Type de l'article principal. Même couverture que la catégorie : "
                + "100 % des prospections portant un article.")
        String articleType,

        // -- Société : dérivée de la tranche (CfgTranche → CfgProject → CfgCompany), exactement
        //    comme le fait la vue légataire. Elle n'est jamais saisie sur la prospection.
        UUID societeId,

        @Schema(description = "Code de la société. 99,8 %.", example = "ADMOD")
        String societeCode,

        String societeLibelle,

        @Schema(description = "Identifiant du pays du prospect. 27,3 %.")
        UUID paysId,

        @Schema(description = "Code du pays. 27,3 %.")
        String paysCode,

        @Schema(description = "Libellé du pays. 27,3 % — `null` sur les deux tiers des fiches.")
        String pays,

        String creePar,

        @Schema(description = "Date de création. 100 % sur les deux populations.")
        LocalDateTime creeLe,

        String modifiePar,

        @Schema(description = "Date de la dernière modification. **100 % des prospections, 28,1 % "
                + "des pré-prospections.**")
        LocalDateTime modifieLe,

        // -- Tri manuel et déduplication (ComProspection.TempField1 / TempField10).
        //    Voir instructions/FEATURE-marqueurs-tri-pre-prospection.md.
        @Schema(example = "salon_+_2_chambres")
        String noteQualification,

        @Schema(allowableValues = {"DOUBLON", "SPAM", "TRAITE"}, example = "DOUBLON")
        String marqueurTri,

        @Schema(example = "25/11847")
        String doublonDeCode,

        UUID doublonDeId,

        @Schema(description = "Nom du tiers dupliqué. `null` dans les mêmes cas que `doublonDeId`.")
        String doublonDeNom,

        // -- Type de document du légataire (StrEntity) : le discriminant réel entre prospection
        //    de vente, pré-prospection et prospection de location.
        //    Voir instructions/FEATURE-strentity-prospection.md §3.1.
        UUID typeProspectionId,

        @Schema(example = "ComProspection")
        String typeProspectionCode,

        @Schema(example = "Prospection vente")
        String typeProspection,

        ActiviteProspectionDto activite
) {
}
