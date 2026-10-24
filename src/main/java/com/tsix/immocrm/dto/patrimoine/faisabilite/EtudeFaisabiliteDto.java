package com.tsix.immocrm.dto.patrimoine.faisabilite;

import com.tsix.immocrm.domain.faisabilite.ValeursFaisabilite;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'une étude de faisabilité (StkFeasibilityStudy) : identité, bloc A et phases calculés, "
        + "grilles, bilan en lecture seule.")
public record EtudeFaisabiliteDto(
        @Schema(description = "PKey de l'étude.") UUID id,
        @Schema(description = "Code unique.") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Date du document.") LocalDateTime dateDocument,
        @Schema(description = "Société (CfgCompany).") UUID societeId,
        @Schema(description = "Libellé de la société.") String societeLibelle,
        @Schema(description = "Terrain (article TER), null si aucun.") UUID terrainId,
        @Schema(description = "Libellé du terrain.") String terrainLibelle,
        @Schema(description = "Prospection terrain liée (LndLandId), null si aucune ; se pose par PUT …/prospection-terrain.")
        UUID prospectionTerrainId,
        @Schema(description = "Libellé de la prospection liée (LndLand.Description), null si aucune.")
        String prospectionTerrainDescription,
        @Schema(description = "Description du terrain.") String descriptionTerrain,
        @Schema(description = "Statut, lecture seule (« En étude » à la création).") UUID statutId,
        @Schema(description = "Libellé du statut.") String statutLibelle,
        @Schema(description = "Couleur du statut (nom .NET), null si aucune.") String statutCouleur,
        @Schema(description = "Étude bloquée : toute écriture répond 409 avant Débloquer.") boolean bloquee,
        @Schema(description = "Calculé : Σ quantités des unités à catégorie d'article (hors sous-article), non surface.")
        Integer nombreArticlesAVendre,
        @Schema(description = "Quote-part du terrain utilisée pour la TVA.") Boolean proportionTerrainPourTva,
        @Schema(description = "TVA par défaut (fraction).") BigDecimal tauxTvaDefaut,
        @Schema(description = "TVA sur intérêts (fraction).") BigDecimal tauxTvaInterets,
        @Schema(description = "Écart-type des travaux.") BigDecimal ecartTypeTravaux,
        @Schema(description = "Écart-type de la concrétisation.") BigDecimal ecartTypeConcretisation,
        @Schema(description = "Écart-type de la finalisation.") BigDecimal ecartTypeFinalisation,
        @Schema(description = "Délai de retour de la retenue de garantie (jours).") Integer delaiRetourJours,
        @Schema(description = "Taux de retenue de garantie (fraction).") BigDecimal tauxRetenueGarantie,
        @Schema(description = "Taux d'application de la retenue de garantie (fraction).")
        BigDecimal tauxApplicationRetenueGarantie,
        @Schema(description = "Part de la concrétisation dans le chiffre d'affaires (fraction).")
        BigDecimal pourcentageConcretisation,
        @Schema(description = "Taux de l'impôt sur les sociétés (fraction).") BigDecimal tauxIs,
        @Schema(description = "Prix de vente du m² (UnitSalePrice).") BigDecimal prixVenteM2,
        @Schema(description = "Bloc A et phases tels que stockés (recalculés à chaque écriture).") ValeursFaisabilite valeurs,
        @Schema(description = "Bilan recalculé à chaque écriture depuis les prévisions et le financement.") BilanEtudeDto bilan,
        @Schema(description = "Unités de base de l'étude, par ordre.") List<LigneUniteEtudeDto> unites,
        @Schema(description = "Tranches de l'étude et leurs ventilations, par ordre.") List<LigneTrancheEtudeDto> tranches,
        @Schema(description = "Créateur (UserCreate).") String userCreate,
        @Schema(description = "Date de création.") LocalDateTime userCreateDateTime,
        @Schema(description = "Dernier auteur (UserUpdate).") String userUpdate,
        @Schema(description = "Date de dernière modification.") LocalDateTime userUpdateDateTime) {
}
