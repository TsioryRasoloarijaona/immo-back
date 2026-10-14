package com.tsix.immocrm.dto.dossier;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneDossier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seules les clés demandées par {@code ?colonnes=} sont sérialisées ({@link ColonneDossier}, liste
 * blanche) ; sans le paramètre, les colonnes par défaut.
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
        /** Jalon « Concrétisation » ({@code ComFolder.advanceDate}) ; {@code dateDocument} tient lieu de date de
         * réservation (voir {@code ChampDateDossier} et instructions/filtres-avances.md, point 3). */
        LocalDateTime dateConcretisation,
        @Schema(description = "Identifiant de la prospection à l'origine du dossier (ComFolder.ComProspectionId).")
        UUID prospectionId,
        String prospectionCode,

        // Hors sélection par défaut, toutes les colonnes qui suivent.

        @Schema(description = "Prix avant remise.")
        BigDecimal prixAvantRemise,

        @Schema(description = "Taux de remise en pourcentage (5 = 5 %), jamais un ratio 0–1.")
        BigDecimal remise,

        @Schema(description = "Montant de la remise, hors taxes (`ComFolder.DiscountAmount`).")
        BigDecimal remiseMontant,

        @Schema(description = "Prix après remise.")
        BigDecimal prixApresRemise,

        @Schema(description = "Montant de l'avance. Renseigné sur 41,6 % des 358 dossiers réels.")
        BigDecimal montantAvance,

        @Schema(description = "Probabilité de finalisation. 100 %.")
        BigDecimal probabiliteFinalisation,

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

        // Article principal : demander une de ces colonnes coûte jusqu'à cinq jointures paresseuses par ligne.
        @Schema(description = "Id de l'article principal (StkItem.PKey). 100 % des 358 dossiers.")
        UUID articleId,

        @Schema(example = "A-203")
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

        @Schema(description = "Id du responsable de vente (ComFolder.CfgSaleManagerId).")
        UUID responsableVenteId,

        @Schema(example = "Hamza ATCHANY")
        String responsableVenteNom,

        @Schema(description = "Date du compromis / approbation crédit (ComFolder.CreditApprovalDate).")
        LocalDateTime dateCompromis,

        @Schema(description = "Date de livraison réelle (ComFolder.DeliveryDateReal). 63 / 358.")
        LocalDateTime dateLivraisonReelle,

        @Schema(example = "47")
        Integer delaiConcretisation,

        @Schema(description = "Jours entre la concrétisation et l'envoi au notaire. Calculé côté serveur.",
                example = "112")
        Integer delaiNotaire,

        @Schema(example = "365")
        Integer delaiLivraison,

        @Schema(description = "Date d'envoi au notaire / avocat (ComFolder.AgreementDate).")
        LocalDateTime dateEnvoiNotaire
) {
}
