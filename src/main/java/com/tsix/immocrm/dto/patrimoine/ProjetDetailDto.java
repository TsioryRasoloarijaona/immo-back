package com.tsix.immocrm.dto.patrimoine;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjetDetailDto(
        UUID id,
        String code,
        String description,
        String descriptionLongue,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        String villeSignature,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinCommercialisation
) {
}
