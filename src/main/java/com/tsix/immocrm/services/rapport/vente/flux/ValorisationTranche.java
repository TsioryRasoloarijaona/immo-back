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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Stock d'une tranche en vente (FEATURE-tdb-vente-rapports-flux.md §4.6, §6) : valorisation Σ {@code StkItem.SalePrice} des
 * articles VENTE (sous-articles compris), articles racines et populations de leurs dossiers ({@code StkItem.ComFolderId}) ;
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

    // Statut en jointure interne : un dossier sans statut n'est dans aucune population (R-F5).
    private static final String DOSSIERS = """
            SELECT i.stkHierarchy.id, f, s.id, s.sorting, s.annulation, s.resiliation
            FROM StkItem i JOIN ComFolder f ON f.id = i.comFolderId JOIN f.strStatus s
            WHERE i.stkHierarchy.id IN :tranches AND i.isUnderItem = false AND i.stkVocation.code = 'VENTE'""";

    /** Dossiers ni annulés ni désistés d'un article racine, par état (§4.6). */
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

    /** Nombre de dossiers, Σ {@code ComFolder.Price}, Σ {@code PRIX_RESTE}. */
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

    private static <T> T lire(String parametre, Function<String, Optional<T>> lecture,
                              List<String> absents) {
        Optional<T> valeur = lecture.apply(parametre);
        if (valeur.isEmpty()) {
            absents.add(parametre);
        }
        return valeur.orElse(null);
    }

    private Map<UUID, Map<Population, Cumul>> populations(List<UUID> tranches, Seuils s) {
        List<Ligne> lignes = LotsTranches.lire(tranches, lot -> em.createQuery(DOSSIERS, Object[].class)
                .setParameter(TRANCHES, lot).getResultList()).stream()
                .map(r -> new Ligne((UUID) r[0], (ComFolder) r[1], (UUID) r[2],
                        new Etat(true, (Integer) r[3], (Boolean) r[4], (Boolean) r[5])))
                .filter(l -> l.etat().vivant()).toList();
        Map<UUID, Map<ColonneDossier, BigDecimal>> restes = montants.pour(lignes.stream().map(Ligne::dossier).toList());
        Map<UUID, Map<Population, Cumul>> parTranche = new HashMap<>();
        for (Ligne l : lignes) {
            ComFolder f = l.dossier();
            Cumul un = new Cumul(1, nonNul(f.getPrice()),
                    nonNul(restes.getOrDefault(f.getId(), Map.of()).get(ColonneDossier.PRIX_RESTE)));
            Map<Population, Cumul> p = parTranche.computeIfAbsent(l.tranche(), k -> new EnumMap<>(Population.class));
            for (Population population : Population.values()) {
                if (s.contient(population, l.statut(), l.etat(), f)) {
                    p.merge(population, un, Cumul::plus);
                }
            }
        }
        return parTranche;
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
