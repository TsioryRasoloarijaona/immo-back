package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de la liste des études de faisabilité.")
public record EtudeFaisabiliteListeDto(
        @Schema(description = "PKey de l'étude.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Date du document.") LocalDateTime dateDocument,
        @Schema(description = "Société (CfgCompany).") UUID societeId,
        @Schema(description = "Libellé de la société.") String societeLibelle,
        @Schema(description = "Libellé du terrain (article TER), null si aucun.") String terrainLibelle,
        @Schema(description = "Libellé de la prospection terrain liée (LndLand.Description), null si aucune.")
        String prospectionTerrainDescription,
        @Schema(description = "Statut (StkFeasibilityStudyStatus).") UUID statutId,
        @Schema(description = "Libellé du statut.") String statutLibelle,
        @Schema(description = "Couleur du statut (nom .NET, ex. Green), null si aucune.") String statutCouleur,
        @Schema(description = "Étude bloquée (IsLocked) : aucune écriture avant Débloquer.") boolean bloquee,
        @Schema(description = "Superficie du terrain (m²).") BigDecimal superficie,
        @Schema(description = "Coût total du terrain TTC.") BigDecimal coutTotalTerrain,
        @Schema(description = "Charge TTC (TotalCost).") BigDecimal coutTotal,
        @Schema(description = "Chiffre d'affaires TTC (Turnover).") BigDecimal chiffreAffaires,
        @Schema(description = "Fonds propres TTC (AutofinancementAmount).") BigDecimal montantAutofinancement,
        @Schema(description = "Marge bénéficiaire nette.") BigDecimal margeNette,
        @Schema(description = "Montant de la concrétisation TTC.") BigDecimal montantConcretisation,
        @Schema(description = "Montant de la finalisation TTC.") BigDecimal montantFinalisation,
        @Schema(description = "Taux de concrétisation (fraction).") BigDecimal pourcentageConcretisation,
        @Schema(description = "Retour sur coût projet ; null si coût HT nul.") BigDecimal rentabiliteCoutProjet,
        @Schema(description = "Retour sur le C.A. ; null si C.A. HT nul.") BigDecimal rentabiliteChiffreAffaires,
        @Schema(description = "Retour sur fonds propres ; null si fonds propres HT nuls.") BigDecimal rentabiliteFondsPropres) {
}
