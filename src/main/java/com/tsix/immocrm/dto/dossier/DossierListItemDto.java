package com.tsix.immocrm.dto.dossier;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneDossier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/dossiers}.
 *
 * <p><strong>Toutes les clés ne sont pas toujours présentes.</strong> Le paramètre
 * {@code ?colonnes=} sélectionne celles qui sont sérialisées ; les autres sont absentes de la
 * réponse — voir {@link ColonneDossier}, qui fait aussi office de liste blanche et alimente
 * {@code GET /api/referentiels/colonnes/dossiers}.
 *
 * <p>Sans ce paramètre, la réponse est <em>exactement</em> celle d'avant son introduction : les
 * treize colonnes historiques, et rien d'autre.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComFolder}. Adjust once a spec is available.
 */
@JsonFilter(ColonneDossier.FILTRE)
public record DossierListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        BigDecimal prix,
        BigDecimal totalAPayer,
        /**
         * Jalon "Concrétisation" ({@code ComFolder.advanceDate}) — exposé en plus de
         * {@code dateDocument} (qui sert de proxy pour "Date de réservation", voir
         * {@code ChampDateDossier}) pour le filtre par période, point 3 de
         * {@code instructions/filtres-avances.md}. Null tant que le dossier n'a pas atteint ce
         * statut.
         */
        LocalDateTime dateConcretisation,
        @Schema(description = "Identifiant de la prospection à l'origine du dossier (ComFolder.ComProspectionId).")
        UUID prospectionId,
        @Schema(description = "Code de la prospection à l'origine du dossier — pour le lien cliquable vers la prospection depuis le dossier (`GET /api/prospections/{prospectionId}`).")
        String prospectionCode,

        // ------------------------------------------------------------------
        // Colonnes ajoutées avec le sélecteur. Hors sélection par défaut, sans exception.
        // ------------------------------------------------------------------

        @Schema(description = "Prix avant remise.")
        BigDecimal prixAvantRemise,

        @Schema(description = "Taux de remise, en pourcentage.")
        BigDecimal remise,

        @Schema(description = "Montant de la remise.")
        BigDecimal remiseMontant,

        @Schema(description = "Prix après remise.")
        BigDecimal prixApresRemise,

        @Schema(description = "Montant de l'avance. Renseigné sur 41,6 % des 358 dossiers réels.")
        BigDecimal montantAvance,

        @Schema(description = "Probabilité de finalisation. 100 %.")
        BigDecimal probabiliteFinalisation,

        @Schema(description = "Identifiant du projet. 100 %. **Provient de la tranche de "
                + "l'article** — `StkItem.CfgProjectId` est nul sur les 358 articles réels.")
        UUID projetId,

        @Schema(description = "Code du projet. 100 %.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. 100 %.")
        String projetLibelle,

        @Schema(description = "Identifiant de la tranche. 100 %.")
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

        @Schema(description = "Code de la vocation. 100 %.")
        String vocationCode,

        @Schema(description = "Libellé de la vocation. 100 %.")
        String vocationLibelle,

        @Schema(description = "Identifiant du notaire. 19,3 %.")
        UUID avocatId,

        @Schema(description = "Nom du notaire. 19,3 %.")
        String avocatNom,

        @Schema(description = "Mode de paiement.")
        String modePaie,

        @Schema(description = "Étape de la prospection d'origine. `null` sur les 3 dossiers (sur "
                + "358) sans prospection.")
        String etapeProspection,

        @Schema(description = "Origine de la prospection. 99,2 %.", example = "FACEBOOK")
        String origineProspection,

        @Schema(description = "Date de la prospection d'origine. 99,2 %.")
        LocalDateTime dateProspection,

        @Schema(description = "Auteur de la création. 100 % des 358 dossiers réels.")
        String creePar,

        @Schema(description = "Date de création. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 100 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 100 %.")
        LocalDateTime modifieLe,

        // -- Article principal. Hors sélection par défaut : demander une de ces colonnes coûte
        //    jusqu'à cinq jointures paresseuses par ligne.
        @Schema(description = "Id de l'article principal (StkItem.PKey). 100 % des 358 dossiers.")
        UUID articleId,

        @Schema(description = "Code de l'article principal. **C'est la clé de tri par défaut de la "
                + "liste du logiciel de bureau** (`ORDER BY ArticleCode, H1Code, H2Code, H3Code`, "
                + "où H1/H2/H3 sont la tranche, le projet et la société — déjà servis ici sous "
                + "`trancheCode`, `projetCode` et `societeCode`).", example = "A-203")
        String articleCode,

        @Schema(description = "Libellé de l'article principal.", example = "Appartement 203")
        String articleLibelle,

        @Schema(description = "Numéro de titre foncier de l'article.")
        String articleTitreFoncier,

        @Schema(description = "Bâtiment de l'article (StkBuilding). Renseigné sur 1 063 des 1 068 "
                + "articles réels.", example = "Bloc B")
        String bloc,

        @Schema(description = "Étage de l'article (StkFloor). 1 063 / 1 068.", example = "2e étage")
        String etage,

        @Schema(description = "Code catégorie de l'article.", example = "APPARTEMENT")
        String categorieArticle,

        @Schema(description = "Code type de l'article.", example = "2 CHAMBRES + SALON")
        String typeArticle,

        @Schema(description = "Code du type de présentation de l'article.")
        String typePresentation,

        // -- Acteur supplémentaire.
        @Schema(description = "Id du responsable de vente (ComFolder.CfgSaleManagerId).")
        UUID responsableVenteId,

        @Schema(description = "Responsable de vente — acteur **distinct du commercial**, renseigné "
                + "sur 221 des 358 dossiers réels (61 %). Le second responsable du légataire "
                + "(`CfgSaleManager2Id`) n'est pas exposé : mesuré à 0 ligne.",
                example = "Hamza ATCHANY")
        String responsableVenteNom,

        // -- Jalons et délais du cycle de vente.
        @Schema(description = "Date du compromis (ComFolder.AgreementDate). 73 / 358.")
        LocalDateTime dateCompromis,

        @Schema(description = "Date de livraison réelle (ComFolder.DeliveryDateReal). 63 / 358.")
        LocalDateTime dateLivraisonReelle,

        @Schema(description = "Jours entre l'origine du dossier et sa concrétisation. Calculé "
                + "côté serveur, jamais stocké. L'origine est la date de la **prospection** quand "
                + "il y en a une, sinon celle du dossier — même règle que le logiciel de bureau. "
                + "`null` tant que la concrétisation n'a pas eu lieu : calculable sur 152 / 358.",
                example = "47")
        Integer delaiConcretisation,

        @Schema(description = "Jours entre la concrétisation et le compromis. Calculé côté "
                + "serveur. Calculable sur 73 / 358.", example = "112")
        Integer delaiNotaire,

        @Schema(description = "Jours entre le compromis et la livraison réelle. Calculé côté "
                + "serveur. Calculable sur 63 / 358.\n\nLe délai de crédit du logiciel de bureau "
                + "n'est pas exposé : il dépend de `CreditPossessionDate`, mesurée à **0 ligne sur "
                + "358**, donc il vaudrait `null` partout.", example = "365")
        Integer delaiLivraison
) {
}
