package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Phases ouvertes par le statut stocké (BoolField001..004), toutes fausses si la demande n'est pas la dernière.")
public record PhasesDemandeAchat(
        @Schema(description = "Bordereau modifiable (BoolField001).") boolean bordereau,
        @Schema(description = "Présélection des fournisseurs (BoolField002).") boolean preselection,
        @Schema(description = "Saisie des offres (BoolField003).") boolean offres,
        @Schema(description = "Sélection d'une offre (BoolField004).") boolean selection,
        @Schema(description = "Bon de commande depuis une offre : BoolField004, procédure multi, lot multi ou sans lot.")
        boolean bonCommande,
        @Schema(description = "Comparaison des offres (BoolField003 ou BoolField004).") boolean comparaison) {
}
