package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 10 RYTHME … (Comparaison), modèle {@code ComTdbSalesRythmeLivraison} (FEATURE-tdb-vente-rapports-rythme.md §4.3) :
 * ligne r = r-ième mois actif de chaque tranche (rang dense, Q7) ; une tranche sans r-ième mois a une cellule vide.
 */
@Component
public class RythmeComparaisonRapport extends RythmeParTranche {

    static final String CODE = "ComTdbSalesRythmeLivraison";

    public RythmeComparaisonRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, EntityManager em,
                                    Clock clock) {
        super(CODE, flux, valorisation, em, clock);
    }

    @Override
    protected List<ColonneRapport> colonnesAxe() {
        return List.of(ColonneRapport.texte(MOIS, "Mois"));
    }

    @Override
    protected List<Map<String, Object>> lignes(Grille grille) {
        int rangs = grille.colonnes().stream().mapToInt(c -> c.mois().size()).max().orElse(0);
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (int r = 1; r <= rangs; r++) {
            int rang = r;
            lignes.add(grille.ligne(Map.of(MOIS, "Mois " + r),
                    c -> rang <= c.mois().size() ? c.mois().get(rang - 1) : null));
        }
        return lignes;
    }

    @Override
    protected List<GraphiqueRapport> graphiques(List<String> series) {
        return List.of(new GraphiqueRapport(TypeGraphique.LIGNES, "Rythme comparé des tranches", MOIS, series));
    }
}
