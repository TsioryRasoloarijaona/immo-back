package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Seul calcul des n° 20 et n° 26 (FEATURE-tdb-vente-rapports-commercial.md §6) : par (commercial, projet), prospections
 * de la période, actions du type compté, réservations, concrétisations E, signés ou livrés G, totaux sans période ;
 * taux = (E + G) / actions × 100. Les deux rapports ne diffèrent que par {@link Variante}.
 */
@Component
class TransformationCommerciale {

    static final String NON_AFFECTE = "Non affecté";
    private static final String DU = "du";
    private static final String AU = "au";
    private static final String COMMERCIAL = "commercial";
    private static final String PROJET = "projet";
    private static final String TAUX = "taux";
    private static final String REALISATION = "realisation";
    private static final String PARAM_TRANCHES = "tranches";
    private static final String PARAM_IDS = "ids";

    /** Rang des effectifs dans le tableau de comptes d'une ligne. */
    private enum Effectif {
        PROSPECTIONS("prospections", "Prospections"),
        CONCRETISATIONS_TOTAL("concretisationsTotal", "Concrétisations (toutes dates)"),
        ACTIONS(null, null),
        RESERVATIONS("reservations", "Réservations"),
        CONCRETISATIONS("concretisations", "Concrétisations"),
        ANNULATIONS_TOTAL("annulationsTotal", "Annulations (toutes dates)"),
        SIGNES_OU_LIVRES("signesOuLivres", "Signés ou livrés");

        private final String nom;
        private final String libelle;

        Effectif(String nom, String libelle) {
            this.nom = nom;
            this.libelle = libelle;
        }
    }

    private static final List<Effectif> PAR_COMMERCIAL = List.of(Effectif.ACTIONS, Effectif.RESERVATIONS,
            Effectif.CONCRETISATIONS, Effectif.ANNULATIONS_TOTAL, Effectif.SIGNES_OU_LIVRES);

    /**
     * Ce qui distingue les deux rapports : colonne et libellé des actions comptées ; {@code parNature} (n° 26) =
     * lignes sans commercial sous « Non affecté » (exemptés) et colonne {@code realisation} (E + G).
     */
    record Variante(String colonneActions, String libelleActions, boolean parNature) {
    }

    private record Cle(UUID commercial, UUID projet) {
    }

    private record Dossier(Cle cle, LocalDate saisie, LocalDate avance, Integer ordre, boolean sorti) {
    }

    // Jointure interne sur la tranche, comme le modèle : une prospection sans tranche est absente.
    private static final String PROSPECTIONS = """
            SELECT c.id, pr.id, ty.id, COUNT(p) FROM ComProspection p JOIN p.stkHierarchy t LEFT JOIN t.cfgProject pr
            LEFT JOIN p.cfgCommercial c LEFT JOIN p.comActionMessageType ty
            WHERE t.id IN :tranches AND p.docDate >= :debut AND p.docDate < :fin GROUP BY c.id, pr.id, ty.id""";

    // Statut NULL gardé : un dossier sans statut compte en réservation (décision T6).
    private static final String DOSSIERS = """
            SELECT c.id, pr.id, f.docDate, f.advanceDate, s.sorting, s.annulation, s.resiliation
            FROM ComFolder f JOIN f.stkVocation v JOIN f.stkItem i JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr
            LEFT JOIN f.cfgCommercial c LEFT JOIN f.strStatus s WHERE t.id IN :tranches AND v.code = 'VENTE'""";

    private final EntityManager em;
    private final SeuilsVente seuils;
    private final PorteeCommerciauxRapport portee;
    private final Clock clock;

    TransformationCommerciale(EntityManager em, SeuilsVente seuils, PorteeCommerciauxRapport portee, Clock clock) {
        this.em = em;
        this.seuils = seuils;
        this.portee = portee;
        this.clock = clock;
    }

    /** {@code du} = 1er du mois, {@code au} = aujourd'hui, tranches. */
    List<ParametreRapport> parametres() {
        LocalDate aujourdhui = LocalDate.now(clock);
        return List.of(ParametreRapport.date(DU, "Du", aujourdhui.withDayOfMonth(1)),
                ParametreRapport.date(AU, "Au", aujourdhui), ParametreRapport.tranches());
    }

    ResultatRapport executer(ContexteRapport contexte, Set<UUID> types, Variante variante,
                             List<String> avertissements) {
        LocalDate du = contexte.date(DU);
        LocalDate au = contexte.date(AU);
        Predicate<UUID> retenu = portee.retenus(contexte);
        Predicate<Cle> garde = cle -> retenu.test(cle.commercial())
                && (variante.parNature() || cle.commercial() != null);
        Map<Cle, long[]> comptes = new LinkedHashMap<>();
        List<String> avis = new ArrayList<>(avertissements);
        prospections(contexte.tranchesVisibles(), du, au).forEach(r -> {
            Cle cle = new Cle((UUID) r[0], (UUID) r[1]);
            if (garde.test(cle)) {
                long[] c = comptes.computeIfAbsent(cle, k -> new long[Effectif.values().length]);
                long n = (Long) r[3];
                c[Effectif.PROSPECTIONS.ordinal()] += n;
                c[Effectif.ACTIONS.ordinal()] += r[2] != null && types.contains(r[2]) ? n : 0;
            }
        });
        Map<String, Integer> s = seuils.lire();
        Integer conc = s.get(SeuilsVente.CONCRETISATION);
        Integer fin = s.get(SeuilsVente.FINALISATION);
        if (conc == null || fin == null) {
            avis.add(SeuilsVente.absent(conc == null ? SeuilsVente.CONCRETISATION : SeuilsVente.FINALISATION,
                    "concrétisations, signés ou livrés"));
        }
        for (Dossier d : dossiers(contexte.tranchesVisibles())) {
            if (garde.test(d.cle())) {
                compter(comptes.computeIfAbsent(d.cle(), k -> new long[Effectif.values().length]), d, du, au,
                        conc, fin);
            }
        }
        comptes.values().removeIf(c -> Arrays.stream(c).allMatch(n -> n == 0));
        return new ResultatRapport(sections(comptes, variante), avis);
    }

    private static void compter(long[] c, Dossier d, LocalDate du, LocalDate au, Integer conc, Integer fin) {
        if (!d.sorti() && dans(d.saisie(), du, au)) {
            c[Effectif.RESERVATIONS.ordinal()]++;
        }
        if (d.avance() == null) {
            return;
        }
        if (d.sorti()) {
            c[Effectif.ANNULATIONS_TOTAL.ordinal()]++;
            return;
        }
        if (conc == null || fin == null || d.ordre() == null || d.ordre() < conc) {
            return;
        }
        boolean periode = dans(d.avance(), du, au);
        if (d.ordre() < fin) {
            c[Effectif.CONCRETISATIONS_TOTAL.ordinal()]++;
            c[Effectif.CONCRETISATIONS.ordinal()] += periode ? 1 : 0;
        } else {
            c[Effectif.SIGNES_OU_LIVRES.ordinal()] += periode ? 1 : 0;
        }
    }

    private static boolean dans(LocalDate jour, LocalDate du, LocalDate au) {
        return jour != null && !jour.isBefore(du) && !jour.isAfter(au);
    }

    private List<Object[]> prospections(List<UUID> tranches, LocalDate du, LocalDate au) {
        LocalDateTime debut = du.atStartOfDay();
        LocalDateTime fin = au.plusDays(1).atStartOfDay();
        return LotsTranches.lire(tranches, lot -> em.createQuery(PROSPECTIONS, Object[].class)
                .setParameter(PARAM_TRANCHES, lot).setParameter("debut", debut).setParameter("fin", fin)
                .getResultList());
    }

    private List<Dossier> dossiers(List<UUID> tranches) {
        return LotsTranches.lire(tranches, lot -> em.createQuery(DOSSIERS, Object[].class)
                .setParameter(PARAM_TRANCHES, lot).getResultList()).stream()
                .map(r -> new Dossier(new Cle((UUID) r[0], (UUID) r[1]), jour(r[2]), jour(r[3]), (Integer) r[4],
                        Boolean.TRUE.equals(r[5]) || Boolean.TRUE.equals(r[6])))
                .toList();
    }

    private static LocalDate jour(Object date) {
        return date == null ? null : ((LocalDateTime) date).toLocalDate();
    }

    // ------------------------------------------------------------------ sections

    private List<SectionRapport> sections(Map<Cle, long[]> comptes, Variante variante) {
        Map<UUID, String> commerciaux = commerciaux(comptes.keySet().stream().map(Cle::commercial)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, String> projets = projets(comptes.keySet().stream().map(Cle::projet).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Comparator<Map<String, Object>> ordre = Comparator.comparing(l -> Objects.toString(l.get(COMMERCIAL), ""));
        List<Map<String, Object>> detail = new ArrayList<>();
        Map<UUID, long[]> parCommercial = new HashMap<>();
        comptes.forEach((cle, c) -> {
            Map<String, Object> ligne = ligne(libelle(commerciaux, cle.commercial()), c, List.of(Effectif.values()),
                    variante);
            ligne.put(PROJET, projets.get(cle.projet()));
            detail.add(ligne);
            long[] total = parCommercial.computeIfAbsent(cle.commercial(), k -> new long[c.length]);
            Arrays.setAll(total, i -> total[i] + c[i]);
        });
        List<Map<String, Object>> lignes = new ArrayList<>();
        parCommercial.forEach((commercial, c) -> lignes.add(ligne(libelle(commerciaux, commercial), c,
                PAR_COMMERCIAL, variante)));
        detail.sort(ordre.thenComparing(l -> Objects.toString(l.get(PROJET), "")));
        lignes.sort(ordre);
        List<String> effectifs = variante.parNature() ? List.of(variante.colonneActions(), REALISATION)
                : PAR_COMMERCIAL.stream().map(e -> nom(e, variante)).toList();
        return List.of(
                new SectionRapport("detail", "Détail par commercial et projet",
                        colonnes(List.of(ColonneRapport.texte(COMMERCIAL, "Commercial"),
                                ColonneRapport.texte(PROJET, "Projet")), List.of(Effectif.values()), variante),
                        detail, null, totaux(detail, variante), List.of()),
                new SectionRapport("par-commercial", "Total par commercial",
                        colonnes(List.of(ColonneRapport.texte(COMMERCIAL, "Commercial")), PAR_COMMERCIAL, variante),
                        lignes, null, totaux(lignes, variante), List.of(
                        new GraphiqueRapport(TypeGraphique.COLONNES, "Taux de transformation", COMMERCIAL,
                                List.of(TAUX)),
                        new GraphiqueRapport(TypeGraphique.COLONNES, "Effectifs", COMMERCIAL, effectifs))));
    }

    private static String nom(Effectif e, Variante variante) {
        return e == Effectif.ACTIONS ? variante.colonneActions() : e.nom;
    }

    private static String libelle(Map<UUID, String> commerciaux, UUID commercial) {
        return commercial == null ? NON_AFFECTE : commerciaux.getOrDefault(commercial, commercial.toString());
    }

    /** Ligne : commercial, (projet ajouté par l'appelant), effectifs, réalisation (n° 26), taux. */
    private static Map<String, Object> ligne(String commercial, long[] c, List<Effectif> effectifs,
                                             Variante variante) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(COMMERCIAL, commercial);
        effectifs.forEach(e -> ligne.put(nom(e, variante), c[e.ordinal()]));
        long realisation = c[Effectif.CONCRETISATIONS.ordinal()] + c[Effectif.SIGNES_OU_LIVRES.ordinal()];
        if (variante.parNature()) {
            ligne.put(REALISATION, realisation);
        }
        ligne.put(TAUX, SuiviGlobalVenteRapport.taux(BigDecimal.valueOf(realisation),
                BigDecimal.valueOf(c[Effectif.ACTIONS.ordinal()])));
        return ligne;
    }

    private static List<ColonneRapport> colonnes(List<ColonneRapport> textes, List<Effectif> effectifs,
                                                 Variante variante) {
        List<ColonneRapport> colonnes = new ArrayList<>(textes);
        effectifs.forEach(e -> colonnes.add(ColonneRapport.effectif(nom(e, variante),
                e == Effectif.ACTIONS ? variante.libelleActions() : e.libelle, null)));
        if (variante.parNature()) {
            colonnes.add(ColonneRapport.effectif(REALISATION, "Réalisation", null));
        }
        colonnes.add(ColonneRapport.pourcentage(TAUX, "Taux de transformation", null));
        return colonnes;
    }

    /** Taux imposé : Σ (E + G) / Σ actions, jamais la somme des taux (RF-R3). */
    private static Map<String, Object> totaux(List<Map<String, Object>> lignes, Variante variante) {
        long actions = somme(lignes, variante.colonneActions());
        long realisation = somme(lignes, Effectif.CONCRETISATIONS.nom) + somme(lignes, Effectif.SIGNES_OU_LIVRES.nom);
        return Map.of(TAUX, SuiviGlobalVenteRapport.taux(BigDecimal.valueOf(realisation), BigDecimal.valueOf(actions)));
    }

    private static long somme(List<Map<String, Object>> lignes, String nom) {
        return lignes.stream().mapToLong(l -> (Long) l.get(nom)).sum();
    }

    private Map<UUID, String> commerciaux(Collection<UUID> ids) {
        Map<UUID, String> noms = new HashMap<>();
        LotsTranches.lire(List.copyOf(ids), lot -> em.createQuery("SELECT t FROM CfgTier t WHERE t.id IN :ids",
                CfgTier.class).setParameter(PARAM_IDS, lot).getResultList())
                .forEach(t -> noms.put(t.getId(), Objects.requireNonNullElse(TierNomFormatter.nom(t), t.getCode())));
        return noms;
    }

    private Map<UUID, String> projets(Collection<UUID> ids) {
        Map<UUID, String> libelles = new HashMap<>();
        LotsTranches.lire(List.copyOf(ids), lot -> em.createQuery(
                "SELECT p.id, p.description FROM CfgProject p WHERE p.id IN :ids", Object[].class)
                .setParameter(PARAM_IDS, lot).getResultList())
                .forEach(r -> libelles.put((UUID) r[0], (String) r[1]));
        return libelles;
    }
}
