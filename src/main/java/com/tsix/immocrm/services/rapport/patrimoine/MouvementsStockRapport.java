package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.services.rapport.stock.MouvementStock;
import com.tsix.immocrm.services.rapport.stock.MouvementsStockTranche;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TEXTE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHE;

/**
 * MOUVEMENTS DU STOCK (FEATURE-patrimoine-rapports.md §3), lus dans {@link MouvementsStockTranche} : lancement,
 * concrétisation, désistement, annulation et transferts inter-tranches. Tranches en vente seules.
 */
@Component
public class MouvementsStockRapport implements Rapport {

    static final String CODE = "StkTdbMotion";

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String MOUVEMENT = "mouvement";
    private static final String STOCK_INITIAL = "stockInitial";

    private static final Comparator<MouvementStock> ORDRE = Comparator.comparing(MouvementStock::date)
            .thenComparing(MouvementStock::libelleTranche, TEXTE).thenComparing(MouvementStock::article, TEXTE)
            .thenComparing(MouvementStock::libelle, TEXTE);

    private final MouvementsStockTranche stock;
    private final Clock clock;

    public MouvementsStockRapport(MouvementsStockTranche stock, Clock clock) {
        this.stock = stock;
        this.clock = clock;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.PATRIMOINE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate aujourdhui = LocalDate.now(clock);
        return List.of(ParametreRapport.date(DU, "Du", aujourdhui.minusMonths(1)),
                ParametreRapport.date(AU, "Au", aujourdhui), ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDate aujourdhui = LocalDate.now(clock);
        LocalDate du = Objects.requireNonNullElse(contexte.date(DU), aujourdhui.minusMonths(1));
        LocalDate au = Objects.requireNonNullElse(contexte.date(AU), aujourdhui);
        List<MouvementStock> tous = stock.lire(contexte.tranchesVisibles());
        long initial = tous.stream().filter(m -> m.date().isBefore(du)).mapToLong(MouvementStock::quantite).sum();
        List<MouvementStock> periode = tous.stream().filter(m -> !m.date().isBefore(du) && !m.date().isAfter(au))
                .sorted(ORDRE).toList();
        long entrees = periode.stream().mapToLong(MouvementStock::quantite).filter(q -> q > 0).sum();
        long sorties = periode.stream().mapToLong(MouvementStock::quantite).filter(q -> q < 0).sum();
        return new ResultatRapport(List.of(synthese(initial, entrees, sorties), mouvements(initial, periode)),
                stock.avertissements());
    }

    private static SectionRapport synthese(long initial, long entrees, long sorties) {
        List<ColonneRapport> colonnes = List.of(ColonnesStock.compteur(STOCK_INITIAL, "Stock initial"),
                ColonnesStock.compteur("entrees", "Entrées"), ColonnesStock.compteur("sorties", "Sorties"),
                ColonnesStock.compteur("stockFinal", "Stock final"));
        Map<String, Object> ligne = ColonnesStock.ligne(STOCK_INITIAL, initial, "entrees", entrees, "sorties", sorties,
                "stockFinal", initial + entrees + sorties);
        return new SectionRapport("synthese", "Synthèse", colonnes, List.of(ligne), null, null, null);
    }

    private static SectionRapport mouvements(long initial, List<MouvementStock> periode) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        long cumul = initial;
        for (MouvementStock m : periode) {
            cumul += m.quantite();
            lignes.add(ColonnesStock.ligne(TRANCHE, m.libelleTranche(), "article", m.article(), "dossier", m.dossier(),
                    "libelle", m.libelle(), "date", m.date(), MOUVEMENT, m.quantite(), "cumul", cumul));
        }
        List<ColonneRapport> colonnes = List.of(ColonnesStock.COLONNE_TRANCHE,
                ColonneRapport.texte("article", "Article"), ColonnesStock.texteMasque("dossier", "Dossier"),
                ColonneRapport.texte("libelle", "Description"),
                new ColonneRapport("date", "Date", TypeColonne.DATE, null, null, true, 0),
                ColonneRapport.effectif(MOUVEMENT, "Mouv.", null), ColonnesStock.compteur("cumul", "Cumul"));
        return new SectionRapport("mouvements", "Mouvements du stock", colonnes, lignes, null, null, null);
    }
}
