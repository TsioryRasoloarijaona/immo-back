package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche « Réunion du chantier » avec ses quatre onglets.")
public record ReunionFicheDto(
        @Schema(description = "BulMeeting.PKey.") UUID id,
        @Schema(description = "O.D.T. : rang de la réunion dans son projet et son type, calculé à la création.") Integer ordre,
        @Schema(description = "Référence P.V. (BulMeeting.Code), générée « yy/nnnnn » si laissée vide.") String reference,
        @Schema(description = "Projet (PrjProject.PKey), non modifiable.") UUID projetId,
        @Schema(description = "Libellé du projet.") String projet,
        @Schema(description = "Type de réunion (BulMeetingType.PKey), non modifiable.") UUID typeId,
        @Schema(description = "Libellé du type de réunion.") String type,
        @Schema(description = "Date de la réunion (jour).") LocalDate date,
        @Schema(description = "Date de la prochaine réunion ; null si non prévue.") LocalDate prochaineDate,
        @Schema(description = "Surveillant (CfgTier.PKey) ; null si aucun.") UUID surveillantId,
        @Schema(description = "Raison sociale du surveillant.") String surveillant,
        @Schema(description = "Note libre (500 caractères).") String note,
        @Schema(description = "Onglet Maître d'ouvrage (BulMeetingContact), par O.D.T.") List<ContactReunionDto> maitresOuvrage,
        @Schema(description = "Onglet Intervenants (BulDelegate), par O.D.T.") List<IntervenantDto> intervenants,
        @Schema(description = "Onglet Décisions (BulMeetingLine), par O.D.T.") List<DecisionDto> decisions,
        @Schema(description = "Onglet Avancements : critères × niveaux.") AvancementsDto avancements) {
}
