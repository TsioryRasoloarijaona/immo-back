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

        @Schema(description = "Probabilité de finalisation du dossier.\n\n"
                + "⚠️ **Exprimée en fraction, pas en pourcentage** : `1.00` signifie 100 %. "
                + "L'écran légataire affiche « 100.00 % » pour cette même valeur. Comportement "
                + "inchangé — la précision est ajoutée ici parce qu'elle manquait.")
        BigDecimal probabiliteFinalisation,

        String modePaie,

        // ------------------------------------------------------------------
        // En-tête de l'écran « Dossier de vente » du légataire — les cases que la fiche
        // ne portait pas. Toutes lues sur ComFolder, aucune requête supplémentaire.
        // Voir instructions/FEATURE-fiche-dossier-alignement-ecran.md §4.
        // ------------------------------------------------------------------

        @Schema(description = "Prix unitaire au m² (`ComFolder.UnitCost`) — la case « Prix du m² ». "
                + "**98,3 %** des 358 dossiers réels.", example = "13000.00")
        BigDecimal prixM2,

        @Schema(description = "Prix de vente **hors taxes** (`ComFolder.PriceHT`), après remise. "
                + "Le pendant HT de `prix`. **98,3 %**.", example = "776250.00")
        BigDecimal prixHT,

        @Schema(description = "Prix **hors taxes avant remise** (`ComFolder.PriceBeforeDiscountHT`) "
                + "— la case « Total HT ». Le pendant HT de `prixAvantRemise`. **98,3 %**.",
                example = "747500.00")
        BigDecimal prixAvantRemiseHT,

        @Schema(description = "Taux de TVA appliqué au dossier (`ComFolder.VATRatio`). **42,7 %** "
                + "— `null` ou zéro sur la majorité des dossiers, ne pas en déduire un taux nul.")
        BigDecimal tauxTVA,

        @Schema(description = "Date de validité du statut courant (`ComFolder.StatusValidityDate`) "
                + "— la date affichée à droite du statut sur l'écran légataire. **11,5 %** : elle "
                + "n'a de sens que sur les statuts qui expirent, la réservation au premier chef.")
        LocalDateTime dateValiditeStatut,

        @Schema(description = "Quote-part de l'acquéreur principal sur le dossier "
                + "(`ComFolder.PartPercent`).\n\n"
                + "⚠️ **Fraction, pas pourcentage** : `1.00` = 100 %. Renseignée sur 100 % des "
                + "dossiers, mais **95 % valent exactement 1,00** — l'information ne porte que sur "
                + "les 5 % en indivision. Les quotes-parts des co-acquéreurs se lisent sur "
                + "`GET /api/dossiers/{id}/payeurs`.")
        BigDecimal quotePart,
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
        LocalDateTime dateProspection,

        // ------------------------------------------------------------------
        // Responsable de vente et délais de cycle — lot « colonnes de l'écran légataire ».
        // Trouvés en comparant ComFolder (114 colonnes) à ce que la fiche servait (30).
        // ------------------------------------------------------------------

        @Schema(description = "Id du responsable de vente (ComFolder.CfgSaleManagerId).")
        UUID responsableVenteId,

        @Schema(description = "Responsable de vente — acteur **distinct du commercial**, renseigné "
                + "sur 221 des 358 dossiers réels (61 %). Le second responsable du légataire "
                + "(`CfgSaleManager2Id`) n'est pas exposé : mesuré à 0 ligne.",
                example = "Hamza ATCHANY")
        String responsableVenteNom,

        @Schema(description = "Date du compromis (ComFolder.AgreementDate). 73 / 358.")
        LocalDateTime dateCompromis,

        @Schema(description = "Date de livraison réelle (ComFolder.DeliveryDateReal). 63 / 358.")
        LocalDateTime dateLivraisonReelle,

        @Schema(description = "Jours entre l'origine du dossier et sa concrétisation — calculé "
                + "côté serveur, jamais stocké. L'origine est la date de la **prospection** quand "
                + "il y en a une, sinon celle du dossier, comme dans le logiciel de bureau. "
                + "`null` tant que la concrétisation n'a pas eu lieu : 152 / 358.", example = "47")
        Integer delaiConcretisation,

        @Schema(description = "Jours entre la concrétisation et le compromis. 73 / 358.",
                example = "112")
        Integer delaiNotaire,

        @Schema(description = "Jours entre le compromis et la livraison réelle. 63 / 358.\n\n"
                + "Le délai de crédit du logiciel de bureau n'est pas exposé : il dépend de "
                + "`CreditPossessionDate`, mesurée à **0 ligne sur 358**.", example = "365")
        Integer delaiLivraison
) {
}
