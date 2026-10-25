package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.flux.LecturesFlux.Etat;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock d'une tranche en vente (FEATURE-tdb-vente-rapports-flux.md §4.6, §6) : valorisation Σ {@code StkItem.SalePrice} des
 * articles VENTE (sous-articles compris), articles racines et populations de dossiers ;
 * reste à régler = {@code PRIX_RESTE} de {@link MontantsParNatureDossier}. Remplace les vues non mappées du modèle.
 */
@Service
@Transactional(readOnly = true)
public class ValorisationTranche {

    private static final String TRANCHES = "tranches";

    // Jointure externe : une tranche en vente sans article a sa ligne, à 0.
    private static final String STOCK = """
            SELECT t.PKey AS Tranche, p.Description AS Programme, t.Description AS Libelle,
                   SUM(i.SalePrice) AS PrixTotal,
                   SUM(CASE WHEN i.ComFolderId IS NULL THEN i.SalePrice END) AS PrixLibres,
                   SUM(CASE WHEN i.IsUnderItem = 0 THEN 1 ELSE 0 END) AS Articles,
                   SUM(CASE WHEN i.IsUnderItem = 0 AND i.ComFolderId IS NULL THEN 1 ELSE 0 END) AS Libres
            FROM CfgTranche t
            JOIN CfgProject p ON p.PKey = t.CfgProjectId
            LEFT JOIN StkItem i ON i.StkHierarchyId = t.PKey
                AND i.StkVocationId IN (SELECT v.PKey FROM StkVocation v WHERE v.Code = 'VENTE')
            WHERE t.IsForSale = 1 AND t.PKey IN (:tranches)
            GROUP BY t.PKey, p.Description, t.Description""";

    private static final String ARTICLES = """
            SELECT i.StkHierarchyId AS Tranche, COUNT(*) AS Articles
            FROM StkItem i
            JOIN StkVocation v ON v.PKey = i.StkVocationId AND v.Code = 'VENTE'
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId AND t.IsForSale = 1
            WHERE i.IsUnderItem = 0 AND i.StkHierarchyId IN (:tranches)""";
    private static final String PAR_TRANCHE = " GROUP BY i.StkHierarchyId";
    private static final String ARTICLES_TOUTES = ARTICLES + PAR_TRANCHE;
    private static final String ARTICLES_FILTRES = ARTICLES + " AND i.StkItemCategoryId IN (:categories)" + PAR_TRANCHE;
    private static final String CATEGORIES = "categories";

    // Statut en jointure interne : un dossier sans statut n'est dans aucune population (R-F5).
    private static final String SELECT_DOSSIERS = "SELECT i.stkHierarchy.id, f, s.id, s.sorting, s.annulation, s.resiliation ";

    // Nombres et prix : lien retour des articles racines, comme le modèle.
    private static final String DOSSIERS_ARTICLES = SELECT_DOSSIERS + """
            FROM StkItem i JOIN ComFolder f ON f.id = i.comFolderId JOIN f.strStatus s
            WHERE i.stkHierarchy.id IN :tranches AND i.isUnderItem = false AND i.stkVocation.code = 'VENTE'""";

    // Restes : article du dossier (ComFolder.StkItemId, §3.1) ; un article sans lien retour n'en perd aucun.
    private static final String DOSSIERS_TRANCHE = SELECT_DOSSIERS + """
            FROM ComFolder f JOIN f.stkItem i JOIN f.strStatus s
            WHERE i.stkHierarchy.id IN :tranches AND i.stkVocation.code = 'VENTE'""";

    /** Dossiers ni annulés ni désistés, par état (§4.6). */
    public enum Population {
        /** Au statut du paramètre {@code DefaultComFolderStatusReservation}. */
        RESERVES,
        /** Concrétisés ({@code AdvanceDate}), statut entre concrétisation (inclus) et finalisation (exclu). */
        CONCRETISES,
        /** Actés ({@code AgreementSavingDate}), statut ≥ finalisation. */
        VENDUS,
        /** Livrés ({@code DeliveryDateReal}), statut ≥ livraison. */
        LIVRES,
        /** Concrétisés ({@code AdvanceDate}), statut ≥ concrétisation, sans borne haute. */
        ENGAGES,
        /** Tous. */
        VIVANTS
    }

    /** Nombre de dossiers et Σ {@code ComFolder.Price} (lien retour d'article), Σ {@code PRIX_RESTE} (article du dossier). */
    public record Cumul(long nombre, BigDecimal prix, BigDecimal reste) {

        public static final Cumul ZERO = new Cumul(0, BigDecimal.ZERO, BigDecimal.ZERO);

        Cumul plus(Cumul autre) {
            return new Cumul(nombre + autre.nombre, prix.add(autre.prix), reste.add(autre.reste));
        }
    }

    /** Une tranche en vente : articles racines (tous, libres), valorisation (toute, libre) et populations. */
    public record Valorisation(UUID tranche, String programme, String libelle, long articles, long libres,
                               BigDecimal prixTotal, BigDecimal prixLibres, Map<Population, Cumul> populations) {

        public Cumul de(Population p) {
            return populations.getOrDefault(p, Cumul.ZERO);
        }

        /** CA potentiel (rythme n° 9) : Σ {@code ComFolder.Price} des dossiers vivants + {@link #prixLibres}. */
        public BigDecimal caPotentiel() {
            return de(Population.VIVANTS).prix().add(prixLibres);
        }
    }

    /** Tranches triées par programme puis tranche ; paramètres de statut absents (populations concernées vides). */
    public record Valorisations(List<Valorisation> tranches, List<String> parametresAbsents) {
    }

    private record Ligne(UUID tranche, ComFolder dossier, UUID statut, Etat etat) {
    }

    private record Seuils(Integer concretisation, Integer finalisation, Integer livraison, UUID reservation) {

        boolean contient(Population p, UUID statut, Etat e, ComFolder f) {
            return switch (p) {
                case RESERVES -> statut.equals(reservation);
                case CONCRETISES -> f.getAdvanceDate() != null && e.atteint(concretisation) && finalisation != null
                        && !e.atteint(finalisation);
                case VENDUS -> f.getAgreementSavingDate() != null && e.atteint(finalisation);
                case LIVRES -> f.getDeliveryDateReal() != null && e.atteint(livraison);
                case ENGAGES -> f.getAdvanceDate() != null && e.atteint(concretisation);
                case VIVANTS -> true;
            };
        }
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final EntityManager em;
    private final SeuilsStatutsVente seuils;
    private final MontantsParNatureDossier montants;

    public ValorisationTranche(NamedParameterJdbcTemplate jdbc, EntityManager em, SeuilsStatutsVente seuils,
                               MontantsParNatureDossier montants) {
        this.jdbc = jdbc;
        this.em = em;
        this.seuils = seuils;
        this.montants = montants;
    }

    /** {@code tranches} = tranches visibles ; vide ⇒ aucune requête. */
    public Valorisations pour(List<UUID> tranches) {
        if (tranches.isEmpty()) {
            return new Valorisations(List.of(), List.of());
        }
        List<String> absents = new ArrayList<>();
        Seuils s = new Seuils(lire(SeuilsStatutsVente.CONCRETISATION, seuils::sorting, absents),
                lire(SeuilsStatutsVente.FINALISATION, seuils::sorting, absents),
                lire(SeuilsStatutsVente.LIVRAISON, seuils::sorting, absents),
                lire(SeuilsStatutsVente.RESERVATION, seuils::valeur, absents));
        Map<UUID, Map<Population, Cumul>> populations = populations(tranches, s);
        List<Valorisation> lignes = LotsTranches.lire(tranches, lot -> jdbc.query(STOCK,
                Map.of(TRANCHES, LotsTranches.textes(lot)), (rs, n) -> {
                    UUID t = UUID.fromString(rs.getString("Tranche"));
                    return new Valorisation(t, rs.getString("Programme"), rs.getString("Libelle"), rs.getLong("Articles"),
                            rs.getLong("Libres"), nonNul(rs.getBigDecimal("PrixTotal")),
                            nonNul(rs.getBigDecimal("PrixLibres")), populations.getOrDefault(t, Map.of()));
                }));
        return new Valorisations(lignes.stream().sorted(Comparator
                .comparing((Valorisation v) -> Objects.toString(v.programme(), ""))
                .thenComparing(v -> Objects.toString(v.libelle(), ""))).toList(), List.copyOf(absents));
    }

    /**
     * Articles racines VENTE par tranche en vente (même définition que {@link Valorisation#articles()}), limités aux
     * catégories {@code categories} (vide = toutes) ; une tranche sans article n'a pas d'entrée.
     */
    public Map<UUID, Long> articlesParCategorie(List<UUID> tranches, Collection<UUID> categories) {
        String requete = categories.isEmpty() ? ARTICLES_TOUTES : ARTICLES_FILTRES;
        List<String> filtre = LotsTranches.textes(List.copyOf(categories));
        return LotsTranches.lire(tranches, lot -> jdbc.query(requete,
                        Map.of(TRANCHES, LotsTranches.textes(lot), CATEGORIES, filtre),
                        (rs, n) -> Map.entry(UUID.fromString(rs.getString("Tranche")), rs.getLong("Articles"))))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static <T> T lire(String parametre, Function<String, Optional<T>> lecture,
                              List<String> absents) {
        Optional<T> valeur = lecture.apply(parametre);
        if (valeur.isEmpty()) {
            absents.add(parametre);
        }
        return valeur.orElse(null);
    }

    private Map<UUID, Map<Population, Cumul>> populations(List<UUID> tranches, Seuils s) {
        List<Ligne> parArticle = vivants(DOSSIERS_ARTICLES, tranches);
        List<Ligne> parDossier = vivants(DOSSIERS_TRANCHE, tranches);
        Map<UUID, Map<ColonneDossier, BigDecimal>> restes = montants.pour(parDossier.stream().map(Ligne::dossier).toList());
        Map<UUID, Map<Population, Cumul>> parTranche = new HashMap<>();
        parArticle.forEach(l -> cumuler(parTranche, l, s,
                new Cumul(1, nonNul(l.dossier().getPrice()), BigDecimal.ZERO)));
        parDossier.forEach(l -> cumuler(parTranche, l, s, new Cumul(0, BigDecimal.ZERO,
                nonNul(restes.getOrDefault(l.dossier().getId(), Map.of()).get(ColonneDossier.PRIX_RESTE)))));
        return parTranche;
    }

    private List<Ligne> vivants(String requete, List<UUID> tranches) {
        return LotsTranches.lire(tranches, lot -> em.createQuery(requete, Object[].class)
                .setParameter(TRANCHES, lot).getResultList()).stream()
                .map(r -> new Ligne((UUID) r[0], (ComFolder) r[1], (UUID) r[2],
                        new Etat(true, (Integer) r[3], (Boolean) r[4], (Boolean) r[5])))
                .filter(l -> l.etat().vivant()).toList();
    }

    private static void cumuler(Map<UUID, Map<Population, Cumul>> parTranche, Ligne l, Seuils s, Cumul un) {
        Map<Population, Cumul> p = parTranche.computeIfAbsent(l.tranche(), k -> new EnumMap<>(Population.class));
        for (Population population : Population.values()) {
            if (s.contient(population, l.statut(), l.etat(), l.dossier())) {
                p.merge(population, un, Cumul::plus);
            }
        }
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
