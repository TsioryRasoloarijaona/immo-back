package com.tsix.immocrm.dto.dossier;

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

        Boolean dossierSorti
) {
}
