package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsFlux;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Valorisation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;

/**
 * N° 27 TAUX DE CONCRÉTISATION PAR ANNÉE / TRANCHE, modèle {@code ComSaleTauxConcretisationParAnnee}
 * (FEATURE-tdb-vente-rapports-rythme.md §4.8) : concrétisations nettes sur article racine ({@code IsUnderItem = 0}).
 */
@Component
public class TauxConcretisationAnneeRapport extends RapportFluxVente {

    static final String CODE = "ComSaleTauxConcretisationParAnnee";
    static final String RESTE = "Reste";
    private static final String TOTAL_ARTICLES = "totalArticles";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String TAUX = "taux";

    private final ValorisationTranche valorisation;

    public TauxConcretisationAnneeRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, Clock clock) {
        super(CODE, flux, clock);
        this.valorisation = valorisation;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<UUID> visibles = contexte.tranchesVisibles();
        MouvementsFlux mouvements = flux.mouvements(new DemandeFlux(visibles, null, null,
                Set.of(MesureFlux.CONCRETISATIONS_NETTES)));
        Map<UUID, List<LocalDate>> dates = mouvements.lignes().stream().filter(l -> !l.sousArticle())
                .collect(Collectors.groupingBy(MouvementFlux::trancheId,
                        Collectors.mapping(MouvementFlux::date, Collectors.toList())));
        List<Map<String, Object>> tranches = new ArrayList<>();
        List<Map<String, Object>> taux = new ArrayList<>();
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(mouvements.anomalies()));
        long totalNb = 0;
        long totalArticles = 0;
        for (Valorisation v : valorisation.pour(visibles).tranches()) {
            if (v.articles() > 0) {
                List<LocalDate> d = dates.getOrDefault(v.tranche(), List.of());
                String premiere = d.stream().min(LocalDate::compareTo)
                        .map(p -> LignesMensuelles.libelleMois(p.getMonthValue()) + " " + p.getYear()).orElse(null);
                tranches.add(ligne(TRANCHE, v.libelle(), TOTAL_ARTICLES, v.articles(), "premiereConcretisation",
                        premiere));
                totalNb += taux(v, d, taux, avertissements);
                totalArticles += v.articles();
            }
        }
        List<ColonneRapport> colonnesTranches = List.of(ColonneRapport.texte(TRANCHE, "Tranche"),
                ColonneRapport.effectif(TOTAL_ARTICLES, "Total articles", null),
                ColonneRapport.texte("premiereConcretisation", "Première concrétisation"));
        List<ColonneRapport> colonnesTaux = List.of(ColonneRapport.texte(TRANCHE, "Tranche"),
                ColonneRapport.texte(ANNEE, "Année"), ColonneRapport.effectif(NB_ARTICLES, "Articles", null),
                new ColonneRapport(TAUX, "Taux", TypeColonne.POURCENTAGE, null, FonctionTotal.SOMME, true, 0));
        return new ResultatRapport(List.of(
                new SectionRapport("tranches", "Articles par tranche", colonnesTranches, tranches, null, null, null),
                new SectionRapport(TAUX, "Taux de concrétisation par année", colonnesTaux, taux, TRANCHE,
                        Map.of(TAUX, LignesMensuelles.taux(BigDecimal.valueOf(totalNb), BigDecimal.valueOf(totalArticles))),
                        List.of(new GraphiqueRapport(TypeGraphique.COLONNES, "Concrétisations par année", ANNEE,
                                List.of(NB_ARTICLES))))), avertissements);
    }

    /** Lignes d'une tranche : une par année puis « Reste », taux ÷ total d'articles ; rend Σ des nombres. */
    private static long taux(Valorisation v, List<LocalDate> dates, List<Map<String, Object>> lignes,
                             List<String> avertissements) {
        BigDecimal total = BigDecimal.valueOf(v.articles());
        Map<Integer, Long> parAnnee = dates.stream().collect(Collectors.groupingBy(LocalDate::getYear, TreeMap::new,
                Collectors.counting()));
        long concretisees = 0;
        for (Map.Entry<Integer, Long> e : parAnnee.entrySet()) {
            lignes.add(ligne(TRANCHE, v.libelle(), ANNEE, String.valueOf(e.getKey()), NB_ARTICLES, e.getValue(), TAUX,
                    LignesMensuelles.taux(BigDecimal.valueOf(e.getValue()), total)));
            concretisees += e.getValue();
        }
        long reste = Math.max(v.articles() - concretisees, 0);
        if (concretisees > v.articles()) {
            avertissements.add("Tranche %s : plus de concrétisations (%d) que d'articles (%d), reste ramené à 0."
                    .formatted(v.libelle(), concretisees, v.articles()));
        }
        lignes.add(ligne(TRANCHE, v.libelle(), ANNEE, RESTE, NB_ARTICLES, reste, TAUX,
                LignesMensuelles.taux(BigDecimal.valueOf(reste), total)));
        return concretisees + reste;
    }
}
