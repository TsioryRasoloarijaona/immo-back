package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.LotsTranches;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * N° 30 SUIVI DES ACTIONS COMMERCIALES (FEATURE-tdb-vente-rapports-commercial.md §9.1), modèle OMRAN
 * {@code ComSaleDashboardSuiviActionCommerciale} : une ligne par action dont le début ou la fin tombe dans la période
 * et qui a au moins une prospection d'une tranche visible ; ratios imposés dans les totaux.
 */
@Component
public class SuiviActionsCommercialesRapport implements Rapport {

    static final String CODE = "ComSaleDashboardSuiviActionCommerciale";
    static final String SANS_ACHATS = "Montants facturé et réglé absents : les commandes d'achat ne sont pas liées "
            + "aux actions commerciales (BuyFolderId vide) ; budget initial = budget de l'action.";

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String PREFIXE = "prefixe";
    private static final String ACTION = "action";
    private static final String DEBUT = "debut";
    private static final String FIN = "fin";
    private static final String SEUIL = "seuil";
    private static final String PARAM_TRANCHES = "tranches";
    private static final String VISITES = "visites";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String CA = "caConcretise";
    private static final String PROSPECTIONS = "prospections";
    private static final String BUDGET = "budgetInitial";
    private static final String COUT_PROSPECTION = "coutParProspection";
    private static final String COUT_CONCRETISATION = "coutParConcretisation";
    private static final String PCT_CA = "pctCoutCaConcretise";
    private static final String PCT_CA_GLOBAL = "pctCoutCaGlobal";
    private static final String TAUX_CONVERSION = "tauxConversion";

    // Jointure interne sur la tranche, comme le modèle : seules les prospections d'une tranche visible comptent.
    private static final String ACTIONS = """
            SELECT a.id, a.code, a.budget, COUNT(p), SUM(CASE WHEN ty.code = :visite THEN 1 ELSE 0 END)
            FROM ComProspection p JOIN p.comAction a JOIN p.stkHierarchy t LEFT JOIN p.comActionMessageType ty
            WHERE t.id IN :tranches AND ((a.startDate >= :debut AND a.startDate < :fin)
                OR (a.endDate >= :debut AND a.endDate < :fin))
            GROUP BY a.id, a.code, a.budget""";

    // Dossier borné par la tranche de son article (comme n° 20/26), pas par celle de sa prospection.
    private static final String CONCRETISES = """
            SELECT a.id, COUNT(f), SUM(f.price) FROM ComFolder f JOIN f.comProspection p JOIN p.comAction a
            JOIN f.stkItem i JOIN i.stkHierarchy t JOIN f.strStatus s
            WHERE t.id IN :tranches AND s.sorting >= :seuil
              AND COALESCE(s.annulation, false) = false AND COALESCE(s.resiliation, false) = false
              AND ((a.startDate >= :debut AND a.startDate < :fin) OR (a.endDate >= :debut AND a.endDate < :fin))
            GROUP BY a.id""";

    // CA global : dossiers concrétisés nés de toutes les prospections datées de la période (décision R3b).
    private static final String CA_GLOBAL = """
            SELECT SUM(f.price) FROM ComFolder f JOIN f.comProspection p JOIN f.stkItem i JOIN i.stkHierarchy t
            JOIN f.strStatus s
            WHERE t.id IN :tranches AND s.sorting >= :seuil
              AND COALESCE(s.annulation, false) = false AND COALESCE(s.resiliation, false) = false
              AND p.docDate >= :debut AND p.docDate < :fin""";

    private final EntityManager em;
    private final SeuilsVente seuils;
    private final Clock clock;

    public SuiviActionsCommercialesRapport(EntityManager em, SeuilsVente seuils, Clock clock) {
        this.em = em;
        this.seuils = seuils;
        this.clock = clock;
    }

    private record Action(String code, BigDecimal budget, long prospections, long visites) {

        Action plus(Action autre) {
            return new Action(code, budget, prospections + autre.prospections, visites + autre.visites);
        }
    }

    private record Concretise(long nombre, BigDecimal ca) {

        static final Concretise AUCUN = new Concretise(0, BigDecimal.ZERO);

        Concretise plus(Concretise autre) {
            return new Concretise(nombre + autre.nombre, ca.add(autre.ca));
        }
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
        int annee = LocalDate.now(clock).getYear();
        return List.of(ParametreRapport.date(DU, "Du", LocalDate.of(annee, 1, 1)),
                ParametreRapport.date(AU, "Au", LocalDate.of(annee, 12, 31)), ParametreRapport.tranches(),
                new ParametreRapport(PREFIXE, "Code d'action commençant par", TypeParametre.TEXTE, false, false, null,
                        List.of(), null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDateTime debut = contexte.date(DU).atStartOfDay();
        LocalDateTime fin = contexte.date(AU).plusDays(1).atStartOfDay();
        String prefixe = contexte.texte(PREFIXE);
        List<UUID> tranches = contexte.tranchesVisibles();
        Map<UUID, Action> actions = new LinkedHashMap<>();
        lire(ACTIONS, tranches, debut, fin, q -> q.setParameter("visite", InteractionKpi.TYPE_VISITE))
                .forEach(r -> actions.merge((UUID) r[0], new Action((String) r[1], nonNul((BigDecimal) r[2]),
                        ((Number) r[3]).longValue(), ((Number) r[4]).longValue()), Action::plus));
        actions.values().removeIf(a -> prefixe != null && (a.code() == null
                || !a.code().regionMatches(true, 0, prefixe, 0, prefixe.length())));
        List<String> avertissements = new ArrayList<>(List.of(SANS_ACHATS));
        Integer seuil = seuils.lire().get(SeuilsVente.CONCRETISATION);
        Map<UUID, Concretise> concretises = new LinkedHashMap<>();
        BigDecimal caGlobal = BigDecimal.ZERO;
        if (seuil == null) {
            avertissements.add(SeuilsVente.absent(SeuilsVente.CONCRETISATION, "concrétisations"));
        } else {
            lire(CONCRETISES, tranches, debut, fin, q -> q.setParameter(SEUIL, seuil))
                    .forEach(r -> concretises.merge((UUID) r[0], new Concretise(((Number) r[1]).longValue(),
                            nonNul((BigDecimal) r[2])), Concretise::plus));
            caGlobal = lire(CA_GLOBAL, tranches, debut, fin, q -> q.setParameter(SEUIL, seuil)).stream()
                    .map(r -> nonNul((BigDecimal) r[0])).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Map.Entry<UUID, Action> e : actions.entrySet()) {
            lignes.add(ligne(e.getValue(), concretises.getOrDefault(e.getKey(), Concretise.AUCUN), caGlobal));
        }
        lignes.sort(Comparator.comparing(l -> String.valueOf(l.get(ACTION))));
        return new ResultatRapport(List.of(new SectionRapport("actions", "Actions commerciales", colonnes(), lignes,
                null, totaux(lignes, caGlobal), List.of())), avertissements);
    }

    private List<Object[]> lire(String jpql, List<UUID> tranches, LocalDateTime debut, LocalDateTime fin,
                                Consumer<TypedQuery<Object[]>> parametrage) {
        return LotsTranches.lire(tranches, lot -> {
            TypedQuery<Object[]> q = em.createQuery(jpql, Object[].class).setParameter(PARAM_TRANCHES, lot)
                    .setParameter(DEBUT, debut).setParameter(FIN, fin);
            parametrage.accept(q);
            return q.getResultList();
        });
    }

    private static Map<String, Object> ligne(Action a, Concretise c, BigDecimal caGlobal) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(ACTION, a.code());
        ligne.put(VISITES, a.visites());
        ligne.put(CONCRETISATIONS, c.nombre());
        ligne.put(CA, c.ca());
        ligne.put(PROSPECTIONS, a.prospections());
        ligne.put(BUDGET, a.budget());
        ligne.putAll(ratios(a.budget(), a.prospections(), c.nombre(), c.ca(), caGlobal));
        return ligne;
    }

    /** Ratios du modèle, 0 quand le dénominateur est nul ; mêmes formules pour une ligne et pour les totaux. */
    private static Map<String, Object> ratios(BigDecimal budget, long prospections, long concretisations,
                                              BigDecimal ca, BigDecimal caGlobal) {
        Map<String, Object> ratios = new LinkedHashMap<>();
        ratios.put(COUT_PROSPECTION, cout(budget, prospections));
        ratios.put(COUT_CONCRETISATION, cout(budget, concretisations));
        ratios.put(PCT_CA, SuiviGlobalVenteRapport.taux(budget, ca));
        ratios.put(PCT_CA_GLOBAL, SuiviGlobalVenteRapport.taux(budget, caGlobal));
        ratios.put(TAUX_CONVERSION, SuiviGlobalVenteRapport.taux(BigDecimal.valueOf(concretisations),
                BigDecimal.valueOf(prospections)));
        return ratios;
    }

    private static BigDecimal cout(BigDecimal budget, long nombre) {
        return nombre == 0 ? BigDecimal.ZERO : budget.divide(BigDecimal.valueOf(nombre), 2, RoundingMode.HALF_UP);
    }

    private static Map<String, Object> totaux(List<Map<String, Object>> lignes, BigDecimal caGlobal) {
        return ratios(somme(lignes, BUDGET), somme(lignes, PROSPECTIONS).longValue(),
                somme(lignes, CONCRETISATIONS).longValue(), somme(lignes, CA), caGlobal);
    }

    private static BigDecimal somme(List<Map<String, Object>> lignes, String nom) {
        return lignes.stream().map(l -> new BigDecimal(l.get(nom).toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<ColonneRapport> colonnes() {
        return List.of(ColonneRapport.texte(ACTION, "Action"),
                ColonneRapport.effectif(VISITES, "Visites", null),
                ColonneRapport.effectif(CONCRETISATIONS, "Concrétisations", null),
                ColonneRapport.montant(CA, "CA concrétisé", null),
                ColonneRapport.effectif(PROSPECTIONS, "Prospections", null),
                ColonneRapport.montant(BUDGET, "Budget initial", null),
                ratio(COUT_PROSPECTION, "Coût par prospection", TypeColonne.MONTANT),
                ratio(COUT_CONCRETISATION, "Coût par concrétisation", TypeColonne.MONTANT),
                ratio(PCT_CA, "% coût / CA concrétisé", TypeColonne.POURCENTAGE),
                ratio(PCT_CA_GLOBAL, "% coût / CA global", TypeColonne.POURCENTAGE),
                ratio(TAUX_CONVERSION, "Taux de conversion", TypeColonne.POURCENTAGE));
    }

    private static ColonneRapport ratio(String nom, String libelle, TypeColonne type) {
        return new ColonneRapport(nom, libelle, type, null, FonctionTotal.AUCUNE, true, 0);
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
