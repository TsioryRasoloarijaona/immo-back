package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de l'import Excel des prix d'une offre : l'offre relue et le décompte des rangées du fichier.")
public record ImportOffreDemandeAchatDto(
        @Schema(description = "Offre relue après l'import, totaux recalculés.") OffreDemandeAchat offre,
        @Schema(description = "Rangées du fichier dont le code a été trouvé dans l'offre (lignes mises à jour).") int lignesMisesAJour,
        @Schema(description = "Rangées du fichier dont le code est absent de l'offre : ignorées.") int codesIgnores) {
}
