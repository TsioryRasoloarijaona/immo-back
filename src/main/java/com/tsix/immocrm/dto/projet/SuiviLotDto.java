package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Onglets Suivi et Retenues de la fiche lot (montants arrondis à 2 décimales, calculés côté serveur).")
public record SuiviLotDto(
        @Schema(description = "Colonnes Décomptes, Factures et Avoirs.") ColonnesSuiviDto colonnes,
        @Schema(description = "Avance sur marché.") BlocAvanceDto avanceMarche,
        @Schema(description = "Avance sur décompte (taux, estimations et marché null).") BlocAvanceDto avanceDecompte,
        @Schema(description = "Retenue de garantie.") RetenueGarantieDto retenueGarantie,
        @Schema(description = "Onglet Retenues.") RetenuesLotDto retenues
) {

    @Schema(description = "Les trois colonnes de l'onglet Suivi.")
    public record ColonnesSuiviDto(
            @Schema(description = "Décomptes du lot.") ColonneSuiviDto decomptes,
            @Schema(description = "Factures du lot (hors factures d'avenant).") ColonneSuiviDto factures,
            @Schema(description = "Avoirs du lot : seuls montantHT, tva, montantTTC et regulation sont renseignés.")
            ColonneSuiviDto avoirs
    ) {
    }
}
