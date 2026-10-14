package com.tsix.immocrm.dto.dossier.acquereurs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Onglet Acquéreurs : le client principal en tête, puis les co-acquéreurs.")
public record AcquereursDto(
        List<AcquereurDto> acquereurs,
        @Schema(description = "Somme des répartitions, pourcentage ; 100 sur un dossier cohérent.")
        BigDecimal total,
        @Schema(description = "Prospections ouvertes des acquéreurs ajoutés par cet enregistrement, à proposer à l'archivage ; vide en lecture.")
        List<ProspectionAArchiverDto> prospectionsAArchiver
) {
}
