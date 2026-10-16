package com.tsix.immocrm.dto.dossier.compteurs;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Masque « Compteur » d'OMRAN ({@code Yes.Com.Counter/Item.cs}) : colonnes de l'article principal du dossier.
 * En lecture, chaque champ vaut la valeur en base (null si vide) ; en PATCH, null = inchangé, "" = effacé.
 */
@Schema(description = "Compteurs de l'article principal du dossier (StkItem), 50 caractères au plus par champ.")
public record CompteursDto(
        @Schema(description = "Référence STEG (StkItem.ElectricityRef).")
        String referenceSteg,
        @Schema(description = "N° compteur d'électricité (StkItem.ElectricityMeter).")
        String compteurElectricite,
        @Schema(description = "Arrêté (relevé) du compteur électricité (StkItem.ElectricityMeterIndex).")
        String releveElectricite,
        @Schema(description = "N° police électricité (StkItem.ElectricityMeterPolice).")
        String policeElectricite,
        @Schema(description = "N° compteur d'eau (StkItem.WaterMeter).")
        String compteurEau,
        @Schema(description = "Arrêté (relevé) du compteur eau (StkItem.WaterMeterIndex).")
        String releveEau,
        @Schema(description = "N° police eau (StkItem.WaterMeterPolice).")
        String policeEau,
        @Schema(description = "N° compteur du gaz (StkItem.GasMeter).")
        String compteurGaz,
        @Schema(description = "Arrêté (relevé) du compteur gaz (StkItem.GasMeterIndex).")
        String releveGaz,
        @Schema(description = "N° police gaz (StkItem.GasMeterPolice).")
        String policeGaz
) {
}
