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
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * N° 13 RÉPARTITION DES CLIENTS (FEATURE-tdb-vente-rapports-socle.md §7), modèle OMRAN {@code ComTdbRepartitionClient} :
 * clients distincts (acquéreur ou co-acquéreur, tiers CLIENT) d'un dossier vendu dont l'avance tombe dans la période.
 * Écart voulu (décision 3) : naissance absente ⇒ « Non définie », pas « Moins de 20 ans ».
 */
@Component
public class RepartitionClientsRapport implements Rapport {

    static final String CODE = "ComTdbRepartitionClient";
    static final String NON_DEFINIE = "Non définie";
    static final String AUTRES = "AUTRES < 1%";
    static final String AUCUN_DOSSIER = "Aucun dossier concrétisé sur la période";
    static final List<String> AGES = List.of("Moins de 20 ans", "Entre 21 et 40 ans", "Entre 41 et 60 ans",
            "Entre 61 et 80 ans", "plus de 81 ans");

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String LIBELLE = "libelle";
    private static final String NB_CLIENTS = "nbClients";
    private static final String PART = "part";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final int SEUIL_AUTRES_POURCENT = 1;

    // Acquéreur principal et co-acquéreurs du dossier, puis filtre « tiers CLIENT » sur chacun (comme le modèle).
    private static final String CLIENTS = """
            SELECT DISTINCT tier.PKey, tier.BirthDate, pr.Description AS Profession, sa.Description AS Salaire,
                   sa.Sorting AS SalaireOrdre, ci.Description AS Ville, na.Description AS Nationalite
            FROM ComFolder f
            JOIN StrStatus s ON s.PKey = f.StrStatusId AND s.BoolField001 = 1
            JOIN StkItem i ON i.PKey = f.StkItemId AND i.IsUnderItem = 0
            JOIN StkVocation v ON v.PKey = i.StkVocationId AND v.Code = 'VENTE'
            CROSS APPLY (SELECT f.CfgCustomerId AS TierId
                         UNION SELECT fc.CfgTierId FROM ComFolderCustomer fc WHERE fc.ComFolderId = f.PKey) a
            JOIN CfgTier tier ON tier.PKey = a.TierId
            JOIN CfgTierType tt ON tt.PKey = tier.CfgTierTypeId AND tt.Code = 'CLI'
            LEFT JOIN AdmProfession pr ON pr.PKey = tier.AdmProfessionId
            LEFT JOIN AdmSalary sa ON sa.PKey = tier.AdmSalaryId
            LEFT JOIN AdmCity ci ON ci.PKey = tier.AdmCityId
            LEFT JOIN AdmNationality na ON na.PKey = tier.AdmNationalityId
            WHERE i.StkHierarchyId IN (:tranches)
              AND CAST(f.AdvanceDate AS date) >= :du AND CAST(f.AdvanceDate AS date) <= :au""";

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public RepartitionClientsRapport(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    record Client(String id, LocalDate naissance, String profession, String salaire, Integer salaireOrdre,
                  String ville, String nationalite) {
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
        LocalDate aujourdhui = LocalDate.now(clock);
        return List.of(ParametreRapport.date(DU, "Du", aujourdhui.withDayOfYear(1)),
                ParametreRapport.date(AU, "Au", aujourdhui), ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        // Bornes en date comparées à CAST(AdvanceDate AS date) : comparées au smalldatetime, elles y seraient
        // converties et « Toutes les dates » (2100-12-31) dépasserait sa limite de 2079.
        Map<String, Object> bornes = Map.of(DU, contexte.date(DU), AU, contexte.date(AU));
        Map<String, Client> parId = new LinkedHashMap<>();
        LotsTranches.lire(contexte.tranchesVisibles(), lot -> {
            Map<String, Object> parametres = new LinkedHashMap<>(bornes);
            parametres.put("tranches", LotsTranches.textes(lot));
            return jdbc.query(CLIENTS, parametres, (rs, n) -> {
                Timestamp naissance = rs.getTimestamp("BirthDate");
                int ordre = rs.getInt("SalaireOrdre");
                Integer salaireOrdre = rs.wasNull() ? null : ordre;
                return new Client(rs.getString("PKey"),
                        naissance == null ? null : naissance.toLocalDateTime().toLocalDate(),
                        rs.getString("Profession"), rs.getString("Salaire"), salaireOrdre,
                        rs.getString("Ville"), rs.getString("Nationalite"));
            });
        }).forEach(c -> parId.putIfAbsent(c.id().toUpperCase(), c));
        List<Client> clients = List.copyOf(parId.values());
        int annee = LocalDate.now(clock).getYear();
        List<SectionRapport> sections = List.of(
                section("professions", "Professions", parLibelle(clients, Client::profession)),
                section("salaires", "Salaires", parSalaire(clients)),
                section("ages", "Tranches d'âge", parAge(clients, annee)),
                section("villes", "Villes", parVille(clients)),
                section("nationalites", "Nationalités", parLibelle(clients, Client::nationalite)));
        return new ResultatRapport(sections, clients.isEmpty() ? List.of(AUCUN_DOSSIER) : List.of());
    }

    /** Effectifs par libellé (NULL ⇒ « Non définie »), dans l'ordre d'insertion. */
    private static Map<String, Long> compter(List<Client> clients, Function<Client, String> libelle) {
        Map<String, Long> comptes = new LinkedHashMap<>();
        clients.forEach(c -> comptes.merge(Objects.requireNonNullElse(libelle.apply(c), NON_DEFINIE), 1L, Long::sum));
        return comptes;
    }

    private static Map<String, Long> parLibelle(List<Client> clients, Function<Client, String> libelle) {
        Map<String, Long> tries = new LinkedHashMap<>();
        compter(clients, libelle).entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> tries.put(e.getKey(), e.getValue()));
        return tries;
    }

    private static Map<String, Long> parSalaire(List<Client> clients) {
        List<Client> tries = clients.stream().sorted(Comparator.comparing(Client::salaireOrdre,
                Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        return compter(tries, Client::salaire);
    }

    /** Âge en années civiles, comme {@code DATEDIFF(YEAR, …)} du modèle. */
    static String trancheDAge(LocalDate naissance, int annee) {
        if (naissance == null) {
            return null;
        }
        int age = annee - naissance.getYear();
        int rang = age <= 20 ? 0 : Math.min((age - 1) / 20, AGES.size() - 1);
        return AGES.get(rang);
    }

    private static Map<String, Long> parAge(List<Client> clients, int annee) {
        Map<String, Long> comptes = compter(clients, c -> trancheDAge(c.naissance(), annee));
        Map<String, Long> tries = new LinkedHashMap<>();
        for (String libelle : AGES) {
            if (comptes.containsKey(libelle)) {
                tries.put(libelle, comptes.get(libelle));
            }
        }
        if (comptes.containsKey(NON_DEFINIE)) {
            tries.put(NON_DEFINIE, comptes.get(NON_DEFINIE));
        }
        return tries;
    }

    /** Villes à moins de 1 % du total regroupées en « AUTRES < 1% » (seuil du modèle). */
    private static Map<String, Long> parVille(List<Client> clients) {
        Map<String, Long> tries = new LinkedHashMap<>();
        long autres = 0;
        for (Map.Entry<String, Long> e : parLibelle(clients, Client::ville).entrySet()) {
            // Part non arrondie, comme le modèle : arrondie à une décimale, 0,99 % passerait le seuil.
            if (e.getValue() * 100 < (long) clients.size() * SEUIL_AUTRES_POURCENT) {
                autres += e.getValue();
            } else {
                tries.put(e.getKey(), e.getValue());
            }
        }
        if (autres > 0) {
            tries.put(AUTRES, autres);
        }
        return tries;
    }

    private static BigDecimal part(long effectif, long total) {
        return BigDecimal.valueOf(effectif).multiply(CENT).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private static SectionRapport section(String id, String titre, Map<String, Long> comptes) {
        long total = comptes.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> lignes = new ArrayList<>();
        comptes.forEach((libelle, n) -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(LIBELLE, libelle);
            ligne.put(NB_CLIENTS, n);
            ligne.put(PART, part(n, total));
            lignes.add(ligne);
        });
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(LIBELLE, titre),
                ColonneRapport.effectif(NB_CLIENTS, "Clients", null), ColonneRapport.pourcentage(PART, "Part", null));
        return new SectionRapport(id, titre, colonnes, lignes, null, null,
                List.of(new GraphiqueRapport(TypeGraphique.SECTEURS, titre, LIBELLE, List.of(NB_CLIENTS))));
    }
}
