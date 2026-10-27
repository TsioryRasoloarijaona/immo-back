package com.tsix.immocrm.dto.commission;

import com.tsix.immocrm.domain.commission.RaisonIgnoree;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat d'une génération : lignes écrites et dossiers ignorés avec leur raison.")
public record ResultatGenerationDto(List<LigneCommissionCalculeeDto> creees, List<DossierIgnoreDto> ignores) {

    @Schema(description = "Dossier sans ligne générée ; ce n'est pas une erreur.")
    public record DossierIgnoreDto(UUID dossierId, String dossierCode, RaisonIgnoree raison,
                                   @Schema(description = "Raison en clair.") String libelle) {
    }
}
