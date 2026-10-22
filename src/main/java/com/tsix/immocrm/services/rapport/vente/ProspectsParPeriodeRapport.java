package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.commercial.PeriodesRapport;
import com.tsix.immocrm.services.rapport.vente.commercial.SectionsCommerciales;
import com.tsix.immocrm.services.rapport.vente.commercial.SectionsCommerciales.LigneCommerciale;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * N° 25 PROSPECTS PAR PÉRIODE (FEATURE-tdb-vente-rapports-commercial.md §7), modèle {@code ComTdbProspectsPerCommercial} :
 * tiers PROSPECT et CLIENT créés, par commercial affecté, par semaine ISO, mois et année. Pas de tranche : la
 * visibilité passe par les commerciaux ; « Non affecté » réservé aux rôles exemptés.
 */
@Component
public class ProspectsParPeriodeRapport implements Rapport {

    static final String CODE = "ComTdbProspectsPerCommercial";
    static final String NON_AFFECTE = "Non affecté";

    private static final String PERIODE = "periode";

    // Un jour par ligne : semaines et mois se déduisent en Java ; tiers sans commercial gardés (« Non affecté »).
    private static final String PAR_JOUR = """
            SELECT t.CfgCommercialId AS Commercial, CAST(t.UserCreateDateTime AS date) AS Jour, COUNT(*) AS Nombre
            FROM CfgTier t
            JOIN CfgTierType tt ON tt.PKey = t.CfgTierTypeId AND tt.Code IN ('PRO', 'CLI')
            WHERE CAST(t.UserCreateDateTime AS date) >= :du AND CAST(t.UserCreateDateTime AS date) <= :au
            GROUP BY t.CfgCommercialId, CAST(t.UserCreateDateTime AS date)""";
    private static final String NOMS = """
            SELECT c.PKey, c.SocialReason, c.Description, c.FirstName, c.LastName FROM CfgTier c
            WHERE c.PKey IN (:ids)""";

    private final NamedParameterJdbcTemplate jdbc;
    private final PerimetreCommercialService perimetreService;
    private final Clock clock;

    public ProspectsParPeriodeRapport(NamedParameterJdbcTemplate jdbc, PerimetreCommercialService perimetreService,
                                      Clock clock) {
        this.jdbc = jdbc;
        this.perimetreService = perimetreService;
        this.clock = clock;
    }

    private record Compte(UUID commercial, LocalDate jour, long nombre) {
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
        LocalDate lundi = LocalDate.now(clock).with(DayOfWeek.MONDAY);
        return List.of(ParametreRapport.date(PeriodesRapport.DU, "Du", lundi),
                ParametreRapport.date(PeriodesRapport.AU, "Au", lundi.plusDays(6)), PeriodesRapport.annee(clock));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDate du = contexte.date(PeriodesRapport.DU);
        LocalDate au = contexte.date(PeriodesRapport.AU);
        int annee = contexte.annee(PeriodesRapport.ANNEE);
        Predicate<UUID> visible = visibilite(contexte);
        List<Compte> periode = lire(du, au, visible);
        List<Compte> annees = lire(LocalDate.of(annee - 4, 1, 1), LocalDate.of(annee, 12, 31), visible);
        Map<UUID, String> noms = noms(periode, annees);

        // Semaine ISO 8601 (« s11 ») ; suffixée de l'année ISO seulement si la période en couvre deux.
        boolean uneAnnee = du.get(IsoFields.WEEK_BASED_YEAR) == au.get(IsoFields.WEEK_BASED_YEAR);
        Function<LocalDate, String> suffixe = j -> uneAnnee ? "" : " " + j.get(IsoFields.WEEK_BASED_YEAR);
        Function<LocalDate, String> libelleSemaine = j -> "S" + j.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                + suffixe.apply(j);
        Function<String, String> nom = l -> l.toLowerCase(Locale.ROOT).replace(' ', '-');
        Function<Compte, String> semaine = c -> nom.apply(libelleSemaine.apply(c.jour()));
        Map<String, ColonneRapport> semaines = new LinkedHashMap<>();
        du.datesUntil(au.plusDays(1)).map(libelleSemaine).forEach(l -> semaines.computeIfAbsent(l,
                k -> ColonneRapport.effectif(nom.apply(k), k, null)));
        List<ColonneRapport> colonnesSemaines = List.copyOf(semaines.values());
        List<ColonneRapport> colonnesAnnees = IntStream.rangeClosed(annee - 4, annee)
                .mapToObj(a -> ColonneRapport.effectif("a" + a, String.valueOf(a), null)).toList();
        Function<Compte, String> mois = c -> PeriodesRapport.colonneMois(c.jour().getMonthValue());
        Function<Compte, String> an = c -> "a" + c.jour().getYear();
        return new ResultatRapport(List.of(
                matrice("par-semaine", "Prospects par semaine", colonnesSemaines, periode, semaine, noms),
                matrice("par-mois", "Prospects par mois", PeriodesRapport.colonnesMois(), periode, mois, noms),
                matrice("par-annee", "Prospects par année", colonnesAnnees, annees, an, noms),
                global("global-semaine", "Prospects par semaine (total)", colonnesSemaines, periode, semaine),
                global("global-mois", "Prospects par mois (total)", PeriodesRapport.colonnesMois(), periode, mois),
                global("global-annee", "Prospects par année (total)", colonnesAnnees, annees, an)), List.of());
    }

    /** Exempté : tous, tiers sans commercial compris ; sinon les seuls commerciaux visibles. */
    private Predicate<UUID> visibilite(ContexteRapport contexte) {
        if (contexte.admUserId() != null && perimetreService.estExempte(contexte.admUserId())) {
            return c -> true;
        }
        Set<UUID> cles = Set.copyOf(contexte.commerciauxVisibles());
        return c -> c != null && cles.contains(c);
    }

    private List<Compte> lire(LocalDate du, LocalDate au, Predicate<UUID> visible) {
        return jdbc.query(PAR_JOUR, Map.of(PeriodesRapport.DU, du, PeriodesRapport.AU, au), (rs, n) -> {
            String commercial = rs.getString("Commercial");
            return new Compte(commercial == null ? null : UUID.fromString(commercial),
                    rs.getDate("Jour").toLocalDate(), rs.getLong("Nombre"));
        }).stream().filter(c -> visible.test(c.commercial())).toList();
    }

    private Map<UUID, String> noms(List<Compte> periode, List<Compte> annees) {
        List<UUID> ids = Stream.concat(periode.stream(), annees.stream()).map(Compte::commercial)
                .filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> noms = new HashMap<>();
        LotsTranches.lire(ids, lot -> jdbc.query(NOMS, Map.of("ids", LotsTranches.textes(lot)),
                        (rs, n) -> Map.entry(UUID.fromString(rs.getString("PKey")),
                                Objects.requireNonNullElse(TierNomFormatter.nom(rs.getString("SocialReason"),
                                        rs.getString("Description"), rs.getString("FirstName"),
                                        rs.getString("LastName")), ""))))
                .forEach(e -> noms.put(e.getKey(), e.getValue()));
        return noms;
    }

    private static SectionRapport matrice(String id, String titre, List<ColonneRapport> periodes, List<Compte> comptes,
                                          Function<Compte, String> colonne, Map<UUID, String> noms) {
        Map<UUID, Map<String, Long>> parCommercial = new LinkedHashMap<>();
        comptes.forEach(c -> parCommercial.computeIfAbsent(c.commercial(), k -> new HashMap<>())
                .merge(colonne.apply(c), c.nombre(), Long::sum));
        List<LigneCommerciale> lignes = new ArrayList<>();
        parCommercial.forEach((commercial, valeurs) -> lignes.add(new LigneCommerciale(
                commercial == null ? NON_AFFECTE : noms.getOrDefault(commercial, commercial.toString()), valeurs)));
        lignes.sort(Comparator.comparing(LigneCommerciale::commercial));
        return SectionsCommerciales.matrice(id, titre, periodes, lignes);
    }

    /** Série globale par période (une ligne par colonne), qui porte le graphique en colonnes. */
    private static SectionRapport global(String id, String titre, List<ColonneRapport> periodes, List<Compte> comptes,
                                         Function<Compte, String> colonne) {
        Map<String, Long> parColonne = new HashMap<>();
        comptes.forEach(c -> parColonne.merge(colonne.apply(c), c.nombre(), Long::sum));
        Map<String, Long> parPeriode = new LinkedHashMap<>();
        periodes.forEach(p -> parPeriode.put(p.libelle(), parColonne.getOrDefault(p.nom(), 0L)));
        return SectionsCommerciales.serie(id, titre, PERIODE, "Période", parPeriode, TypeGraphique.COLONNES);
    }
}
