package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;
import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 3 COMPARAISON DES VENTES (FEATURE-tdb-vente-rapports-flux.md §4.4), modèle {@code ComTdbSaleComparaison} : une tranche,
 * base / prévision / réel mois par mois, seulement les mois où la mesure a une cellule.
 */
@Component
public class ComparaisonVentesRapport extends RapportFluxVente {

    static final String CODE = "ComTdbSaleComparaison";
    private static final String BASE = "base";
    private static final String PREVISION = "prevision";
    private static final String REEL = "reel";

    private record Section(String id, String titre, MesureFlux base, MesureFlux prevision, MesureFlux reel) {

        Stream<MesureFlux> mesures() {
            return Stream.of(base, prevision, reel);
        }
    }

    private static final List<Section> SECTIONS = List.of(
            new Section("chiffre-affaires", "Comparaisons du chiffre d'affaire — Chiffre d'affaires TTC",
                    MesureFlux.BASE_CA, MesureFlux.PREVU_CA_TTC, MesureFlux.CA_TTC),
            new Section("encaissements", "Comparaisons des encaissements — Encaissement (*)",
                    MesureFlux.BASE_ENCAISSEMENT, MesureFlux.PREVU_ENCAISSEMENT, MesureFlux.ENCAISSEMENTS));

    public ComparaisonVentesRapport(FluxVenteMensuel flux, Clock clock) {
        super(CODE, flux, clock);
    }

    /** Une seule tranche, obligatoire ; année facultative (vide = toutes les années, comme le modèle). */
    @Override
    public List<ParametreRapport> parametres() {
        return List.of(new ParametreRapport(ParametreRapport.TRANCHE_ID, "Tranche", TypeParametre.TRANCHES, true, false,
                        null, List.of(), null, null),
                new ParametreRapport(ANNEE, "Année (vide = toutes)", TypeParametre.ANNEE, false, false, null, List.of(),
                        null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Integer annee = contexte.annee(ANNEE);
        Set<MesureFlux> mesures = EnumSet.noneOf(MesureFlux.class);
        SECTIONS.forEach(s -> s.mesures().forEach(mesures::add));
        FluxMensuel f = calculer(contexte, annee, annee, mesures);
        return new ResultatRapport(SECTIONS.stream().map(s -> section(s, f)).toList(),
                LignesMensuelles.avertissements(f));
    }

    private static SectionRapport section(Section s, FluxMensuel f) {
        List<Map<String, Object>> lignes = s.mesures().flatMap(m -> f.cles(m).stream())
                .map(c -> new CleFlux(null, c.annee(), c.mois())).distinct()
                .sorted(Comparator.comparingInt(CleFlux::annee).thenComparingInt(CleFlux::mois))
                .map(c -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put(ANNEE, String.valueOf(c.annee()));
                    ligne.put(MOIS, LignesMensuelles.libelleMois(c.mois()) + " " + c.annee());
                    ligne.put(BASE, f.mois(s.base(), c.annee(), c.mois()));
                    ligne.put(PREVISION, f.mois(s.prevision(), c.annee(), c.mois()));
                    ligne.put(REEL, f.mois(s.reel(), c.annee(), c.mois()));
                    return ligne;
                }).toList();
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(ANNEE, "Année"), ColonneRapport.texte(MOIS, "Mois"),
                ColonneRapport.montant(BASE, "Base", null), ColonneRapport.montant(PREVISION, "Prévision", null),
                ColonneRapport.montant(REEL, "Réel", null));
        return new SectionRapport(s.id(), s.titre(), colonnes, lignes, ANNEE, null,
                List.of(new GraphiqueRapport(TypeGraphique.COLONNES, s.titre(), MOIS, List.of(BASE, PREVISION, REEL))));
    }
}
