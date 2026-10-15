package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlQuoteDocument;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * {@code VerifSchema} d'OMRAN ({@code Quote.cs} pour l'offre, {@code Sale.cs} pour le dossier) : seule règle de
 * concordance entre un schéma saisi et le prix après remise. Lignes AVANCE, CRÉDIT, AUTO-FINANCEMENT encaissées.
 */
public final class SchemaFinancementSaisi {

    /** OMRAN arrondit à {@code CurrencyDecimalDigits} (2 pour le dirham), au pair comme {@code Math.Round}. */
    private static final int DECIMALES_DEVISE = 2;

    private SchemaFinancementSaisi() {
    }

    /** Montant du schéma qui finance le prix (les « hors prix », frais syndic compris, en sont exclus). */
    public static BigDecimal montantFinance(List<StlQuoteDocument> lignes) {
        return lignes.stream()
                .filter(ligne -> financeLePrix(ligne.getComSettlementCategory()))
                .map(StlQuoteDocument::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Prix après remise − montant financé, à 2 décimales : 0 si le schéma concorde. */
    public static BigDecimal ecart(BigDecimal prixApresRemise, List<StlQuoteDocument> lignes) {
        return (prixApresRemise != null ? prixApresRemise : BigDecimal.ZERO).subtract(montantFinance(lignes))
                .setScale(DECIMALES_DEVISE, RoundingMode.HALF_EVEN);
    }

    private static boolean financeLePrix(ComSettlementCategory categorie) {
        if (categorie == null || !Boolean.TRUE.equals(categorie.getIsCredit())) {
            return false;
        }
        return Boolean.TRUE.equals(categorie.getIsSaleAdvance()) || Boolean.TRUE.equals(categorie.getIsSaleCredit())
                || Boolean.TRUE.equals(categorie.getIsSaleAutoFinancment());
    }
}
