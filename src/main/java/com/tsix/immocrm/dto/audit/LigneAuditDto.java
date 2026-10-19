package com.tsix.immocrm.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ligne du journal d'audit OMRAN (AdmWebHistorics), écrite par OMRAN ou par le CRM.")
public record LigneAuditDto(
        @Schema(description = "AdmWebHistorics.PKey.") UUID id,
        @Schema(description = "ActionDateTime.") LocalDateTime date,
        @Schema(description = "UserDescription : nom affiché de l'auteur.") String utilisateur,
        @Schema(description = "Action brute, à traduire à l'écran.", allowableValues = {"Added", "Modified", "Deleted"})
        String action,
        @Schema(description = "ObjectType OMRAN (ex. Yes.Cfg.CfgTranche).") String typeObjet,
        @Schema(description = "ObjectTypeLabel (ex. « Tranche »).") String libelleTypeObjet,
        @Schema(description = "ObjectLabel : code ou nom de l'objet.") String libelleObjet,
        @Schema(description = "ParentObjectType d'une ligne enfant, sinon null.") String parentTypeObjet,
        @Schema(description = "Propriétés changées ; vide si Details est illisible.") List<ChangementAuditDto> details
) {
}
