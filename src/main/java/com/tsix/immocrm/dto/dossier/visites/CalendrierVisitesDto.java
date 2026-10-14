package com.tsix.immocrm.dto.dossier.visites;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Calendrier des visites techniques de la tranche du dossier, avec ses règles de programmation.")
public record CalendrierVisitesDto(
        @Schema(description = "Tranche de l'article du dossier.")
        UUID trancheId,
        @Schema(description = "Libellé de la tranche.")
        String trancheLibelle,
        @Schema(description = "Responsable S.A.V. de la tranche ; null = aucune visite ne peut être programmée.")
        String responsableSav,
        @Schema(description = "Délai minimum de programmation, en heures (CfgTranche.InterventionMinimumDelai).")
        Integer delaiMinimumHeures,
        @Schema(description = "Nombre maximum de visites par jour sur la tranche (CfgTranche.MaxVisitNumber).")
        Integer maxVisitesParJour,
        @Schema(description = "Créneaux déjà pris sur la période demandée, par heure de début.")
        List<VisiteTechniqueDto> creneaux
) {
}
