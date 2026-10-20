package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Tranche d'une étude (StkFeasibilityStudyCfgTranche) et ses unités ventilées. Le serveur stocke les "
        + "dates reçues ; le front préremplit celles d'une tranche neuve depuis les phases de l'étude.")
public record LigneTrancheEtudeDto(
        @Schema(description = "PKey de la ligne ; absent = création.") UUID id,
        @Schema(description = "Code, unique dans l'étude, obligatoire.", example = "Tranche 1") String code,
        @Schema(description = "Ordre, obligatoire.") Integer sorting,
        @Schema(description = "Tranche du patrimoine (CfgTranche) de la société de l'étude (liste /choix/tranches).")
        UUID trancheId,
        @Schema(description = "Lecture : libellé de la tranche du patrimoine.") String trancheLibelle,
        @Schema(description = "Poids de la tranche (fraction) ; la somme des poids doit valoir 1.") BigDecimal poids,
        @Schema(description = "WorkStartDate : début des travaux.") LocalDateTime debutTravaux,
        @Schema(description = "WorkEndDate : fin des travaux.") LocalDateTime finTravaux,
        @Schema(description = "MarketingStartDate : début de commercialisation.") LocalDateTime debutCommercialisation,
        @Schema(description = "MarketingEndDate : fin de commercialisation.") LocalDateTime finCommercialisation,
        @Schema(description = "EndDateConcretisation : fin de concrétisation.") LocalDateTime finConcretisation,
        @Schema(description = "StartDateFinalisation : début de finalisation.") LocalDateTime debutFinalisation,
        @Schema(description = "DeliveryDate : livraison.") LocalDateTime dateLivraison,
        @Schema(description = "OccupancyPermitsDate : permis d'habiter.") LocalDateTime datePermisHabiter,
        @Schema(description = "Unités de l'étude ventilées sur cette tranche ; absente = aucune.")
        List<VentilationEtudeDto> unites) {
}
