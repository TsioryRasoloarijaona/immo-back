package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de l'onglet Commission vendeur (ComSalesCommission) : période, clé et six blocs.")
public record LigneCommissionDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Date début (StartDate) ; doit être ≤ dateFin.") LocalDateTime dateDebut,
        @Schema(description = "Date fin (EndDate) ; absente = période sans fin.") LocalDateTime dateFin,
        @Schema(description = "Commercial / gestionnaire (CfgTierId) ; absent = tous.") UUID commercialId,
        @Schema(description = "Catégorie d'article (StkItemCategoryId) ; absente = toutes.") UUID categorieArticleId,
        @Schema(description = "Personnel (ComEmployeeId) ; choix : /api/referentiels-patrimoine/personnels.")
        UUID personnelId,
        @Schema(description = "Action commerciale (ComActionId) ; absente = toutes.") UUID actionCommercialeId,
        @Schema(description = "Concrétisation (SaleCommission…).") BlocCommissionDto concretisation,
        @Schema(description = "Concrétisation, prestataire (SaleCommission…Extern).") BlocCommissionDto concretisationExterne,
        @Schema(description = "Finalisation 1 (DeliveryCommission…).") BlocCommissionDto finalisation1,
        @Schema(description = "Finalisation 1, prestataire (DeliveryCommission…Extern).") BlocCommissionDto finalisation1Externe,
        @Schema(description = "Finalisation 2 (DeliveryCommission…2).") BlocCommissionDto finalisation2,
        @Schema(description = "Finalisation 2, prestataire (DeliveryCommission…Extern2).") BlocCommissionDto finalisation2Externe
) implements LignesTrancheDto.Ligne {
}
