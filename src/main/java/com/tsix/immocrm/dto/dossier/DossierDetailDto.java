package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/dossiers/{id}}, including {@code etapesAtteignables}
 * (computed from {@link com.tsix.immocrm.domain.TransitionsValidees}, never stored) and the
 * dossier's linked articles (plural — confirmed a Dossier can have several).
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComFolder}. Adjust once a spec is available.
 */
public record DossierDetailDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        List<String> etapesAtteignables,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        UUID avocatId,
        String avocatNom,
        List<ArticleSummaryDto> articles,
        BigDecimal prixAvantRemise,
        BigDecimal remise,
        BigDecimal remiseMontant,
        BigDecimal prixApresRemise,
        BigDecimal prix,
        BigDecimal montantAvance,
        BigDecimal totalAPayer,
        String notes,
        BigDecimal probabiliteFinalisation,
        String modePaie,
        @Schema(description = "Identifiant de la prospection à l'origine du dossier (ComFolder.ComProspectionId).")
        UUID prospectionId,
        @Schema(description = "Code de la prospection à l'origine du dossier — pour la référence cascade et le lien cliquable vers la prospection depuis le dossier (`GET /api/prospections/{prospectionId}`).")
        String prospectionCode,

        // ------------------------------------------------------------------
        // Contexte patrimonial — voir instructions/FEATURE-contexte-dossier.md §2.1.
        // Le dossier servait son article sans jamais dire OÙ il se trouve.
        // Chaîne : article → tranche → projet → société, vérifiée à 358/358.
        // ------------------------------------------------------------------

        @Schema(description = "Identifiant du projet. 100 %. **Provient de la tranche de "
                + "l'article, pas de l'article** : `StkItem.CfgProjectId` est `null` sur les 358 "
                + "articles de dossier réels.")
        UUID projetId,

        @Schema(description = "Code du projet. 100 %.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. 100 %.")
        String projetLibelle,

        @Schema(description = "Identifiant de la tranche de l'article principal. 100 %.")
        UUID trancheId,

        @Schema(description = "Code de la tranche. 100 %.")
        String trancheCode,

        @Schema(description = "Libellé de la tranche. 100 %.")
        String trancheLibelle,

        @Schema(description = "Identifiant de la société / entité juridique. 100 %.")
        UUID societeId,

        @Schema(description = "Code de la société. 100 %.", example = "ADMOD")
        String societeCode,

        @Schema(description = "Libellé de la société. 100 %.")
        String societeLibelle,

        @Schema(description = "Identifiant de la vocation du dossier. 100 %.")
        UUID vocationId,

        @Schema(description = "Code de la vocation. 100 %.")
        String vocationCode,

        @Schema(description = "Libellé de la vocation. 100 %.")
        String vocationLibelle,

        // ------------------------------------------------------------------
        // Piste d'audit — §2.3. Colonnes déjà mappées sur ComFolder.
        // ------------------------------------------------------------------

        @Schema(description = "Auteur de la création du dossier. 100 % des 358 dossiers réels.")
        String creePar,

        @Schema(description = "Date de création. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 100 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 100 %.")
        LocalDateTime modifieLe,

        // ------------------------------------------------------------------
        // Origine — §2.4. Le dossier ne renvoyait que l'id et le code de la prospection.
        // ------------------------------------------------------------------

        @Schema(description = "Étape de la prospection d'origine. `null` sur les 3 dossiers (sur "
                + "358) qui n'ont pas de prospection — cas réel, pas une anomalie.")
        String etapeProspection,

        @Schema(description = "Origine de la prospection. 99,2 %.", example = "FACEBOOK")
        String origineProspection,

        @Schema(description = "Date de la prospection d'origine. 99,2 %.")
        LocalDateTime dateProspection
) {
}
