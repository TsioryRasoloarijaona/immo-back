package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Fiche complète d'un dossier de vente en consultation, organisée par onglet de l'écran OMRAN.")
public record DossierFicheDto(
        @Schema(description = "Identifiant du dossier (ComFolder.PKey).")
        UUID id,
        @Schema(description = "N° de dossier (ComFolder.Code).")
        String code,
        EnTeteFicheDto enTete,
        StatutFicheDto statut,
        AutresMontantsFicheDto autresMontants,
        DatesFicheDto dates,
        DatesAutresFicheDto datesAutres,
        AnnulationFicheDto annulation,
        AutresFicheDto autres,
        @Schema(description = "Bandeau « Ce dossier possède des pièces de règlement impayées » : une échéance échue reste due.")
        boolean aDesImpayes,
        @Schema(description = "Vrai si l'appelant est le commercial destinataire d'une instruction non lue sur ce dossier.")
        boolean instructionNonLue
) {
}
