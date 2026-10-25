package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsFlux;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.taux;

/**
 * N° 23 CONCRÉTISATION ET ENCAISSEMENT (FEATURE-tdb-vente-rapports-rythme.md §4.6), modèle {@code ComTdbConcretAndSetlement} :
 * par projet, visites, concrétisations nettes et encaissements cumulés jusqu'à {@code date}, face au prévu × coefficient.
 */
@Component
public class ConcretisationEncaissementRapport extends RapportFluxVente {

    static final String CODE = "ComTdbConcretAndSetlement";
    private static final String DATE = "date";
    private static final String COEFFICIENT = "coefficient";
    private static final String PROJET = "projet";
    private static final String VISITES = "visites";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String TAUX_VISITES = "tauxVisites";
    private static final String CONCRETISATIONS_PREVUES = "concretisationsPrevues";
    private static final String TAUX_CONCRETISATION = "tauxConcretisation";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String ENCAISSEMENTS_PREVUS = "encaissementsPrevus";
    private static final String TAUX_ENCAISSEMENT = "tauxEncaissement";
    private static final LocalDate ORIGINE = LocalDate.of(1900, 1, 1);
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    /** Cumuls d'un projet. */
    private static final class Cumuls {
        private BigDecimal visites = BigDecimal.ZERO;
        private BigDecimal concretisations = BigDecimal.ZERO;
        private BigDecimal concretisationsPrevues = BigDecimal.ZERO;
        private BigDecimal encaissements = BigDecimal.ZERO;
        private BigDecimal encaissementsPrevus = BigDecimal.ZERO;
    }

    private final EntityManager em;
    private final TransformationCommerciale transformation;

    public ConcretisationEncaissementRapport(FluxVenteMensuel flux, EntityManager em,
                                             TransformationCommerciale transformation, Clock clock) {
        super(CODE, flux, clock);
        this.em = em;
        this.transformation = transformation;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.date(DATE, "Au", LocalDate.now(clock)), ParametreRapport.tranches(),
                new ParametreRapport(COEFFICIENT, "Coefficient de prévision (%)", TypeParametre.ENTIER, false, false,
                        "100", List.of(), 0, 1000));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDate au = Objects.requireNonNull(contexte.date(DATE), "Paramètre date absent");
        BigDecimal coefficient = BigDecimal.valueOf(Objects.requireNonNull(contexte.entier(COEFFICIENT),
                "Paramètre coefficient absent")).divide(CENT);
        List<UUID> visibles = contexte.tranchesVisibles();
        Map<UUID, UUID> projetDe = new HashMap<>();
        Map<UUID, String> projets = new HashMap<>();
        tranchesProjets(em, visibles).stream().filter(t -> t.projet() != null).forEach(t -> {
            projetDe.put(t.tranche(), t.projet());
            projets.put(t.projet(), t.libelleProjet());
        });
        Map<UUID, Cumuls> cumuls = new HashMap<>();
        projets.keySet().forEach(p -> cumuls.put(p, new Cumuls()));
        visites(visibles, au, cumuls);
        MouvementsFlux realise = realise(visibles, au, projetDe, cumuls);
        FluxMensuel prevu = calculer(contexte, au.getYear(), au.getYear(),
                Set.of(MesureFlux.BASE_CONCRETISATION_QUANTITE, MesureFlux.BASE_ENCAISSEMENT));
        projetDe.forEach((tranche, projet) -> prevus(prevu, tranche, au, cumuls.get(projet)));
        List<Map<String, Object>> lignes = projets.entrySet().stream()
                .sorted(Comparator.comparing(e -> Objects.toString(e.getValue(), "")))
                .map(e -> ligne(e.getValue(), cumuls.get(e.getKey()), coefficient)).toList();
        AnomaliesFlux a = realise.anomalies();
        return new ResultatRapport(List.of(section(lignes)), LignesMensuelles.avertissements(new AnomaliesFlux(
                a.dossiersSansStatut(), a.reglementsSansDate(), a.parametresAbsents(),
                prevu.anomalies().prevuNonSaisi())));
    }

    /** Prospections de type Visite jusqu'à {@code au} inclus, population de {@link TransformationCommerciale}. */
    private void visites(List<UUID> tranches, LocalDate au, Map<UUID, Cumuls> cumuls) {
        Set<UUID> types = Set.copyOf(em.createQuery("SELECT t.id FROM ComActionMessageType t WHERE t.code = :code",
                UUID.class).setParameter("code", InteractionKpi.TYPE_VISITE).getResultList());
        for (Object[] r : transformation.prospections(tranches, ORIGINE, au)) {
            Cumuls c = cumuls.get((UUID) r[1]);
            if (c != null && r[2] != null && types.contains(r[2])) {
                c.visites = c.visites.add(BigDecimal.valueOf((Long) r[3]));
            }
        }
    }

    /** Concrétisations nettes et encaissements du flux, datés au jour, jusqu'à {@code au} inclus. */
    private MouvementsFlux realise(List<UUID> tranches, LocalDate au, Map<UUID, UUID> projetDe,
                                   Map<UUID, Cumuls> cumuls) {
        MouvementsFlux realise = flux.mouvements(new DemandeFlux(tranches, null, au.getYear(),
                Set.of(MesureFlux.CONCRETISATIONS_NETTES, MesureFlux.ENCAISSEMENTS)));
        realise.lignes().stream().filter(l -> !l.date().isAfter(au) && projetDe.containsKey(l.trancheId()))
                .forEach(l -> {
                    Cumuls c = cumuls.get(projetDe.get(l.trancheId()));
                    if (l.mesure() == MesureFlux.ENCAISSEMENTS) {
                        c.encaissements = c.encaissements.add(l.valeur());
                    } else {
                        c.concretisations = c.concretisations.add(l.valeur());
                    }
                });
        return realise;
    }

    /** Prévu de l'année de {@code au}, mois 1 au mois de {@code au}. */
    private static void prevus(FluxMensuel prevu, UUID tranche, LocalDate au, Cumuls c) {
        for (int mois = 1; mois <= au.getMonthValue(); mois++) {
            c.concretisationsPrevues = c.concretisationsPrevues.add(
                    prevu.valeur(MesureFlux.BASE_CONCRETISATION_QUANTITE, tranche, au.getYear(), mois));
            c.encaissementsPrevus = c.encaissementsPrevus.add(
                    prevu.valeur(MesureFlux.BASE_ENCAISSEMENT, tranche, au.getYear(), mois));
        }
    }

    private static Map<String, Object> ligne(String projet, Cumuls c, BigDecimal coefficient) {
        BigDecimal concretisationsPrevues = c.concretisationsPrevues.multiply(coefficient).setScale(0, RoundingMode.HALF_UP);
        BigDecimal encaissementsPrevus = c.encaissementsPrevus.multiply(coefficient);
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(PROJET, projet);
        ligne.put(VISITES, c.visites);
        ligne.put(CONCRETISATIONS, c.concretisations);
        ligne.put(TAUX_VISITES, taux(c.concretisations, c.visites));
        ligne.put(CONCRETISATIONS_PREVUES, concretisationsPrevues);
        ligne.put(TAUX_CONCRETISATION, taux(c.concretisations, concretisationsPrevues));
        ligne.put(ENCAISSEMENTS, c.encaissements);
        ligne.put(ENCAISSEMENTS_PREVUS, encaissementsPrevus);
        ligne.put(TAUX_ENCAISSEMENT, taux(c.encaissements, encaissementsPrevus));
        return ligne;
    }

    private static SectionRapport section(List<Map<String, Object>> lignes) {
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(PROJET, "Projet"),
                ColonneRapport.effectif(VISITES, "Visites", null),
                ColonneRapport.effectif(CONCRETISATIONS, "Concrétisations", null),
                ColonneRapport.pourcentage(TAUX_VISITES, "Taux / visites", null),
                ColonneRapport.effectif(CONCRETISATIONS_PREVUES, "Concrétisations prévues", null),
                ColonneRapport.pourcentage(TAUX_CONCRETISATION, "Taux de concrétisation", null),
                ColonneRapport.montant(ENCAISSEMENTS, "Encaissements", null),
                ColonneRapport.montant(ENCAISSEMENTS_PREVUS, "Encaissements prévus", null),
                ColonneRapport.pourcentage(TAUX_ENCAISSEMENT, "Taux d'encaissement", null));
        Map<String, Object> totaux = Map.of(
                TAUX_VISITES, taux(somme(lignes, CONCRETISATIONS), somme(lignes, VISITES)),
                TAUX_CONCRETISATION, taux(somme(lignes, CONCRETISATIONS), somme(lignes, CONCRETISATIONS_PREVUES)),
                TAUX_ENCAISSEMENT, taux(somme(lignes, ENCAISSEMENTS), somme(lignes, ENCAISSEMENTS_PREVUS)));
        return new SectionRapport("projets", "Concrétisation et encaissement", colonnes, lignes, null, totaux, List.of(
                new GraphiqueRapport(TypeGraphique.COLONNES, "Concrétisations", PROJET,
                        List.of(CONCRETISATIONS, CONCRETISATIONS_PREVUES)),
                new GraphiqueRapport(TypeGraphique.COLONNES, "Encaissements", PROJET,
                        List.of(ENCAISSEMENTS, ENCAISSEMENTS_PREVUS))));
    }

    private static BigDecimal somme(List<Map<String, Object>> lignes, String colonne) {
        return lignes.stream().map(l -> (BigDecimal) l.get(colonne)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
