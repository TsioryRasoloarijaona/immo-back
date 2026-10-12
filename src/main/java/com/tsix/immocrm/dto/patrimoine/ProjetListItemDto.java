package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/** Les 23 colonnes du tableau « Liste » de la page PROGRAMMES de l'aide OMRAN, plus id, ville et code entité. */
@Schema(description = "Ligne de la liste des programmes (colonnes de la liste OMRAN).")
@Builder
public record ProjetListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        @Schema(description = "Code de l'entité juridique (CfgCompany.Code).")
        String entiteCode,
        @Schema(description = "Libellé de l'entité juridique (« Entité » d'OMRAN).")
        String entiteLibelle,
        @Schema(description = "« Adresse » d'OMRAN : ville, adresses 1 à 3, code postal et pays mis bout à bout.")
        String adresseComplete,
        @Schema(description = "« Courrier fait à » (SignatureCity).")
        String villeSignature,
        @Schema(description = "« Courrier signataire » : nom du personnel.")
        String signataireLibelle,
        @Schema(description = "« Représentant légal » : représentant de l'entité juridique.")
        String representantLegal,
        @Schema(description = "« Représentant commercial » : responsable commercial.")
        String responsableCommercialLibelle,
        @Schema(description = "« Représentant technique » : responsable technique.")
        String responsableTechniqueLibelle,
        String descriptionLongue,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinCommercialisation,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
