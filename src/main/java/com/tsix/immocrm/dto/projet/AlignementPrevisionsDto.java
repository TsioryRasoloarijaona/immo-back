package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Valeurs de la modale « Aligner les prévisions au réel », calculées sur l'état enregistré du lot.")
public record AlignementPrevisionsDto(
        @Schema(description = "Date fin actuelle des prévisions (EndDate).") LocalDate finActuelle,
        @Schema(description = "Date fin proposée par le système : dernier réel (ou fin actuelle) + écart de temps.")
        LocalDate finSysteme,
        @Schema(description = "Date du dernier décompte facturé (multi-fournisseur : facture) ; null sans réel.")
        LocalDate dateDernierDocumentReel,
        @Schema(description = "Différence du montant : factures réelles − nets des décomptes réels ; 0 si |écart| < 0,1.")
        BigDecimal ecartMontant,
        @Schema(description = "Différence d'avancement : réel − prévu à la date du dernier réel (fraction) ; 0 sans réel.")
        BigDecimal ecartAvancement,
        @Schema(description = "Différence du temps en jours : date de référence − dernier réel (négatif = retard).")
        BigDecimal ecartJours
) {
}
