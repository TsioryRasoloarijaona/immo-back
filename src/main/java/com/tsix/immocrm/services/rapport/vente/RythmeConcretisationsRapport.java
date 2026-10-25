package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 9 RYTHME DES CONCRÉTISATIONS / LIVRAISONS / ENCAISSEMENTS (Par mois), modèle {@code ComTdbSalesRythmeConcretisation}
 * (FEATURE-tdb-vente-rapports-rythme.md §4.2) : mois calendaires du premier au dernier mouvement de la mesure.
 */
@Component
public class RythmeConcretisationsRapport extends RythmeParTranche {

    static final String CODE = "ComTdbSalesRythmeConcretisation";
    private static final String PERIODE = "periode";

    public RythmeConcretisationsRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, EntityManager em,
                                        Clock clock) {
        super(CODE, flux, valorisation, em, clock);
    }

    @Override
    protected List<ColonneRapport> colonnesAxe() {
        return List.of(compteur(MOIS, "Mois"), ColonneRapport.texte(PERIODE, "Période"));
    }

    @Override
    protected List<Map<String, Object>> lignes(Grille grille) {
        List<YearMonth> tous = grille.colonnes().stream().flatMap(c -> c.mois().stream()).toList();
        List<Map<String, Object>> lignes = new ArrayList<>();
        if (tous.isEmpty()) {
            return lignes;
        }
        YearMonth fin = tous.stream().max(Comparator.naturalOrder()).orElseThrow();
        for (YearMonth m = tous.stream().min(Comparator.naturalOrder()).orElseThrow(); !m.isAfter(fin);
             m = m.plusMonths(1)) {
            Map<String, Object> axe = new LinkedHashMap<>();
            axe.put(MOIS, lignes.size() + 1);
            axe.put(PERIODE, m.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + m.getYear());
            YearMonth courant = m;
            lignes.add(grille.ligne(axe, c -> courant));
        }
        return lignes;
    }
}
