package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.stock.MouvementStock;
import com.tsix.immocrm.services.rapport.stock.MouvementsStockTranche;
import com.tsix.immocrm.services.rapport.vente.RapportFluxVente.TrancheEnVente;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.tsix.immocrm.services.rapport.vente.RapportFluxVente.TRANCHE;
import static com.tsix.immocrm.services.rapport.vente.RapportFluxVente.compteur;
import static com.tsix.immocrm.services.rapport.vente.RapportFluxVente.ligne;
import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;
import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 24 TAUX DE VENTE PAR RAPPORT AU STOCK, modèle {@code ComTdbSaleStockRatio} (FEATURE-tdb-vente-rapports-rythme.md §4.7) :
 * source unique {@link MouvementsStockTranche} ; vente = − Σ des mouvements hors lancement ; départ d'une période = Σ des
 * mouvements avant son début + lancements de la période.
 */
@Component
public class TauxVenteStockRapport implements Rapport {

    static final String CODE = "ComTdbSaleStockRatio";
    private static final String VENTES_N1 = "ventesAnneeMoins1";
    private static final String VENTES_N = "ventesAnnee";
    private static final String VENTES = "ventes";
    private static final String CUMUL_N1 = "cumulVentesAnneeMoins1";
    private static final String CUMUL_N = "cumulVentesAnnee";
    private static final String STOCK_INITIAL = "stockInitial";
    private static final String TAUX = "taux";
    private static final String DEPART = "Départ ";
    private static final String VENTES_DE = "Ventes ";

    private final MouvementsStockTranche stock;
    private final EntityManager em;
    private final Clock clock;

    public TauxVenteStockRapport(MouvementsStockTranche stock, EntityManager em, Clock clock) {
        this.stock = stock;
        this.em = em;
        this.clock = clock;
    }

    /** Départ et ventes d'une période [{@code debut}, {@code fin}[. */
    private record Periode(long depart, long ventes) {

        static Periode de(List<MouvementStock> mouvements, LocalDate debut, LocalDate fin) {
            Predicate<MouvementStock> dans = m -> !m.date().isBefore(debut) && m.date().isBefore(fin);
            long depart = mouvements.stream().filter(m -> m.date().isBefore(debut) || (m.lancement() && dans.test(m)))
                    .mapToLong(MouvementStock::quantite).sum();
            long ventes = -mouvements.stream().filter(m -> !m.lancement() && dans.test(m))
                    .mapToLong(MouvementStock::quantite).sum();
            return new Periode(depart, ventes);
        }

        static Periode annee(List<MouvementStock> mouvements, int annee) {
            return de(mouvements, LocalDate.of(annee, 1, 1), LocalDate.of(annee + 1, 1, 1));
        }

        static Periode mois(List<MouvementStock> mouvements, int annee, int mois) {
            LocalDate debut = LocalDate.of(annee, mois, 1);
            return de(mouvements, debut, debut.plusMonths(1));
        }

        BigDecimal taux() {
            return LignesMensuelles.taux(BigDecimal.valueOf(ventes), BigDecimal.valueOf(depart));
        }
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(LignesMensuelles.annee(clock), ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int n = RapportFluxVente.annee(contexte);
        List<TrancheEnVente> tranches = RapportFluxVente.tranchesEnVente(em, contexte.tranchesVisibles());
        List<MouvementStock> mouvements = stock.lire(tranches.stream().map(TrancheEnVente::id).toList());
        List<String> avertissements = new ArrayList<>(stock.avertissements());
        tranches.stream().filter(t -> !t.lancee()).forEach(t -> avertissements.add(
                "Tranche %s sans date de lancement : ses articles n'entrent jamais dans le stock.".formatted(t.code())));
        return new ResultatRapport(List.of(mois(mouvements, n), tranches(mouvements, tranches, n),
                annees(mouvements, n), cumuls(mouvements, n)), avertissements);
    }

    private static SectionRapport mois(List<MouvementStock> mouvements, int n) {
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((l, m) -> {
            Periode avant = Periode.mois(mouvements, n - 1, m);
            Periode courant = Periode.mois(mouvements, n, m);
            l.putAll(ligne("departAnneeMoins1", avant.depart(), VENTES_N1, avant.ventes(), "departAnnee",
                    courant.depart(), VENTES_N, courant.ventes(), "perfAnneeMoins1", avant.taux(), "perfAnnee",
                    courant.taux(), "totalVentes", avant.ventes() + courant.ventes()));
        });
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(MOIS, "Mois"),
                compteur("departAnneeMoins1", DEPART + (n - 1)), ColonneRapport.effectif(VENTES_N1, VENTES_DE + (n - 1), null),
                compteur("departAnnee", DEPART + n), ColonneRapport.effectif(VENTES_N, VENTES_DE + n, null),
                ColonneRapport.pourcentage("perfAnneeMoins1", "Perf " + (n - 1), null),
                ColonneRapport.pourcentage("perfAnnee", "Perf " + n, null),
                ColonneRapport.effectif("totalVentes", "Total", null));
        return new SectionRapport(MOIS, "Taux de vente par rapport au stock — années %d et %d".formatted(n - 1, n),
                colonnes, lignes, null, null, List.of(new GraphiqueRapport(TypeGraphique.COLONNES, "Ventes par mois",
                MOIS, List.of(VENTES_N1, VENTES_N))));
    }

    private static SectionRapport tranches(List<MouvementStock> mouvements, List<TrancheEnVente> tranches, int n) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (TrancheEnVente t : tranches) {
            List<MouvementStock> deT = mouvements.stream().filter(m -> t.id().equals(m.tranche())).toList();
            Periode avant = Periode.annee(deT, n - 1);
            Periode courant = Periode.annee(deT, n);
            lignes.add(ligne(TRANCHE, t.libelle(), "stockInitialAnneeMoins1", avant.depart(), VENTES_N1, avant.ventes(),
                    "stockInitialAnnee", courant.depart(), VENTES_N, courant.ventes()));
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(TRANCHE, "Tranche"),
                compteur("stockInitialAnneeMoins1", "Stock initial " + (n - 1)),
                ColonneRapport.effectif(VENTES_N1, VENTES_DE + (n - 1), null),
                compteur("stockInitialAnnee", "Stock initial " + n), ColonneRapport.effectif(VENTES_N, VENTES_DE + n, null));
        return new SectionRapport("tranches", "Analyse des concrétisations par tranche", colonnes, lignes, null, null,
                null);
    }

    private static SectionRapport annees(List<MouvementStock> mouvements, int n) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        long stocks = 0;
        long ventes = 0;
        for (int a = n - 1; a <= n; a++) {
            Periode p = Periode.annee(mouvements, a);
            lignes.add(ligne(ANNEE, String.valueOf(a), STOCK_INITIAL, p.depart(), VENTES, p.ventes(), TAUX, p.taux()));
            stocks += p.depart();
            ventes += p.ventes();
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(ANNEE, "Année"),
                compteur(STOCK_INITIAL, "Stock initial"), ColonneRapport.effectif(VENTES, "Ventes", null),
                ColonneRapport.pourcentage(TAUX, "Taux de concrétisation du stock disponible", null));
        return new SectionRapport("annees", "Ventes par année", colonnes, lignes, null,
                Map.of(TAUX, new Periode(stocks, ventes).taux()),
                List.of(new GraphiqueRapport(TypeGraphique.COLONNES, "Ventes par année", ANNEE, List.of(VENTES))));
    }

    private static SectionRapport cumuls(List<MouvementStock> mouvements, int n) {
        long[] cumul = new long[2];
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((l, m) -> {
            cumul[0] += Periode.mois(mouvements, n - 1, m).ventes();
            cumul[1] += Periode.mois(mouvements, n, m).ventes();
            l.put(CUMUL_N1, cumul[0]);
            l.put(CUMUL_N, cumul[1]);
        });
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(MOIS, "Mois"),
                compteur(CUMUL_N1, "Cumul " + (n - 1)), compteur(CUMUL_N, "Cumul " + n));
        return new SectionRapport("cumuls", "Cumul des ventes", colonnes, lignes, null, null,
                List.of(new GraphiqueRapport(TypeGraphique.LIGNES, "Cumul des ventes", MOIS, List.of(CUMUL_N1, CUMUL_N))));
    }
}
