package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Seul calcul des montants d'un décompte (Enregistrer, Recalculer, création, Définir dernier), d'après
 * {@code omran-src/Yes.Rems/Yes.Prj.WorkingOut/WorkingOut.cs} ; le net passe par {@link NetAPayerDecompte}.
 * Un terme null vaut 0. Voir FEATURE-decomptes-parite.md « Règles d'écriture (OMRAN) » §0.
 */
public final class CalculDecompte {

    private static final int ECHELLE = 6;

    private CalculDecompte() {
    }

    /** Ligne chiffrée : HT = PU HT × (1 + FODEC) × quantité, TTC = HT × (1 + TVA). */
    public record Ligne(BigDecimal quantite, BigDecimal prixHT, BigDecimal fodec, BigDecimal tva) {
        public BigDecimal ht() {
            return z(prixHT).multiply(BigDecimal.ONE.add(z(fodec))).multiply(z(quantite));
        }

        public BigDecimal ttc() {
            return ht().multiply(BigDecimal.ONE.add(z(tva)));
        }
    }

    /** « Basé sur le montant » : montant saisi lu ; sinon taux × base − déjà retenu sur les décomptes précédents. */
    public record Taux(boolean surMontant, BigDecimal taux, BigDecimal montant) {
    }

    /** Assurance plafonnée : au-delà du reste, le reste (seulement si le plafond du lot n'est pas 0). */
    public record Assurance(Taux taux, BigDecimal plafond, BigDecimal reste) {
    }

    /** Σ des décomptes du lot de numéro inférieur. */
    public record Precedents(BigDecimal ttc, BigDecimal avance, BigDecimal garantie, BigDecimal prorata, BigDecimal dec,
                             BigDecimal trc, BigDecimal retenues) {
        public static final Precedents AUCUN = new Precedents(null, null, null, null, null, null, null);
    }

    public record Entree(List<Ligne> lignes, List<Ligne> extras, List<Ligne> approvisionnements,
                         BigDecimal retenuesForfaitaires, Precedents precedents, BigDecimal avancesDuLot,
                         Taux avance, Taux garantie, Taux prorata, Assurance dec, Assurance trc,
                         BigDecimal avanceSurDecompte, BigDecimal remboursementProrata, BigDecimal tauxApprovisionnement,
                         BigDecimal approvisionnementAncien, BigDecimal approvisionnementAncienHT,
                         BigDecimal fluctuations, BigDecimal fluctuationsHT, BigDecimal fluctuationsAnciennes,
                         BigDecimal fluctuationsAnciennesHT) {
    }

    /** Montant d'une retenue et son taux écrit (montant / TTC quand il est saisi). */
    public record Retenue(BigDecimal montant, BigDecimal taux) {
    }

    public record Resultat(BigDecimal ht, BigDecimal tva, BigDecimal ttc, BigDecimal extras,
                           BigDecimal approvisionnement, BigDecimal approvisionnementHT, BigDecimal approvisionnementAPayer,
                           BigDecimal approvisionnementAPayerHT, BigDecimal fluctuationsAPayer,
                           BigDecimal fluctuationsAPayerHT, Retenue avance, Retenue garantie, Retenue prorata,
                           Retenue dec, Retenue trc, BigDecimal nonRemboursables, BigDecimal remboursables,
                           BigDecimal montantBrut, BigDecimal travauxDuMois, BigDecimal apresDeductions,
                           BigDecimal netAPayer) {
    }

    public static Resultat calculer(Entree e) {
        BigDecimal ht = somme(e.lignes(), Ligne::ht);
        BigDecimal ttc = somme(e.lignes(), Ligne::ttc);
        BigDecimal extras = somme(e.extras(), Ligne::ttc);
        Precedents p = e.precedents();
        BigDecimal base = z(p.ttc()).add(ttc);
        Retenue avance = avance(e, ttc, base);
        Retenue garantie = retenue(e.garantie(), ttc, base, p.garantie());
        Retenue prorata = retenue(e.prorata(), ttc, base, p.prorata());
        Retenue dec = assurance(e.dec(), ttc, base, p.dec());
        Retenue trc = assurance(e.trc(), ttc, base, p.trc());
        BigDecimal nonRemboursables = prorata.montant().add(dec.montant()).add(trc.montant()).add(z(e.retenuesForfaitaires()));
        BigDecimal deductions = prorata.montant().add(garantie.montant()).add(trc.montant()).add(dec.montant());
        BigDecimal appro = somme(e.approvisionnements(), Ligne::ttc);
        BigDecimal approHT = somme(e.approvisionnements(), Ligne::ht);
        BigDecimal approAPayer = appro.subtract(z(e.approvisionnementAncien())).multiply(z(e.tauxApprovisionnement()));
        BigDecimal approAPayerHT = approHT.subtract(z(e.approvisionnementAncienHT())).multiply(z(e.tauxApprovisionnement()));
        BigDecimal flucAPayer = z(e.fluctuations()).subtract(z(e.fluctuationsAnciennes()));
        BigDecimal net = NetAPayerDecompte.de(new NetAPayerDecompte.Montants(ttc, avance.montant(), e.avanceSurDecompte(),
                garantie.montant(), nonRemboursables, extras, e.remboursementProrata(), approAPayer, flucAPayer, null))
                .recalcule();
        return new Resultat(r(ht), r(ttc.subtract(ht)), r(ttc), r(extras), r(appro), r(approHT), r(approAPayer),
                r(approAPayerHT), r(flucAPayer), r(z(e.fluctuationsHT()).subtract(z(e.fluctuationsAnciennesHT()))),
                avance, garantie, prorata, dec, trc, r(nonRemboursables), garantie.montant(),
                r(ttc.add(nonRemboursables).add(extras)), r(ttc.subtract(deductions)),
                r(base.subtract(z(p.retenues())).subtract(deductions)), net);
    }

    /** « Tot. TTC » de la liste : TTC + extras + approvisionnement et fluctuations à payer. */
    public static BigDecimal totalTtc(BigDecimal ttc, BigDecimal extras, BigDecimal approvisionnement,
                                      BigDecimal fluctuations) {
        return z(ttc).add(z(extras)).add(z(approvisionnement)).add(z(fluctuations));
    }

    // W:740-818 : hors « basé sur le montant », avance cumulée plafonnée aux avances du lot (pièces MARKET_ADVANCE).
    private static Retenue avance(Entree e, BigDecimal ttc, BigDecimal base) {
        Taux t = e.avance();
        if (t.surMontant()) {
            return saisie(t, ttc);
        }
        BigDecimal cumulee = z(t.taux()).multiply(base).min(z(e.avancesDuLot()));
        return new Retenue(r(cumulee.subtract(z(e.precedents().avance()))), t.taux());
    }

    private static Retenue retenue(Taux t, BigDecimal ttc, BigDecimal base, BigDecimal precedent) {
        return t.surMontant() ? saisie(t, ttc) : new Retenue(r(z(t.taux()).multiply(base).subtract(z(precedent))), t.taux());
    }

    private static Retenue assurance(Assurance a, BigDecimal ttc, BigDecimal base, BigDecimal precedent) {
        Retenue brute = retenue(a.taux(), ttc, base, precedent);
        if (z(a.plafond()).signum() == 0 || brute.montant().compareTo(z(a.reste())) <= 0) {
            return brute;
        }
        return new Retenue(r(z(a.reste())), brute.taux());
    }

    private static Retenue saisie(Taux t, BigDecimal ttc) {
        BigDecimal montant = r(z(t.montant()));
        BigDecimal taux = ttc.signum() == 0 ? BigDecimal.ZERO : montant.divide(ttc, ECHELLE, RoundingMode.HALF_UP);
        return new Retenue(montant, taux);
    }

    private static BigDecimal somme(List<Ligne> lignes, Function<Ligne, BigDecimal> montant) {
        return lignes.stream().map(montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal r(BigDecimal v) {
        return v.setScale(ECHELLE, RoundingMode.HALF_UP);
    }

    static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
