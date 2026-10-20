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
        @Schema(description = "LndLandId : terrain foncier, lecture seule (écrit par le lot Terrains).") UUID terrainFoncierId,
        @Schema(description = "Description du terrain.") String descriptionTerrain,
        @Schema(description = "Statut, lecture seule (« En étude » à la création).") UUID statutId,
        @Schema(description = "Libellé du statut.") String statutLibelle,
        @Schema(description = "Couleur du statut (nom .NET), null si aucune.") String statutCouleur,
        @Schema(description = "Étude bloquée : toute écriture répond 409 avant Débloquer.") boolean bloquee,
        @Schema(description = "Nombre d'articles à vendre.") Integer nombreArticlesAVendre,
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
        @Schema(description = "Bloc A et phases tels que stockés (recalculés à chaque écriture).") ValeursFaisabilite valeurs,
        @Schema(description = "Bilan stocké, lecture seule.") BilanEtudeDto bilan,
        @Schema(description = "Unités de base de l'étude, par ordre.") List<LigneUniteEtudeDto> unites,
        @Schema(description = "Tranches de l'étude et leurs ventilations, par ordre.") List<LigneTrancheEtudeDto> tranches,
        @Schema(description = "Vrai si l'étude a des prévisions (FSPrevision) : à recalculer en P8b après une modification.")
        boolean previsionsARecalculer,
        @Schema(description = "Créateur (UserCreate).") String userCreate,
        @Schema(description = "Date de création.") LocalDateTime userCreateDateTime,
        @Schema(description = "Dernier auteur (UserUpdate).") String userUpdate,
        @Schema(description = "Date de dernière modification.") LocalDateTime userUpdateDateTime) {
}
