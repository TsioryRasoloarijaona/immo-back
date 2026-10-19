package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Type de dossier proposé dans l'onglet Finalisations, avec ses statuts.")
public record EntiteFinalisationDto(
        @Schema(description = "PKey de StrEntity.") UUID id,
        @Schema(description = "Code (ComFolder, PrmFolderSale, RomFolderSale).") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Statuts de ce type de dossier, par ordre (Sorting) ; choix des deux statuts.")
        List<ReferentielItemDto> statuts
) {
}
