package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;

/**
 * N° 8 RYTHME DES VENTES (FEATURE-tdb-vente-rapports-rythme.md §4.1), modèle {@code ComTdbSalesRythme} : six tableaux,
 * une ligne par (tranche visible, année) de {@code annee} à l'année de l'horloge, zéros compris.
 */
@Component
public class RythmeVentesRapport extends RapportFluxVente {

    static final String CODE = "ComTdbSalesRythme";
    static final String SEUILS = "Concrétisations et contrats : seuils de statut appliqués (concrétisation, finalisation) ; "
            + "un dossier sous le seuil n'est pas compté.";
    private static final String TOTAL = "total";
    private static final int RECUL = 5;

    private record Mesure(String id, String titre, MesureFlux mesure) {
    }

    private static final List<Mesure> MESURES = List.of(
            new Mesure("reservations", "Rythme des ventes pour les réservations", MesureFlux.RESERVATIONS),
            new Mesure("concretisations", "Rythme des ventes pour les concrétisations (dont annulées)",
                    MesureFlux.CONCRETISATIONS),
            new Mesure("contrats", "Rythme des ventes pour les contrats", MesureFlux.CA_QUANTITE),
            new Mesure("livraisons", "Rythme des ventes pour les livraisons", MesureFlux.LIVRAISONS),
            new Mesure("desistements", "Rythme des ventes pour les désistements", MesureFlux.DESISTEMENTS),
            new Mesure("annulations", "Rythme des ventes pour les annulations", MesureFlux.ANNULATIONS));

    private final EntityManager em;

    public RythmeVentesRapport(FluxVenteMensuel flux, EntityManager em, Clock clock) {
        super(CODE, flux, clock);
        this.em = em;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(new ParametreRapport(ANNEE, "À partir de l'année", TypeParametre.ANNEE, false, false,
                String.valueOf(LocalDate.now(clock).getYear() - RECUL), List.of(), null, null),
                ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int debut = annee(contexte);
        int fin = LocalDate.now(clock).getYear();
        if (debut > fin) {
            FluxMensuel vide = FluxMensuel.vide(AnomaliesFlux.AUCUNE);
            return new ResultatRapport(MESURES.stream().map(m -> section(m, vide, List.of(), debut, fin)).toList(),
                    List.of(SEUILS));
        }
        FluxMensuel f = calculer(contexte, debut, fin, MESURES.stream().map(Mesure::mesure).collect(Collectors.toSet()));
        List<TrancheProjet> tranches = tranchesProjets(em, contexte.tranchesVisibles());
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        avertissements.add(SEUILS);
        return new ResultatRapport(MESURES.stream().map(m -> section(m, f, tranches, debut, fin)).toList(),
                avertissements);
    }

    private static SectionRapport section(Mesure m, FluxMensuel f, List<TrancheProjet> tranches, int debut, int fin) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (TrancheProjet t : tranches) {
            for (int annee = debut; annee <= fin; annee++) {
                Map<String, Object> ligne = new LinkedHashMap<>();
                ligne.put(TRANCHE, t.libelle());
                ligne.put(ANNEE, String.valueOf(annee));
                for (int mois = 1; mois <= 12; mois++) {
                    ligne.put("m%02d".formatted(mois), f.valeur(m.mesure(), t.tranche(), annee, mois));
                }
                ligne.put(TOTAL, f.tranche(m.mesure(), t.tranche(), annee));
                lignes.add(ligne);
            }
        }
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(TRANCHE, "Tranche"),
                ColonneRapport.texte(ANNEE, "Année")));
        for (int mois = 1; mois <= 12; mois++) {
            colonnes.add(ColonneRapport.effectif("m%02d".formatted(mois), LignesMensuelles.libelleMois(mois), null));
        }
        colonnes.add(ColonneRapport.effectif(TOTAL, "Total", null));
        return new SectionRapport(m.id(), m.titre(), colonnes, lignes, TRANCHE, null, List.of());
    }
}
