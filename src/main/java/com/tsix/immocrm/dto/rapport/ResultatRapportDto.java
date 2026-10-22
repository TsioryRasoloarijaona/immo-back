package com.tsix.immocrm.dto.rapport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Résultat d'un rapport : sections calculées, totaux compris.")
public record ResultatRapportDto(
        @Schema(description = "Code du rapport.", example = "ComTdbSalesMonitoringGlobal") String code,
        @Schema(description = "Libellé du rapport.", example = "SUIVI GLOBAL DE VENTE*") String libelle,
        @Schema(description = "Slug du module.", example = "vente") String module,
        @Schema(description = "Valeurs effectivement appliquées, par nom de paramètre, défauts compris.")
        Map<String, List<String>> parametres,
        @Schema(description = "Heure de calcul (horloge du serveur, Casablanca).") LocalDateTime genereLe,
        @Schema(description = "Écarts assumés ou données manquantes, à afficher tels quels.")
        List<String> avertissements,
        @Schema(description = "Sections, dans l'ordre d'affichage.") List<SectionRapportDto> sections) {
}
