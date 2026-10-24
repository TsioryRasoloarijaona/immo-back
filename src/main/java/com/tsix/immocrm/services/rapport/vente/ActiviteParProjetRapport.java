package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


/**
 * N° 6 ACTIVITÉS COMMERCIALES PAR PROJET et PAR ANNÉE (FEATURE-tdb-vente-rapports-flux.md §4.2), modèle
 * {@code ComTdbActiviteParProjet} : une ligne par tranche ayant au moins une cellule dans l'année.
 */
@Component
public class ActiviteParProjetRapport extends RapportFluxVente {

    static final String CODE = "ComTdbActiviteParProjet";
    static final String NOTE_VERSEMENTS = "(*) Versements sans les frais ni les pièces non comptabilisées.";

    private static final String TRANCHES = """
            SELECT t.id, p.description, t.description FROM CfgTranche t LEFT JOIN t.cfgProject p WHERE t.id IN :ids""";

    private static final String CONCRETISATIONS = "concretisations";
    private static final String LIVRAISONS = "livraisons";
    private static final String DESISTEMENTS = "desistements";
    private static final String CA_TTC = "caTtc";
    private static final String VERSEMENTS = "versements";

    private static final Map<String, MesureFlux> MESURES = Map.of(CONCRETISATIONS, MesureFlux.CONCRETISATIONS,
            LIVRAISONS, MesureFlux.LIVRAISONS, DESISTEMENTS, MesureFlux.DESISTEMENTS, CA_TTC, MesureFlux.CA_TTC,
            VERSEMENTS, MesureFlux.ENCAISSEMENTS);

    private final EntityManager em;

    public ActiviteParProjetRapport(FluxVenteMensuel flux, EntityManager em, Clock clock) {
        super(CODE, flux, clock);
        this.em = em;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        FluxMensuel f = calculer(contexte, annee, annee, Set.copyOf(MESURES.values()));
        List<UUID> actives = f.cles().stream().map(CleFlux::trancheId).distinct().toList();
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Object[] t : LotsTranches.lire(actives, lot -> em.createQuery(TRANCHES, Object[].class)
                .setParameter("ids", lot).getResultList())) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(PROGRAMME, t[1]);
            ligne.put(TRANCHE, t[2]);
            MESURES.forEach((nom, m) -> ligne.put(nom, f.tranche(m, (UUID) t[0], annee)));
            lignes.add(ligne);
        }
        lignes.sort(Comparator.comparing((Map<String, Object> l) -> Objects.toString(l.get(PROGRAMME), ""))
                .thenComparing(l -> Objects.toString(l.get(TRANCHE), "")));
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        avertissements.add(NOTE_VERSEMENTS);
        return new ResultatRapport(List.of(new SectionRapport("projets",
                "Activité commerciale par projet pour l'année " + annee, colonnes(annee), lignes, null, null,
                List.of())), avertissements);
    }

    private static List<ColonneRapport> colonnes(int annee) {
        List<ColonneRapport> colonnes = new ArrayList<>(colonnesTranche());
        colonnes.addAll(List.of(ColonneRapport.effectif(CONCRETISATIONS, "Concrétisations (dont annulées)", null),
                ColonneRapport.effectif(LIVRAISONS, "Livraisons", null),
                ColonneRapport.effectif(DESISTEMENTS, "Désistements", null),
                ColonneRapport.montant(CA_TTC, "CA TTC " + annee, null),
                ColonneRapport.montant(VERSEMENTS, "Versements (*)", null)));
        return colonnes;
    }
}
