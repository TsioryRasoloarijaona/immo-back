package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calculé depuis l'échéancier, jamais stocké. Les FRAIS sont exclus du total TTC par
 * {@code ComDocument.excludeFromPrice}, pas par leur code de catégorie (texte libre configurable).
 * Voir FEATURE-plan-financement.md, « Règle de validation ».
 */
public record MontantsDossierDto(
        List<CategorieMontantDto> parCategorie,
        BigDecimal totalGeneral,
        BigDecimal totalSoumisReglePrixTTC,

        // Synthèses de l'onglet « Montants » du bureau, pas des catégories d'échéancier —
        // voir FEATURE-fiche-dossier-alignement-ecran.md §5.

        SyntheseMontantDto prixVente,

        SyntheseMontantDto netAPayer,

        Boolean dossierSorti,

        @Schema(description = "Trop-perçu (≥ 0) : réglé au-delà du total dû, formule de ComSaleFolderView. "
                + "0 pour un dossier débiteur.", example = "0")
        BigDecimal tropPercu,

        @Schema(description = "Trop-perçu restant à rembourser (≥ 0), formule de ComSaleFolderView. "
                + "0 pour un dossier débiteur.", example = "0")
        BigDecimal tropPercuReste
) {
}
