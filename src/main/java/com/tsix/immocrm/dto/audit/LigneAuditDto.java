package com.tsix.immocrm.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Une ligne du journal d'audit OMRAN (AdmWebHistorics), qu'elle vienne d'OMRAN ou du CRM.")
public record LigneAuditDto(
        @Schema(description = "AdmWebHistorics.PKey.")
        UUID id,
        @Schema(description = "Date de l'écriture (ActionDateTime).")
        LocalDateTime date,
        @Schema(description = "Nom affiché de l'auteur (UserDescription).", example = "Khadija ELKORRI")
        String utilisateur,
        @Schema(description = "Code brut : Added, Modified ou Deleted (traduit par le front).", example = "Modified")
        String action,
        @Schema(description = "Classe OMRAN de l'objet (ObjectType).", example = "Yes.Stk.Building.Item")
        String typeObjet,
        @Schema(description = "Libellé de la classe (ObjectTypeLabel).", example = "Article")
        String libelleTypeObjet,
        @Schema(description = "Libellé de l'objet au moment de l'écriture (ObjectLabel).", example = "ARS1141")
        String libelleObjet,
        @Schema(description = "Classe du parent pour une ligne enfant (ex. terrain d'une tranche), sinon null.")
        String parentTypeObjet,
        @Schema(description = "Propriétés modifiées ; vide si le JSON Details est absent ou illisible.")
        List<ChangementAuditDto> details
) {
}
