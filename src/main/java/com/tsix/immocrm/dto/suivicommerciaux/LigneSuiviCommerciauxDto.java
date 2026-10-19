package com.tsix.immocrm.dto.suivicommerciaux;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneSuiviCommerciaux;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonFilter(ColonneSuiviCommerciaux.FILTRE)
@Schema(description = "Une interaction attribuée à son auteur ; seules les colonnes demandées par ?colonnes= sont présentes.")
public record LigneSuiviCommerciauxDto(
        @Schema(description = "ComActionMessage.PKey.") UUID id,
        @Schema(description = "Jour de DocDate (heure de Casablanca).") LocalDate jour,
        @Schema(description = "ComActionMessage.DocDate.") LocalDateTime dateSaisie,
        @Schema(description = "Auteur : description du compte, sinon son code.") String commercialNom,
        @Schema(description = "Code du type (ComActionMessageType.Code).", example = "Téléphone Sortant") String type,
        @Schema(description = "Code du résultat (ComActionMessageSubCategory.Code), null si absent.") String resultat,
        @Schema(description = "Appel joint (R2) ; null pour une interaction qui n'est pas un appel.") Boolean joint,
        @Schema(description = "Nom du tiers de l'interaction, sinon du prospect, sinon code de la prospection.")
        String personneNom,
        @Schema(description = "ComProspection.PKey, null hors prospection.") UUID prospectionId,
        @Schema(description = "Projet de la prospection, sinon du dossier.") String projet) {
}
