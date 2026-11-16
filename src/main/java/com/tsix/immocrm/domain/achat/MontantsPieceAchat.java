package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Seul calcul des montants d'une pièce d'achat saisie, sans arrondi (OMRAN {@code BaseLine.cs} l.254-334,
 * {@code DocumentBase.cs:VatLines}) : FEATURE-achat-fiche-creation.md E-C2. Taux en fraction.
 */
public final class MontantsPieceAchat {

    /** Ligne : quantité, prix unitaire brut HT, remise, FODEC, TVA (null lu 0). */
    public record Ligne(BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal remise, BigDecimal fodec,
                        BigDecimal tva) {

        private LigneAchatCalculee calcul() {
            return LigneAchatCalculee.de(prixUnitaire, quantite, remise, fodec, tva);
        }

        /** {@code UnitPriceTTC} stocké : PU HT net × (1 + TVA). */
        public BigDecimal prixTTC() {
            return calcul().prixUnitaireHTNet().multiply(BigDecimal.ONE.add(zero(tva)));
        }

        public BigDecimal totalHT() {
            return calcul().totalHT();
        }

        public BigDecimal totalTVA() {
            return calcul().totalTVA();
        }

        public BigDecimal totalTTC() {
            return calcul().totalTTC();
        }
    }

    public record Totaux(BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    private MontantsPieceAchat() {
    }

    public static Totaux totaux(List<Ligne> lignes) {
        BigDecimal ht = lignes.stream().map(Ligne::totalHT).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tva = lignes.stream().map(Ligne::totalTVA).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Totaux(ht, tva, ht.add(tva));
    }

    private static BigDecimal zero(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
