package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Valorisation;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rythmes n° 9 et 10 (FEATURE-tdb-vente-rapports-rythme.md §4.2, §4.3) : par mesure, une colonne par tranche ayant un
 * mouvement = numérateur ÷ dénominateur de la tranche × 100, {@code ensemble} pondéré ; seul l'axe des lignes diffère.
 */
abstract class RythmeParTranche extends RapportFluxVente {

    static final String ENSEMBLE = "ensemble";
    static final String DECAISSEMENTS = "Décaissements non repris : les achats ventilés par tranche n'existent pas dans Arpent.";
    static final String SEUIL = "Concrétisations : seuil de statut appliqué ; un dossier sous le seuil n'est pas compté.";

    private record Mesure(String id, String titre, MesureFlux mesure, Function<Valorisation, BigDecimal> base) {
    }

    private static final List<Mesure> MESURES = List.of(
            new Mesure("concretisations", "Rythme des concrétisations", MesureFlux.CONCRETISATIONS,
                    v -> BigDecimal.valueOf(v.articles())),
            new Mesure("livraisons", "Rythme des livraisons", MesureFlux.LIVRAISONS,
                    v -> BigDecimal.valueOf(v.articles())),
            new Mesure("encaissements",
                    "Rythme des encaissements (*) — (*) sans les frais ni les pièces non comptabilisées",
                    MesureFlux.ENCAISSEMENTS_VIVANTS, Valorisation::caPotentiel));

    /** Colonne d'une tranche : nom de fil, dénominateur, mois où la mesure a un mouvement (croissants). */
    record Colonne(String nom, UUID tranche, BigDecimal base, List<YearMonth> mois) {
    }

    /** Une section : mesure, flux, colonnes dans l'ordre programme puis code de tranche. */
    record Grille(MesureFlux mesure, FluxMensuel flux, List<Colonne> colonnes) {

        BigDecimal valeur(Colonne c, YearMonth m) {
            return flux.valeur(mesure, c.tranche(), m.getYear(), m.getMonthValue());
        }

        BigDecimal base() {
            return colonnes.stream().map(Colonne::base).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /** Ligne : axe, puis la cellule de chaque colonne au mois {@code moisDe} (null = pas de mois), puis l'ensemble. */
        Map<String, Object> ligne(Map<String, Object> axe, Function<Colonne, YearMonth> moisDe) {
            Map<String, Object> ligne = new LinkedHashMap<>(axe);
            BigDecimal numerateurs = BigDecimal.ZERO;
            for (Colonne c : colonnes) {
                YearMonth m = moisDe.apply(c);
                BigDecimal v = m == null ? null : valeur(c, m);
                ligne.put(c.nom(), v == null ? null : LignesMensuelles.taux(v, c.base()));
                numerateurs = v == null ? numerateurs : numerateurs.add(v);
            }
            ligne.put(ENSEMBLE, LignesMensuelles.taux(numerateurs, base()));
            return ligne;
        }
    }

    private final ValorisationTranche valorisation;
    private final EntityManager em;

    RythmeParTranche(String code, FluxVenteMensuel flux, ValorisationTranche valorisation, EntityManager em,
                     Clock clock) {
        super(code, flux, clock);
        this.valorisation = valorisation;
        this.em = em;
    }

    /** Colonnes d'axe, avant les colonnes de tranche. */
    protected abstract List<ColonneRapport> colonnesAxe();

    protected abstract List<Map<String, Object>> lignes(Grille grille);

    /** Graphiques d'une section ayant au moins une colonne de tranche ({@code series}) ; aucun par défaut. */
    protected List<GraphiqueRapport> graphiques(List<String> series) {
        return List.of();
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        FluxMensuel f = calculer(contexte, null, null, MESURES.stream().map(Mesure::mesure).collect(Collectors.toSet()));
        ValorisationTranche.Valorisations stock = valorisation.pour(contexte.tranchesVisibles());
        Map<UUID, Valorisation> parTranche = stock.tranches().stream()
                .collect(Collectors.toMap(Valorisation::tranche, Function.identity()));
        List<TrancheEnVente> tranches = tranchesEnVente(em, contexte.tranchesVisibles());
        List<SectionRapport> sections = MESURES.stream().map(m -> section(m, f, tranches, parTranche)).toList();
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        stock.parametresAbsents().forEach(p -> avertissements.add(
                "Paramètre %s absent : les dénominateurs qui en dépendent valent 0.".formatted(p)));
        avertissements.add(SEUIL);
        avertissements.add(DECAISSEMENTS);
        return new ResultatRapport(sections, avertissements);
    }

    private SectionRapport section(Mesure m, FluxMensuel f, List<TrancheEnVente> tranches,
                                   Map<UUID, Valorisation> parTranche) {
        Map<UUID, List<YearMonth>> mois = f.cles(m.mesure()).stream().collect(Collectors.groupingBy(
                CleFlux::trancheId, Collectors.mapping(c -> YearMonth.of(c.annee(), c.mois()), Collectors.toList())));
        List<Colonne> colonnes = new ArrayList<>();
        List<ColonneRapport> entetes = new ArrayList<>(colonnesAxe());
        for (TrancheEnVente t : tranches) {
            if (mois.containsKey(t.id())) {
                String nom = "tranche%02d".formatted(colonnes.size() + 1);
                Valorisation v = parTranche.get(t.id());
                colonnes.add(new Colonne(nom, t.id(), v == null ? BigDecimal.ZERO : m.base().apply(v),
                        mois.get(t.id()).stream().distinct().sorted().toList()));
                entetes.add(ColonneRapport.pourcentage(nom, t.code(), t.projet()));
            }
        }
        entetes.add(ColonneRapport.pourcentage(ENSEMBLE, "Ensemble", null));
        Grille grille = new Grille(m.mesure(), f, colonnes);
        Map<String, Object> totaux = new LinkedHashMap<>();
        colonnes.forEach(c -> totaux.put(c.nom(), LignesMensuelles.taux(f.tranche(m.mesure(), c.tranche()), c.base())));
        BigDecimal numerateurs = colonnes.stream().map(c -> f.tranche(m.mesure(), c.tranche()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totaux.put(ENSEMBLE, LignesMensuelles.taux(numerateurs, grille.base()));
        List<String> series = colonnes.stream().map(Colonne::nom).toList();
        return new SectionRapport(m.id(), m.titre(), entetes, lignes(grille), null, totaux,
                series.isEmpty() ? List.of() : graphiques(series));
    }
}
