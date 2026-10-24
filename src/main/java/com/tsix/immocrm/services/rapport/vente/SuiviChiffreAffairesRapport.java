package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.PrevisionsDepuisDossiers;
import com.tsix.immocrm.services.rapport.vente.flux.PrevisionsDepuisDossiers.Prevision;
import com.tsix.immocrm.services.rapport.vente.flux.PrevisionsDepuisDossiers.Previsions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 15 CHIFFRE D'AFFAIRE (FEATURE-tdb-vente-rapports-flux.md §4.7), modèle {@code ComTdbChiffreAffaire} : prévus tirés des
 * dossiers ({@link PrevisionsDepuisDossiers}), réels du flux ; montants en millions de MAD, deux décimales.
 */
@Component
public class SuiviChiffreAffairesRapport extends RapportFluxVente {

    static final String CODE = "ComTdbChiffreAffaire";
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final String PREVU = "prevu";
    private static final String REEL = "reel";

    /** Une colonne : nom, libellé, valeur du mois ; {@code millions} = montant converti, sinon quantité. */
    private record Serie(String nom, String libelle, boolean millions, IntFunction<BigDecimal> valeur) {
    }

    private final PrevisionsDepuisDossiers previsions;

    public SuiviChiffreAffairesRapport(FluxVenteMensuel flux, PrevisionsDepuisDossiers previsions, Clock clock) {
        super(CODE, flux, clock);
        this.previsions = previsions;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        FluxMensuel f = calculer(contexte, annee, annee,
                EnumSet.of(MesureFlux.CA_TTC, MesureFlux.ENCAISSEMENTS_APRES_ACTE));
        Previsions p = previsions.pour(contexte.tranchesVisibles(), annee);
        List<SectionRapport> sections = List.of(
                section("ca-notaire", "Prévision du chiffre d'affaires pour les dossiers avec envoi au notaire pour l'année "
                        + annee, List.of(new Serie(PREVU, "Prévu", true, m -> p.mois(Prevision.CA_NOTAIRE, m)),
                        new Serie(REEL, "Réel", true, m -> f.mois(MesureFlux.CA_TTC, annee, m)))),
                section("encaissements-reste", "Prévision des encaissements (*) du reste à régler pour l'année " + annee,
                        List.of(new Serie(PREVU, "Prévu", true, m -> p.mois(Prevision.RESTE_APRES_ACTE, m)),
                                new Serie(REEL, "Réel", true,
                                        m -> f.mois(MesureFlux.ENCAISSEMENTS_APRES_ACTE, annee, m)))),
                section("concretisation-prevue", "Prévision du CA pour les dossiers en concrétisation, basée sur la date "
                        + "d'envoi au notaire prévisionnelle + délai notaire", List.of(
                        new Serie("prevuQuantite", "Prévu (nombre)", false,
                                m -> p.mois(Prevision.CONCRETISATION_PREVUE_NOMBRE, m)),
                        new Serie("prevuMontant", "Prévu (MDH)", true,
                                m -> p.mois(Prevision.CONCRETISATION_PREVUE_MONTANT, m)),
                        new Serie("reelQuantite", "Réel confirmé (nombre)", false,
                                m -> p.mois(Prevision.CONCRETISATION_CONFIRMEE_NOMBRE, m)),
                        new Serie("reelMontant", "Réel confirmé (MDH)", true,
                                m -> p.mois(Prevision.CONCRETISATION_CONFIRMEE_MONTANT, m)))));
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        avertissements.addAll(p.avertissements());
        avertissements.add("Montants en millions de MAD (MILLION DH).");
        return new ResultatRapport(sections, avertissements);
    }

    private static SectionRapport section(String id, String titre, List<Serie> series) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(MOIS, "Mois")));
        series.forEach(s -> colonnes.add(new ColonneRapport(s.nom(), s.libelle(), TypeColonne.NOMBRE, null,
                FonctionTotal.SOMME, true, s.millions() ? 2 : 0)));
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((ligne, mois) -> series.forEach(s -> {
            BigDecimal v = s.valeur().apply(mois);
            ligne.put(s.nom(), s.millions() ? v.divide(MILLION, 2, RoundingMode.HALF_UP) : v);
        }));
        return new SectionRapport(id, titre, colonnes, lignes, null, null, List.of(new GraphiqueRapport(
                TypeGraphique.COLONNES, titre, MOIS, series.stream().filter(Serie::millions).map(Serie::nom).toList())));
    }
}
