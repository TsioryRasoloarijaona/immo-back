package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne d'un onglet de lecture de la fiche tiers ; sa forme dépend de l'onglet.",
        oneOf = {LigneProspectionTiersDto.class, LigneDossierTiersDto.class, LigneRecetteTiersDto.class,
                LigneDepenseTiersDto.class, LigneReglementTiersDto.class, LigneCongeTiersDto.class,
                LigneHistoriqueStatutTiersDto.class})
public sealed interface LigneOngletTiers permits LigneProspectionTiersDto, LigneDossierTiersDto, LigneRecetteTiersDto,
        LigneDepenseTiersDto, LigneReglementTiersDto, LigneCongeTiersDto, LigneHistoriqueStatutTiersDto {

    UUID id();
}
