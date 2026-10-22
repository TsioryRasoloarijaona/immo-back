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
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.equipe.CommerciauxVisibles;
import com.tsix.immocrm.services.equipe.MembreEquipe;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.commercial.PeriodesRapport;
import com.tsix.immocrm.services.rapport.vente.commercial.SectionsCommerciales;
import com.tsix.immocrm.services.rapport.vente.commercial.SectionsCommerciales.LigneCommerciale;
import com.tsix.immocrm.services.suivicommerciaux.SuiviCommerciauxMensuelService;
import com.tsix.immocrm.services.suivicommerciaux.SuiviCommerciauxMensuelService.ActiviteMois;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToLongFunction;

/**
 * N° 17 SUIVI DES COMMERCIAUX (FEATURE-tdb-vente-rapports-commercial.md §4), modèle {@code ComTdbCommercialState} :
 * commercial × mois, chiffres de {@code /suivi-commerciaux} (visite à l'auteur, pas au commercial de la prospection).
 */
@Component
public class SuiviCommerciauxRapport implements Rapport {

    static final String CODE = "ComTdbCommercialState";
    static final String SANS_AUTEUR = "%d interaction(s) sans compte auteur identifié ne sont comptées pour personne.";

    private static final String MOIS = "mois";
    private static final String PROSPECTIONS = "prospections";
    private static final String VISITES = "visites";
    private static final String ENTRANTES = "entrantes";
    private static final String SORTANTES = "sortantes";

    private final SuiviCommerciauxMensuelService mensuel;
    private final CommerciauxVisibles commerciaux;
    private final Clock clock;

    public SuiviCommerciauxRapport(SuiviCommerciauxMensuelService mensuel, CommerciauxVisibles commerciaux,
                                   Clock clock) {
        this.mensuel = mensuel;
        this.commerciaux = commerciaux;
        this.clock = clock;
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
        return List.of(PeriodesRapport.annee(clock), ParametreRapport.tranches(),
                new ParametreRapport("commercialId", "Commerciaux", TypeParametre.COMMERCIAUX, false, true, null,
                        List.of(), null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = contexte.annee(PeriodesRapport.ANNEE);
        List<UUID> cles = contexte.commerciauxVisibles();
        Map<UUID, List<ActiviteMois>> parCle = new HashMap<>();
        LotsTranches.lire(contexte.tranchesVisibles(), lot -> mensuel.parMois(annee, lot, cles))
                .forEach(a -> parCle.computeIfAbsent(a.cle(), c -> new ArrayList<>()).add(a));
        // Sans commercial visible, aucune ligne : retenus(vide) rendrait tous les commerciaux.
        List<MembreEquipe> actifs = cles.isEmpty() ? List.of()
                : commerciaux.retenus(cles).stream().filter(m -> parCle.containsKey(CommerciauxVisibles.cle(m)))
                        .toList();
        long sansAuteur = LotsTranches.lire(contexte.tranchesVisibles(),
                lot -> List.of(mensuel.interactionsSansAuteur(annee, lot))).stream().mapToLong(Long::longValue).sum();
        List<ActiviteMois> toutes = parCle.values().stream().flatMap(List::stream).toList();
        return new ResultatRapport(List.of(
                matrice(PROSPECTIONS, "Prospections", actifs, parCle, ActiviteMois::prospections),
                matrice(VISITES, "Visites", actifs, parCle, ActiviteMois::visites),
                matrice("communications-entrantes", "Communications entrantes", actifs, parCle, ActiviteMois::entrantes),
                matrice("communications-sortantes", "Communications sortantes", actifs, parCle, ActiviteMois::sortantes),
                evolution(toutes)),
                sansAuteur > 0 ? List.of(SANS_AUTEUR.formatted(sansAuteur)) : List.of());
    }

    private static SectionRapport matrice(String id, String titre, List<MembreEquipe> actifs,
                                          Map<UUID, List<ActiviteMois>> parCle, ToLongFunction<ActiviteMois> valeur) {
        List<LigneCommerciale> lignes = actifs.stream().map(m -> {
            Map<String, Long> parMois = new HashMap<>();
            parCle.get(CommerciauxVisibles.cle(m)).forEach(a ->
                    parMois.merge(PeriodesRapport.colonneMois(a.mois()), valeur.applyAsLong(a), Long::sum));
            return new LigneCommerciale(m.nom(), parMois);
        }).toList();
        return SectionsCommerciales.matrice(id, titre, PeriodesRapport.colonnesMois(), lignes);
    }

    /** Douze lignes, les quatre mesures des commerciaux affichés ; deux graphiques en colonnes par mois. */
    private static SectionRapport evolution(List<ActiviteMois> activite) {
        long[][] parMois = new long[12][4];
        for (ActiviteMois a : activite) {
            long[] m = parMois[a.mois() - 1];
            m[0] += a.prospections();
            m[1] += a.visites();
            m[2] += a.entrantes();
            m[3] += a.sortantes();
        }
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(MOIS, PeriodesRapport.mois(i + 1));
            ligne.put(PROSPECTIONS, parMois[i][0]);
            ligne.put(VISITES, parMois[i][1]);
            ligne.put(ENTRANTES, parMois[i][2]);
            ligne.put(SORTANTES, parMois[i][3]);
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(MOIS, "Mois"),
                ColonneRapport.effectif(PROSPECTIONS, "Prospections", null),
                ColonneRapport.effectif(VISITES, "Visites", null),
                ColonneRapport.effectif(ENTRANTES, "Communications entrantes", null),
                ColonneRapport.effectif(SORTANTES, "Communications sortantes", null));
        return new SectionRapport("evolution", "Évolution mensuelle", colonnes, lignes, null, null, List.of(
                new GraphiqueRapport(TypeGraphique.COLONNES, "Prospections et visites", MOIS,
                        List.of(PROSPECTIONS, VISITES)),
                new GraphiqueRapport(TypeGraphique.COLONNES, "Communications", MOIS, List.of(ENTRANTES, SORTANTES))));
    }
}
