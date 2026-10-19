package com.tsix.immocrm.dto.suivicommerciaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Compteurs d'un commercial pour un jour (ligne), ou leur somme (totaux).")
public record LigneSyntheseSuiviDto(
        @Schema(description = "Employé du compte, sinon le compte ; null dans les totaux par jour et général.")
        UUID commercialCle,
        String commercialNom,
        @Schema(description = "Null dans les totaux par commercial et général.") LocalDate jour,
        @Schema(description = "Interactions « Téléphone Sortant ».") long appelsPasses,
        @Schema(description = "Interactions « Téléphone entrant ».") long appelsRecus,
        @Schema(description = "Appels dont le résultat est renseigné et hors résultats non joints (R2).")
        long contactsJoints,
        @Schema(description = "Personnes distinctes jointes ce jour ; dans un total, somme des cellules.")
        long personnesJointes,
        long visitesProspection,
        @Schema(description = "Visites de catégorie dossier de vente.") long visitesVente,
        @Schema(description = "Rendez-vous planifiés pour ce jour par cet auteur (R6).") long visitesPrevues,
        @Schema(description = "Prévues sans Visite sur la même prospection ce jour-là.") long visitesPrevuesNonConsignees,
        @Schema(description = "personnesJointes ≥ objectif ; null dans les totaux.") Boolean atteint,
        @Schema(description = "personnesJointes / objectif, 2 décimales ; null dans les totaux.") BigDecimal taux,
        @Schema(description = "Totaux par commercial : personnesJointes / nombre de jours de la période ; null sinon.")
        BigDecimal moyennePersonnesJointes) {
}
