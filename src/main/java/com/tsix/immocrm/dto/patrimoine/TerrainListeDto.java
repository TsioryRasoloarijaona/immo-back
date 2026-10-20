package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne des listes Terrains et Lotissements. Références : {id, code, description}, null si absentes.")
public record TerrainListeDto(
        @Schema(description = "StkItem.PKey.") UUID id,
        String code,
        String description,
        @Schema(description = "Société (CfgCompanyId).") ReferentielItemDto entite,
        @Schema(description = "Ville (AdmCityId).") ReferentielItemDto ville,
        @Schema(description = "Statut (StrStatus).") ReferentielItemDto statut,
        @Schema(description = "Vocation (StkVocation).") ReferentielItemDto vocation,
        @Schema(description = "Tranche (StkHierarchyId) : renseignée pour un lot de terrain, sinon null.")
        ReferentielItemDto tranche,
        @Schema(description = "Area.") BigDecimal surfaceDefinitive,
        @Schema(description = "SalePrice.") BigDecimal prixDefinitifTTC,
        @Schema(description = "TitleNumber.") String titreFoncier,
        @Schema(description = "Affecté à un dossier de vente ou de location.") boolean occupe
) {
}
