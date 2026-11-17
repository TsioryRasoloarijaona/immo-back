package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesSimulation.DecompteReel;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Echeance;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Montants;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Planning;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Point;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.projet.CourbePrevisions.PRECISION;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.generer;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.maximumParMois;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.plusJours;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.redater;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

/**
 * Seul calcul des paiements prévus d'un lot (FEATURE-lot-previsions.md §3) : série « Estimation de base » du graphe (A, E1-E8)
 * et lignes de {@code PrjMarketPaymentPrevision} (B, P1-P12). Pur : les entrées sont lues par l'appelant.
 */
public final class SimulationPaiementsLot {

    /** Genre d'une ligne de prévision de paiement : code {@code StrEntity} (P12). */
    public enum Genre {
        AVANCE("PrjDocumentDebitMarketAdvance"),
        RG("PrjDocumentDebitMarketWarranty"),
        DECOMPTE("PrjInvoiceDebitWorkingOut"),
        FACTURE_MULTI("PrjInvoiceDebitMultiSupplier"),
        AVENANT("PrjInvoiceDebitMarketClause"),
        BON_COMMANDE("PrjMarketMultiSupplierOrder"),
        PRORATA("PrjDocumentDebitMarketProrataRefund");

        private final String code;

        Genre(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    /** Ligne datée ; série A : seuls {@code date} et {@code montant} sont renseignés. */
    public record Ligne(LocalDate date, String libelle, BigDecimal montant, BigDecimal ht, BigDecimal ttc,
                        BigDecimal avancement, Genre genre) {
    }

    /** Série A : lignes de paiement (E2-E6) et points d'avancement (E8), triés par date. */
    public record Estimation(List<Ligne> lignes, List<Point> avancements) {
    }

    private static final BigDecimal DEMI = new BigDecimal("0.5");
    private static final BigDecimal MOINS_UN = ONE.negate();
    private static final String RG = "Remboursement de la retenue de garantie prévisionnelle";
    private static final String PRORATA = "Remboursement du compte prorata prévisionnel Tranche ";

    /** P2 : {@code depart} = début ou premier réel, {@code dernier} = date du dernier réel ({@code D_r}). */
    private record Calendrier(LocalDate depart, LocalDate fin, int duree, LocalDate dernier, BigDecimal avancement) {
    }

    @FunctionalInterface
    private interface LigneDePoint {
        Ligne de(Point point, BigDecimal increment, int rang);
    }

    private SimulationPaiementsLot() {
    }

    // ------------------------------------------------------------------ A. Estimation de base

    public static Estimation estimation(EntreesSimulation e, LocalDate aujourdhui) {
        Planning p = e.planning();
        if (p.debutBase() == null || p.finBase() == null) {
            return new Estimation(List.of(), List.of());
        }
        List<Point> points = e.pointsBase().isEmpty() ? generer(p.debutBase(), p.finBase()) : e.pointsBase();
        List<Ligne> lignes = new ArrayList<>();
        decomptesBase(e, points, lignes);
        Montants m = e.montants();
        if (e.etat().gestionnaire() && m.prorataBase().signum() != 0) {
            BigDecimal moitie = m.prorataBase().multiply(DEMI);
            lignes.add(ligne(apresDelai(p, plusJours(p.debutBase(), BigDecimal.valueOf(p.dureeBase()).multiply(DEMI))), moitie));
            lignes.add(ligne(apresDelai(p, p.debutBase().plusDays(p.dureeBase())), moitie));
        }
        if (!e.etat().multi() && m.rgBase().signum() != 0) {
            rgBase(e, aujourdhui, lignes);
        }
        return new Estimation(lignes, avancementsBase(e, points));
    }

    /** E2 avance, E3 décomptes (multi : factures sans retenue), E4 solde. */
    private static void decomptesBase(EntreesSimulation e, List<Point> points, List<Ligne> lignes) {
        Planning p = e.planning();
        BigDecimal b = e.montants().base();
        boolean multi = e.etat().multi();
        BigDecimal a = multi ? ZERO : e.montants().avanceBase();
        if (a.signum() != 0) {
            lignes.add(ligne(apresDelai(p, p.debutBase()), a));
        }
        BigDecimal taux = b.signum() == 0 ? e.retenues().avance() : a.divide(b, PRECISION);
        DecompteSimule decompte = new DecompteSimule(taux, a, e.retenues(), false);
        UnaryOperator<BigDecimal> net = multi ? UnaryOperator.identity() : decompte::net;
        boolean annule = e.etat().annule();
        BigDecimal dernier = ZERO;
        for (Point pt : maximumParMois(points, UnaryOperator.identity())) {
            if (annule && pt.avancement().compareTo(e.montants().avancement()) > 0) {
                break;
            }
            if (pt.avancement().signum() > 0 && !pt.date().isBefore(p.debutBase())) {
                lignes.add(ligne(apresDelai(p, pt.date()), net.apply(b.multiply(pt.avancement().subtract(dernier)))));
                dernier = pt.avancement();
            }
        }
        if (!annule && dernier.compareTo(ONE) < 0) {
            lignes.add(ligne(apresDelai(p, p.finBase()), net.apply(b.multiply(ONE.subtract(dernier)))));
        }
    }

    /** E6 : une ligne à fin + mois de garantie, ou une par échéance (reste à aujourd'hui si leurs taux font moins de 1). */
    private static void rgBase(EntreesSimulation e, LocalDate aujourdhui, List<Ligne> lignes) {
        Planning p = e.planning();
        BigDecimal w = e.montants().rgBase();
        if (e.echeancesRg().isEmpty()) {
            lignes.add(ligne(apresDelai(p, p.finBase().plusMonths(moisGarantie(p))), w));
            return;
        }
        BigDecimal cumul = ZERO;
        for (Echeance ech : rameneesAUn(e.echeancesRg())) {
            cumul = cumul.add(ech.ratio());
            if (ech.ratio().signum() != 0) {
                lignes.add(ligne(apresDelai(p, dateEcheance(ech, p.finBase())), w.multiply(ech.ratio())));
            }
        }
        if (cumul.compareTo(ONE) < 0) {
            lignes.add(ligne(aujourdhui, w.multiply(ONE.subtract(cumul))));
        }
    }

    /** E8 : maximum du mois dans [0 ; 1] redaté, bornes 0 et 1 ajoutées ; annulé : plafonné à l'avancement du lot. */
    private static List<Point> avancementsBase(EntreesSimulation e, List<Point> points) {
        Planning p = e.planning();
        List<Point> liste = new ArrayList<>(maximumParMois(points, UnaryOperator.identity()).stream()
                .filter(pt -> pt.avancement().signum() >= 0 && pt.avancement().compareTo(ONE) <= 0)
                .map(pt -> new Point(redater(p.debutBase(), p.dureeBase(), pt.temps()), pt.avancement(), pt.temps()))
                .toList());
        if (liste.stream().allMatch(pt -> pt.avancement().signum() > 0)) {
            liste.add(new Point(p.debutBase(), ZERO, ZERO));
        }
        if (liste.stream().allMatch(pt -> pt.avancement().compareTo(ONE) < 0)) {
            liste.add(new Point(p.finBase(), ONE, ONE));
        }
        if (e.etat().annule()) {
            BigDecimal a = e.montants().avancement();
            liste.removeIf(pt -> pt.avancement().compareTo(a) > 0);
            liste.add(new Point(p.finBase(), a, ONE));
        }
        liste.sort(Comparator.comparing(Point::date));
        return liste;
    }

    // ------------------------------------------------------------------ B. Prévisions de paiement

    public static List<Ligne> paiements(EntreesSimulation e) {
        Planning p = e.planning();
        if (p.debut() == null) {
            return List.of();
        }
        List<Point> points = pointsPrevision(e);
        Calendrier c = calendrier(e, points);
        List<Ligne> lignes = new ArrayList<>();
        if (!e.etat().multi()) {
            nonMulti(e, points, c, lignes);
        } else if (e.montants().pourMulti().signum() >= 0 && !e.etat().termine()) {
            Montants m = e.montants();
            e.bonsCommande().forEach(bc -> lignes.add(new Ligne(bc.date(), bc.libelle(), bc.montant(), bc.ht(), bc.ttc(),
                    ZERO, Genre.BON_COMMANDE)));
            parPoint(e, points, c, c.avancement(), 1, (pt, increment, rang) -> new Ligne(pt.date(),
                    "Facture prévisionnel N° " + rang, m.coutTTC().multiply(increment), m.coutHT().multiply(increment),
                    m.coutTTC().multiply(increment), ZERO, Genre.FACTURE_MULTI), lignes);
        }
        lignes.sort(Comparator.comparing(Ligne::date));
        return lignes;
    }

    /** Points « Prévisions » stockés, ou la courbe générée s'il n'y en a aucun ; début renseigné. */
    static List<Point> pointsPrevision(EntreesSimulation e) {
        Planning p = e.planning();
        return e.points().isEmpty() ? generer(p.debut(), p.fin()) : e.points();
    }

    /** P3-P9. */
    private static void nonMulti(EntreesSimulation e, List<Point> points, Calendrier c, List<Ligne> lignes) {
        Montants m = e.montants();
        boolean actif = !e.etat().termine();
        BigDecimal existantes = e.avances().stream().reduce(ZERO, BigDecimal::add);
        BigDecimal reste = m.montant().multiply(e.retenues().avance()).max(existantes).subtract(existantes);
        List<DecompteReel> reels = actif ? e.decomptesReels() : List.of();
        BigDecimal dernier = reels.isEmpty() ? ZERO : reels.getLast().avancement();
        boolean avance = actif && dernier.signum() == 0 && reste.signum() != 0;
        if (avance) {
            LocalDate signature = e.planning().signature();
            lignes.add(new Ligne(signature != null && signature.isAfter(c.dernier()) ? signature : c.dernier(),
                    "Avance prévisionnelle", reste, ZERO, ZERO, MOINS_UN, Genre.AVANCE));
        }
        DecompteSimule decompte = new DecompteSimule(e.retenues().avance(), avance ? existantes.add(reste) : existantes,
                e.retenues(), true);
        reels.forEach(decompte::precedent);
        if (actif) {
            parPoint(e, points, c, dernier, reels.size() + 1, (pt, increment, rang) -> new Ligne(pt.date(),
                    "Décompte prévisionnel N° " + rang, decompte.net(m.montant().multiply(increment)),
                    m.coutHT().multiply(increment), m.coutTTC().multiply(increment), pt.avancement(), Genre.DECOMPTE), lignes);
        }
        e.avenants().forEach(av -> lignes.add(new Ligne(av.date(), "Avenant prévisionnel", av.montant(), ZERO, ZERO, MOINS_UN,
                Genre.AVENANT)));
        prorata(e, c, lignes);
        rg(e, c.fin(), lignes);
    }

    /** P6 / P11 : points redatés sur la durée recalculée, le plus avancé du mois, au-delà du réel. */
    private static void parPoint(EntreesSimulation e, List<Point> points, Calendrier c, BigDecimal depuis, int premier,
                                 LigneDePoint fabrique, List<Ligne> lignes) {
        LocalDate debut = e.planning().debut();
        BigDecimal dernier = depuis;
        int rang = premier;
        for (Point pt : maximumParMois(points, pt -> new Point(redater(debut, c.duree(), pt.temps()), pt.avancement(), pt.temps()))) {
            if (pt.avancement().compareTo(dernier) > 0 && pt.date().isAfter(c.dernier())) {
                lignes.add(fabrique.de(pt, pt.avancement().subtract(dernier), rang++));
                dernier = pt.avancement();
            }
        }
    }

    /** P8 : lot gestionnaire, reste du remboursement prévu en deux tranches (mi-durée et fin). */
    private static void prorata(EntreesSimulation e, Calendrier c, List<Ligne> lignes) {
        Montants m = e.montants();
        if (!e.etat().gestionnaire() || m.coutRemboursement().compareTo(m.prorataRembourse()) <= 0) {
            return;
        }
        Planning p = e.planning();
        BigDecimal reste = m.coutRemboursement().subtract(m.prorataRembourse());
        BigDecimal moitie = m.coutRemboursement().multiply(DEMI);
        if (reste.compareTo(moitie) > 0) {
            BigDecimal tranche = moitie.subtract(m.prorataRembourse());
            lignes.add(new Ligne(apresDelai(p, plusJours(c.depart(), BigDecimal.valueOf(c.duree()).multiply(DEMI))),
                    PRORATA + 1, tranche, ZERO, ZERO, MOINS_UN, Genre.PRORATA));
            reste = reste.subtract(tranche);
        }
        lignes.add(new Ligne(apresDelai(p, c.depart().plusDays(c.duree())), PRORATA + 2, reste, ZERO, ZERO, MOINS_UN,
                Genre.PRORATA));
    }

    /** P9 : RG à rembourser (hors validée), une ligne ou une par échéance, le validé déduit du cumul. */
    private static void rg(EntreesSimulation e, LocalDate fin, List<Ligne> lignes) {
        Montants m = e.montants();
        BigDecimal aRembourser = e.etat().rgBloquee() ? ZERO : m.coutRg().subtract(m.rgValidee());
        if (aRembourser.signum() <= 0) {
            return;
        }
        Planning p = e.planning();
        if (e.echeancesRg().isEmpty()) {
            lignes.add(new Ligne(apresDelai(p, fin.plusMonths(moisGarantie(p))), RG, aRembourser, ZERO, ZERO, MOINS_UN, Genre.RG));
            return;
        }
        BigDecimal cumul = ZERO;
        BigDecimal valide = m.rgValidee();
        for (Echeance ech : rameneesAUn(e.echeancesRg())) {
            BigDecimal part = m.coutRg().multiply(ech.ratio());
            cumul = cumul.add(part);
            BigDecimal montant = ZERO;
            if (valide.signum() == 0) {
                montant = part;
            } else if (cumul.compareTo(valide) > 0) {
                montant = cumul.subtract(valide);
                valide = ZERO;
            }
            if (montant.signum() != 0) {
                lignes.add(new Ligne(apresDelai(p, dateEcheance(ech, fin)), RG + " " + Objects.toString(ech.libelle(), ""),
                        montant, ZERO, ZERO, MOINS_UN, Genre.RG));
            }
        }
    }

    /** P2 : réel du lot (multi : avancement du lot et points), fin repoussée quand le réel la dépasse. */
    private static Calendrier calendrier(EntreesSimulation e, List<Point> points) {
        Planning p = e.planning();
        LocalDate depart = p.debut();
        LocalDate dernier = p.debut();
        BigDecimal avancement;
        if (e.etat().multi()) {
            avancement = e.montants().avancement();
            if (avancement.signum() > 0) {
                dernier = points.stream().filter(pt -> pt.avancement().compareTo(avancement) <= 0).map(Point::date)
                        .max(Comparator.naturalOrder()).orElse(dernier);
                depart = plusTot(depart, points.stream().map(Point::date));
            }
        } else {
            avancement = e.decomptesReels().stream().map(DecompteReel::avancement).max(Comparator.naturalOrder()).orElse(ZERO);
            dernier = e.decomptesReels().stream().map(DecompteReel::date).max(Comparator.naturalOrder()).orElse(dernier);
            depart = plusTot(depart, e.decomptesReels().stream().map(DecompteReel::date));
        }
        LocalDate fin = p.fin();
        if (dernier.isAfter(fin)) {
            fin = plusJours(dernier, ecart(e, points));
        }
        return new Calendrier(depart, fin, Math.max(p.duree(), (int) ChronoUnit.DAYS.between(p.debut(), fin)), dernier,
                avancement);
    }

    /**
     * {@code GetDeltaTime} : dernier réel − date de référence, interpolée entre les points qui encadrent l'avancement du lot
     * (OMRAN {@code Market.cs:CalculatereferenceDate}, dates en jours fractionnaires).
     */
    static BigDecimal ecart(EntreesSimulation e, List<Point> points) {
        Planning p = e.planning();
        BigDecimal a = e.montants().avancement();
        LocalDate reel = e.decomptesReels().stream().map(DecompteReel::date).max(Comparator.naturalOrder()).orElse(p.debut());
        Point bas = points.stream().filter(pt -> pt.avancement().compareTo(a) < 0)
                .max(Comparator.comparing(Point::avancement)).orElse(new Point(p.debut(), ZERO, ZERO));
        Point haut = points.stream().filter(pt -> pt.avancement().compareTo(a) >= 0)
                .min(Comparator.comparing(Point::avancement)).orElse(new Point(p.debut(), ZERO, ZERO));
        BigDecimal ecartAvancement = haut.avancement().subtract(bas.avancement());
        BigDecimal reference;
        if (ecartAvancement.signum() == 0) {
            reference = jour(a.signum() > 0 ? p.fin() : p.debut());
        } else {
            reference = jour(bas.date()).add(a.subtract(bas.avancement()).multiply(jour(haut.date()).subtract(jour(bas.date())))
                    .divide(ecartAvancement, PRECISION));
        }
        return jour(reel).subtract(reference);
    }

    // ------------------------------------------------------------------ outils

    /** Taux des échéances ramenés pour que leur somme ne dépasse pas 1 ; arrêt à 1. */
    private static List<Echeance> rameneesAUn(List<Echeance> echeances) {
        List<Echeance> ramenees = new ArrayList<>();
        BigDecimal cumul = ZERO;
        for (Echeance ech : echeances) {
            BigDecimal ratio = ech.ratio().min(ONE.subtract(cumul));
            cumul = cumul.add(ratio);
            ramenees.add(new Echeance(ratio, ech.date(), ech.delai(), ech.libelle()));
            if (cumul.compareTo(ONE) == 0) {
                break;
            }
        }
        return ramenees;
    }

    private static Ligne ligne(LocalDate date, BigDecimal montant) {
        return new Ligne(date, null, montant, null, null, null, null);
    }

    private static LocalDate apresDelai(Planning p, LocalDate date) {
        return date.plusDays(p.delaiPaiement());
    }

    private static LocalDate dateEcheance(Echeance ech, LocalDate fin) {
        return ech.date() != null ? ech.date() : fin.plusDays(ech.delai());
    }

    // (int) WarrantyInMonths d'OMRAN : partie entière, jamais plafonnée.
    private static int moisGarantie(Planning p) {
        return p.moisGarantie() == null ? 0 : p.moisGarantie().intValue();
    }

    private static LocalDate plusTot(LocalDate date, Stream<LocalDate> autres) {
        return autres.filter(d -> d.isBefore(date)).min(Comparator.naturalOrder()).orElse(date);
    }

    static BigDecimal jour(LocalDate date) {
        return BigDecimal.valueOf(date.toEpochDay());
    }
}
