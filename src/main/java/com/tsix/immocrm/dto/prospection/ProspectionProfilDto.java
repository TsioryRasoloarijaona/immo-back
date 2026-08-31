package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** {@code ComProspectionSelectedProfile} row. */
public record ProspectionProfilDto(
        UUID id,
        UUID profilId,
        String profil,
        UUID categorieId,
        String categorie
) {
}
