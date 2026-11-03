package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Prospection dont le tiers est le prospect (ComProspection.CfgProspectId).")
public record LigneProspectionTiersDto(
        @Schema(description = "PKey de la prospection.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Date de la prospection (DocDate).") LocalDateTime date,
        @Schema(description = "Libellé de la vocation.") String vocation,
        @Schema(description = "Libellé de la tranche.") String tranche,
        @Schema(description = "Libellé de la nature de prospection.") String nature,
        @Schema(description = "Code de l'article.") String article,
        @Schema(description = "Libellé de l'action.") String action,
        @Schema(description = "Libellé du support d'origine.") String supportOrigine,
        @Schema(description = "Code d'un dossier de vente issu de la prospection, null sinon.") String dossierCode,
        @Schema(description = "Archivée (IsClosed).") Boolean archivee,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Toujours la fiche prospection.") LienOngletTiersDto lien) implements LigneOngletTiers {

    public LigneProspectionTiersDto(UUID id, String code, LocalDateTime date, String vocation, String tranche,
                                    String nature, String article, String action, String supportOrigine,
                                    String dossierCode, Boolean archivee, String notes) {
        this(id, code, date, vocation, tranche, nature, article, action, supportOrigine, dossierCode, archivee, notes,
                new LienOngletTiersDto("prospection", id));
    }
}
