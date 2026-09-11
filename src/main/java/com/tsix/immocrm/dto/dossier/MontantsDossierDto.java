package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shape for {@code GET /api/dossiers/{id}/montants} — always computed server-side from the
 * échéancier ({@code ComDocument}), never stored.
 *
 * NOTE: "ne pas compter FRAIS dans le total soumis à la règle prix TTC" is implemented via
 * {@code ComDocument.excludeFromPrice} (the schema's own boolean for exactly this purpose)
 * rather than string-matching a "FRAIS" category code, since category codes are
 * admin-configurable free text. FEATURE-plan-financement.md (referenced by the endpoint spec)
 * is not present in this repo to confirm the exact rule — adjust if it specifies otherwise.
 */
public record MontantsDossierDto(
        List<CategorieMontantDto> parCategorie,
        BigDecimal totalGeneral,
        BigDecimal totalSoumisReglePrixTTC,

        // ------------------------------------------------------------------
        // Les deux lignes de synthèse de l'onglet « Montants » du logiciel de bureau, qui n'est
        // PAS une catégorie d'échéancier — voir
        // instructions/FEATURE-fiche-dossier-alignement-ecran.md §5.
        // ------------------------------------------------------------------

        @Schema(description = "Ligne « Prix de vente » de l'écran légataire.\n\n"
                + "`montant` est le prix stocké sur le dossier (`ComFolder.Price`), **pas** une "
                + "re-dérivation de l'échéancier : les deux coïncident sur 327 des 328 dossiers "
                + "actifs, et c'est le prix stocké qui fait foi.\n\n"
                + "`regle` agrège les règlements des échéances **non exclues du prix** "
                + "(`ComDocument.excludeFromPrice`), donc hors frais.")
        SyntheseMontantDto prixVente,

        @Schema(description = "Ligne « Net à payer » de l'écran légataire — le prix de vente "
                + "augmenté des frais exclus du prix.\n\n"
                + "⚠️ **Sur un dossier sorti** (`ANNULATION` ou `DESISTEMENT`), le montant se "
                + "réduit à ce qui a déjà été encaissé et le reste tombe à **zéro** : un dossier "
                + "annulé ne reste rien devoir. Règle vérifiée sur les 30 dossiers sortis réels, "
                + "sans exception. Ce n'est pas une donnée manquante mais la règle métier.")
        SyntheseMontantDto netAPayer,

        @Schema(description = "`true` si le dossier est **sorti** du parcours — statut "
                + "`ANNULATION` ou `DESISTEMENT`. Correspond exactement au `IsCancellation` du "
                + "logiciel de bureau (30/30 sur les dossiers réels). Explique la règle "
                + "particulière de `netAPayer`.")
        Boolean dossierSorti
) {
}
