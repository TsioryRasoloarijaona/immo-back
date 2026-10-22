package com.tsix.immocrm.domain.faisabilite;

import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Parametres;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Tranche;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneTresorerieDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.MoisTresorerieDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.MontantTrancheDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TresorerieFaisabiliteDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.r;
import static com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.z;

/**
 * Lignes de trésorerie d'une étude ({@code omran-src/Yes.Rems/Yes.Stk/StkFeasibilityStudy.cs} l.2496-2851) et courbe
 * mensuelle — FEATURE-patrimoine-faisabilite-workflow.md §2.5-2.6. Seuls les montants &gt; 0 sont gardés.
 */
public final class TresorerieFaisabilite {

    private static final MathContext MC = MathContext.DECIMAL128;

    private TresorerieFaisabilite() {
    }

    /** {@code PrevisionType} OMRAN, libellé de l'onglet « Données » et sens. */
    public enum Type {
        CONCRETISATION("Concretisation", "Concrétisation", true),
        FINALISATION("Finalization", "Finalisation", true),
        DEPENSE("Expense", "Dépense d'exécution", false),
        GARANTIE("ExpenseBackGuarantee", "Retenue de garantie", false),
        FINANCEMENT("Funding", "Financement", true),
        REMBOURSEMENT("Refunding", "Remboursement", false),
        DEPENSE_PROJET("ProjectExpense", "Dépense projet", false);

        public final String code;
        public final String libelle;
        public final boolean credit;

        Type(String code, String libelle, boolean credit) {
            this.code = code;
            this.libelle = libelle;
            this.credit = credit;
        }

        public static Type deCode(String code) {
            return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst().orElse(DEPENSE_PROJET);
        }

        boolean financement() {
            return this == FINANCEMENT || this == REMBOURSEMENT;
        }
    }

    /** {@code trancheId} = tranche de l'étude, null sans tranche. */
    public record Ligne(Type type, LocalDateTime date, BigDecimal montant, UUID trancheId) {
    }

    // ------------------------------------------------------------------ lignes (§2.5)

    public static List<Ligne> lignes(EtatFaisabilite etat, BilanFaisabilite.Resultat bilan) {
        List<Ligne> lignes = new ArrayList<>();
        bilan.noeuds().stream().filter(n -> !n.categorie().recette() && n.dateApplication() != null)
                .forEach(n -> lignes.add(new Ligne(Type.DEPENSE_PROJET, n.dateApplication(), n.propre(), null)));
        Parametres p = etat.parametres();
        if (etat.tranches().isEmpty()) {
            sansTranche(p, bilan, lignes);
        } else {
            etat.tranches().forEach(t -> parTranche(p, bilan, t, lignes));
        }
        etat.credits().forEach(c -> {
            lignes.add(new Ligne(Type.FINANCEMENT, c.date(), c.montant(), null));
            c.echeances().forEach(e -> lignes.add(new Ligne(Type.REMBOURSEMENT, e.date(), e.aRegler(), null)));
        });
        return lignes.stream().filter(l -> l.date() != null && l.montant() != null)
                .map(l -> new Ligne(l.type(), l.date(), r(l.montant()), l.trancheId()))
                .filter(l -> l.montant().signum() > 0).toList();
    }

    private static void sansTranche(Parametres p, BilanFaisabilite.Resultat b, List<Ligne> lignes) {
        if (b.recette().signum() != 0) {
            BigDecimal concretisation = z(p.pourcentageConcretisation()).multiply(b.recette(), MC);
            articles(Type.CONCRETISATION, p.debutConcretisation(), p.finConcretisation(), p.ecartTypeConcretisation(),
                    concretisation, p.nombreArticles(), null, lignes);
            articles(Type.FINALISATION, p.debutFinalisation(), p.finFinalisation(), p.ecartTypeFinalisation(),
                    b.recette().subtract(concretisation, MC), p.nombreArticles(), null, lignes);
        }
        if (b.depense().signum() != 0) {
            execution(p, b.depenseHorsIndivis(), p.debutTravaux(), p.finTravaux(), p.debutTravaux(), null, lignes);
        }
    }

    private static void parTranche(Parametres p, BilanFaisabilite.Resultat b, Tranche t, List<Ligne> lignes) {
        BigDecimal poids = z(t.poids());
        BigDecimal recette = b.recette().multiply(poids, MC);
        if (recette.signum() != 0) {
            BigDecimal concretisation = z(p.pourcentageConcretisation()).multiply(recette, MC);
            articles(Type.CONCRETISATION, t.debutCommercialisation(), t.finConcretisation(), p.ecartTypeConcretisation(),
                    concretisation, t.articles(), t.id(), lignes);
            articles(Type.FINALISATION, t.debutFinalisation(), t.finCommercialisation(), p.ecartTypeFinalisation(),
                    recette.subtract(concretisation, MC), t.articles(), t.id(), lignes);
        }
        BigDecimal depense = b.depenseHorsIndivis().multiply(poids, MC);
        if (depense.signum() != 0) {
            execution(p, depense, t.debutTravaux(), t.finTravaux(), t.finTravaux(), t.id(), lignes);
        }
    }

    /**
     * Dépense d'exécution D − D × application × retenue sur les travaux, et retenue sur [fin, fin + délai jours],
     * datée depuis {@code origineGarantie} + délai : le DÉBUT des travaux sans tranche (défaut OMRAN l.2730, décision 7a).
     */
    private static void execution(Parametres p, BigDecimal d, LocalDateTime debut, LocalDateTime fin,
                                  LocalDateTime origineGarantie, UUID tranche, List<Ligne> lignes) {
        BigDecimal garantie = d.multiply(z(p.tauxApplicationRetenue()), MC).multiply(z(p.tauxRetenue()), MC);
        montants(Type.DEPENSE, debut, RepartitionGauss.montants(RepartitionGauss.nombreDeMois(debut, fin),
                p.ecartTypeTravaux(), d.subtract(garantie, MC)), tranche, lignes);
        if (z(p.tauxRetenue()).signum() != 0 && fin != null && origineGarantie != null) {
            montants(Type.GARANTIE, origineGarantie.plusDays(p.delaiRetourJours()), RepartitionGauss.montants(
                    RepartitionGauss.nombreDeMois(fin, fin.plusDays(p.delaiRetourJours())), p.ecartTypeTravaux(), garantie),
                    tranche, lignes);
        }
    }

    private static void montants(Type type, LocalDateTime debut, List<BigDecimal> parMois, UUID tranche, List<Ligne> lignes) {
        for (int k = 0; k < parMois.size(); k++) {
            lignes.add(new Ligne(type, debut.plusMonths(k), parMois.get(k), tranche));
        }
    }

    /** Variante quantités : chaque mois vaut u × (montant / q) ; q = 0 → aucune ligne. */
    private static void articles(Type type, LocalDateTime debut, LocalDateTime fin, BigDecimal sigma, BigDecimal montant,
                                 int q, UUID tranche, List<Ligne> lignes) {
        if (montant.signum() == 0 || q <= 0) {
            return;
        }
        int[] u = RepartitionGauss.quantites(RepartitionGauss.nombreDeMois(debut, fin), sigma, q);
        BigDecimal unitaire = montant.divide(BigDecimal.valueOf(q), MC);
        List<BigDecimal> parMois = Arrays.stream(u).mapToObj(x -> r(unitaire.multiply(BigDecimal.valueOf(x), MC))).toList();
        montants(type, debut, RepartitionGauss.avecReliquat(parMois, montant), tranche, lignes);
    }

    // ------------------------------------------------------------------ courbe et « Données » (§2.6)

    /** Un point par mois du premier au dernier ; solde = cumul des crédits − débits ({@code GetCommercialTresoriePrevisions}). */
    public static TresorerieFaisabiliteDto serie(List<Ligne> toutes, Map<UUID, String> codesTranches, boolean avecFinancement) {
        List<Ligne> lignes = toutes.stream().filter(l -> avecFinancement || !l.type().financement())
                .sorted(Comparator.comparing(Ligne::date)).toList();
        List<MoisTresorerieDto> mois = new ArrayList<>();
        BigDecimal solde = BigDecimal.ZERO;
        if (!lignes.isEmpty()) {
            YearMonth fin = YearMonth.from(lignes.get(lignes.size() - 1).date());
            for (YearMonth m = YearMonth.from(lignes.get(0).date()); !m.isAfter(fin); m = m.plusMonths(1)) {
                YearMonth courant = m;
                List<Ligne> duMois = lignes.stream().filter(l -> YearMonth.from(l.date()).equals(courant)).toList();
                solde = solde.add(net(duMois));
                mois.add(new MoisTresorerieDto(m.atDay(1), somme(duMois, Type.CONCRETISATION), somme(duMois, Type.FINALISATION),
                        somme(duMois, Type.DEPENSE), somme(duMois, Type.GARANTIE), somme(duMois, Type.FINANCEMENT),
                        somme(duMois, Type.REMBOURSEMENT), somme(duMois, Type.DEPENSE_PROJET), solde,
                        tranches(duMois, codesTranches)));
            }
        }
        List<LigneTresorerieDto> donnees = lignes.stream().map(l -> new LigneTresorerieDto(l.date(), l.type().code,
                l.type().libelle, l.type().credit ? BigDecimal.ZERO : l.montant(), l.type().credit ? l.montant() : BigDecimal.ZERO,
                l.trancheId() == null ? null : codesTranches.get(l.trancheId()))).toList();
        return new TresorerieFaisabiliteDto(mois, donnees);
    }

    private static List<MontantTrancheDto> tranches(List<Ligne> duMois, Map<UUID, String> codes) {
        return codes.entrySet().stream().map(c -> {
            List<Ligne> l = duMois.stream().filter(x -> c.getKey().equals(x.trancheId())).toList();
            return new MontantTrancheDto(c.getValue(), sommeSens(l, true), sommeSens(l, false));
        }).toList();
    }

    static BigDecimal net(List<Ligne> lignes) {
        return sommeSens(lignes, true).subtract(sommeSens(lignes, false));
    }

    private static BigDecimal somme(List<Ligne> lignes, Type type) {
        return lignes.stream().filter(l -> l.type() == type).map(Ligne::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sommeSens(List<Ligne> lignes, boolean credit) {
        return lignes.stream().filter(l -> l.type().credit == credit).map(Ligne::montant)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
