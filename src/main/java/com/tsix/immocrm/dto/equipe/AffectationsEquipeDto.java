package com.tsix.immocrm.dto.equipe;

import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Matrice projets × commerciaux de l'appelant.")
public record AffectationsEquipeDto(

        @Schema(description = "Projets de l'appelant (tous les projets visibles pour Administrateur / Direction Générale).")
        List<ProjetPerimetreDto> projets,

        @Schema(description = "Commerciaux de l'équipe.")
        List<MembreAffectationDto> commerciaux,

        @Schema(description = "Commerciaux actifs hors équipe qu'un chef peut ajouter à l'un de ses projets.")
        List<MembreAffectationDto> affectables
) {

    @Schema(description = "Un commercial et ses projets.")
    public record MembreAffectationDto(

            UUID admUserId,

            @Schema(example = "m.jakha")
            String code,

            @Schema(example = "Maha JAKHA")
            String nom,

            @Schema(description = "Employé (CfgTier) ; `null` = compte sans employé.")
            UUID commercialId,

            @Schema(description = "Ses projets parmi `projets`.")
            List<UUID> projetIds,

            @Schema(description = "Aucun projet affecté : il voit tous les projets (possible seulement hors équipe d'un chef).")
            boolean sansRestriction,

            @Schema(description = "`false` : l'appelant ne peut pas modifier ses affectations (autre chef, admin, direction).")
            boolean modifiable
    ) {
    }
}
