package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Une ligne de synthèse de l'onglet « Montants » du logiciel de bureau : un montant, ce qui en a
 * été réglé, et ce qui reste.
 *
 * <p>Distinct de {@link CategorieMontantDto}, qui porte la même triade mais pour une
 * <em>catégorie d'échéancier</em>. Les lignes de synthèse ne sont pas des catégories : elles
 * agrègent l'échéancier selon des règles métier vérifiées sur les 358 dossiers réels — voir
 * {@code instructions/FEATURE-fiche-dossier-alignement-ecran.md} §5.
 */
@Schema(description = "Triade Montant / Réglé / Reste d'une ligne de synthèse financière.")
public record SyntheseMontantDto(

        @Schema(description = "Montant dû au titre de cette ligne.", example = "931500.00")
        BigDecimal montant,

        @Schema(description = "Part déjà encaissée.", example = "160000.00")
        BigDecimal regle,

        @Schema(description = "Solde restant — toujours `montant − regle`, vérifié sur les 358 "
                + "dossiers réels.", example = "771500.00")
        BigDecimal reste
) {
}
