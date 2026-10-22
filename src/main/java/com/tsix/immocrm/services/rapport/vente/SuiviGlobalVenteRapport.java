package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.rapport.vente.MontantsVenteParTranche.TRANCHE_LIBELLE;

/**
 * N° 12 SUIVI GLOBAL DE VENTE (FEATURE-tdb-vente-rapports-socle.md §6), modèle OMRAN {@code ComTdbSalesMonitoringGlobal} :
 * article = {@code IsUnderItem = 0} de vocation VENTE, vendu = statut {@code BoolField001 = 1}, libre = articles − vendus.
 */
@Component
public class SuiviGlobalVenteRapport implements Rapport {

    static final String CODE = "ComTdbSalesMonitoringGlobal";
    static final String AVERTISSEMENT = "Article = IsUnderItem 0 comme le modèle OMRAN : les parkings autonomes sont "
            + "comptés en sous-articles (l'État des ventes compte les articles racines).";

    private static final String NB_ARTICLES = "nbArticles";
    private static final String NB_VENDUS = "nbVendus";
    private static final String NB_LIBRES = "nbLibres";
    private static final String LIBELLE = "libelle";
    private static final String TRANCHE_CODE = "trancheCode";
    private static final String ARTICLES = "Articles";
    private static final String TRANCHE = "Tranche";
    private static final String VENDUS = "Vendus";
    private static final String LIBRES = "Libres";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private static final String TRANCHES = "SELECT PKey, Code, Description FROM CfgTranche WHERE PKey IN (:tranches)";

    // Une ligne par article de vocation VENTE ; LEFT JOIN : un dossier sans statut reste compté (« Sans statut »).
    private static final String ARTICLES_VENTE = """
            SELECT i.StkHierarchyId, i.IsUnderItem, i.Area, i.ComFolderId, s.PKey AS StatutId,
                   COALESCE(s.Description, s.Code) AS Statut, s.Sorting AS StatutOrdre, s.BoolField001 AS Vendu,
                   c.Description AS Categorie, ty.Description AS TypeArticle, fl.Description AS Etage,
                   fl.Sorting AS EtageOrdre, b.Description AS Immeuble, b.Sorting AS ImmeubleOrdre,
                   p.Description AS Presentation, o.Description AS Orientation
            FROM StkItem i
            JOIN StkVocation v ON v.PKey = i.StkVocationId AND v.Code = 'VENTE'
            LEFT JOIN ComFolder f ON f.PKey = i.ComFolderId
            LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId
            LEFT JOIN StkItemCategory c ON c.PKey = i.StkItemCategoryId
            LEFT JOIN StkItemType ty ON ty.PKey = i.StkItemTypeId
            LEFT JOIN StkFloor fl ON fl.PKey = i.StkFloorId
            LEFT JOIN StkBuilding b ON b.PKey = i.StkBuildingId
            LEFT JOIN StkPresentationType p ON p.PKey = i.StkPresentationTypeId
            LEFT JOIN StkOrientation o ON o.PKey = i.StkOrientationId
            WHERE i.StkHierarchyId IN (:tranches) AND i.IsUnderItem IN (0, 1)""";

    private final NamedParameterJdbcTemplate jdbc;
    private final MontantsVenteParTranche montants;

    public SuiviGlobalVenteRapport(NamedParameterJdbcTemplate jdbc, MontantsVenteParTranche montants) {
        this.jdbc = jdbc;
        this.montants = montants;
    }

    record Tranche(UUID id, String code, String libelle) {
    }

    record Article(UUID tranche, boolean sous, BigDecimal surface, boolean retenu, UUID statutId, String statut,
                   Integer statutOrdre, boolean vendu, String categorie, String type, String etage, Integer etageOrdre,
                   String immeuble, Integer immeubleOrdre, String presentation, String orientation) {
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
        return List.of(ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<UUID> ids = contexte.tranchesVisibles();
        List<Tranche> tranches = LotsTranches.lire(ids, lot -> jdbc.query(TRANCHES,
                Map.of("tranches", LotsTranches.textes(lot)), (rs, n) -> new Tranche(uuid(rs, "PKey"),
                        rs.getString("Code"), rs.getString("Description")))).stream()
                .sorted(Comparator.comparing(t -> Objects.toString(t.libelle(), ""))).toList();
        List<Article> articles = LotsTranches.lire(ids, lot -> jdbc.query(ARTICLES_VENTE,
                Map.of("tranches", LotsTranches.textes(lot)), (rs, n) -> article(rs)));
        List<Article> principaux = articles.stream().filter(a -> !a.sous()).toList();
        List<SectionRapport> sections = new ArrayList<>(List.of(
                StockParTranche.section(tranches, articles),
                StatutsParTranche.section(tranches, principaux),
                new SectionRapport("ventes", "Ventes par tranche", MontantsVenteParTranche.colonnesVentes(),
                        montants.ventes(ids), null, null, List.of()),
                new SectionRapport("annulations", "Annulations par tranche", colonnesAnnulations(),
                        montants.annulations(ids), null, null, List.of())));
        sections.add(repartition("categories", "Catégories", principaux, Article::categorie, null));
        sections.add(repartition("types", "Types", principaux, Article::type, null));
        sections.add(repartition("etages", "Étages", principaux, Article::etage,
                a -> a.etageOrdre() == null ? Integer.MAX_VALUE : a.etageOrdre()));
        sections.add(repartition("immeubles", "Immeubles", principaux, Article::immeuble,
                a -> a.immeubleOrdre() == null ? Integer.MAX_VALUE : a.immeubleOrdre()));
        sections.add(repartition("presentations", "Présentations", principaux, Article::presentation, null));
        sections.add(repartition("orientations", "Orientations", principaux, Article::orientation, null));
        return new ResultatRapport(sections, List.of(AVERTISSEMENT));
    }

    private static Article article(ResultSet rs) throws SQLException {
        BigDecimal surface = rs.getBigDecimal("Area");
        return new Article(uuid(rs, "StkHierarchyId"), rs.getBoolean("IsUnderItem"),
                surface == null ? BigDecimal.ZERO : surface, rs.getString("ComFolderId") != null,
                uuid(rs, "StatutId"), rs.getString("Statut"), entier(rs, "StatutOrdre"), rs.getBoolean("Vendu"),
                rs.getString("Categorie"), rs.getString("TypeArticle"), rs.getString("Etage"),
                entier(rs, "EtageOrdre"), rs.getString("Immeuble"), entier(rs, "ImmeubleOrdre"),
                rs.getString("Presentation"), rs.getString("Orientation"));
    }

    private static UUID uuid(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null ? null : UUID.fromString(valeur);
    }

    private static Integer entier(ResultSet rs, String colonne) throws SQLException {
        int valeur = rs.getInt(colonne);
        return rs.wasNull() ? null : valeur;
    }

    /** {@code part / base × 100}, 2 décimales ; 0 si la base est nulle (B1 : échelle 0–100). */
    static BigDecimal taux(BigDecimal part, BigDecimal base) {
        return base.signum() == 0 ? BigDecimal.ZERO : part.multiply(CENT).divide(base, 2, RoundingMode.HALF_UP);
    }

    /** Articles par tranche, liste vide pour une tranche sans article. */
    static List<Article> de(Map<UUID, List<Article>> parTranche, Tranche t) {
        return parTranche.getOrDefault(t.id(), List.of());
    }

    static Map<UUID, List<Article>> parTranche(List<Article> articles) {
        return articles.stream().collect(Collectors.groupingBy(Article::tranche));
    }

    private static List<ColonneRapport> colonnesAnnulations() {
        return List.of(ColonneRapport.texte(TRANCHE_LIBELLE, TRANCHE),
                ColonneRapport.montant("montantAnnulation", "Montant", null),
                ColonneRapport.montant("regleAnnulation", "Réglé", null),
                ColonneRapport.montant("resteAnnulation", "Reste", null));
    }

    /**
     * Articles par valeur d'un référentiel (modèle : {@code ItemCount <> 0}, d'où aucune ligne vide) ; tri par
     * {@code ordre} s'il est donné (étages, immeubles), sinon par libellé.
     */
    private static SectionRapport repartition(String id, String titre, List<Article> articles,
                                              Function<Article, String> libelle, ToIntFunction<Article> ordre) {
        Map<String, long[]> comptes = new LinkedHashMap<>();
        Map<String, Integer> ordres = new LinkedHashMap<>();
        for (Article a : articles) {
            String valeur = libelle.apply(a);
            if (valeur != null) {
                long[] c = comptes.computeIfAbsent(valeur, v -> new long[2]);
                c[0]++;
                c[1] += a.vendu() ? 1 : 0;
                ordres.merge(valeur, ordre == null ? Integer.MAX_VALUE : ordre.applyAsInt(a), Math::min);
            }
        }
        List<Map<String, Object>> lignes = comptes.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, long[]>>comparingInt(e -> ordres.get(e.getKey()))
                        .thenComparing(Map.Entry::getKey))
                .map(e -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put(LIBELLE, e.getKey());
                    ligne.put(NB_ARTICLES, e.getValue()[0]);
                    ligne.put(NB_LIBRES, e.getValue()[0] - e.getValue()[1]);
                    ligne.put(NB_VENDUS, e.getValue()[1]);
                    return ligne;
                })
                .toList();
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(LIBELLE, titre),
                ColonneRapport.effectif(NB_ARTICLES, ARTICLES, null),
                ColonneRapport.effectif(NB_LIBRES, LIBRES, null),
                ColonneRapport.effectif(NB_VENDUS, VENDUS, null));
        return new SectionRapport(id, titre, colonnes, lignes, null, null, List.of(new GraphiqueRapport(
                TypeGraphique.COLONNES, titre + " : libres et vendus", LIBELLE, List.of(NB_LIBRES, NB_VENDUS))));
    }

    /** Section {@code stock} : effectifs et surfaces par tranche, taux imposés dans les totaux (Σ vendus / Σ articles). */
    static final class StockParTranche {

        private static final String[] PREFIXES = {"", "sous"};
        private static final String TAUX_VENTE = "tauxVente";
        private static final String TAUX_SURFACE = "tauxSurface";
        private static final String SURFACE = "surface";
        private static final String SURFACE_VENDUE = "surfaceVendue";
        private static final String SURFACE_LIBRE = "surfaceLibre";

        private StockParTranche() {
        }

        static SectionRapport section(List<Tranche> tranches, List<Article> articles) {
            Map<UUID, List<Article>> parTranche = parTranche(articles);
            List<Map<String, Object>> lignes = new ArrayList<>();
            for (Tranche t : tranches) {
                Map<String, Object> ligne = new LinkedHashMap<>();
                ligne.put(TRANCHE_CODE, t.code());
                ligne.put(TRANCHE_LIBELLE, t.libelle());
                List<Article> ceux = de(parTranche, t);
                remplir(ligne, "", ceux.stream().filter(a -> !a.sous()).toList());
                remplir(ligne, "sous", ceux.stream().filter(Article::sous).toList());
                lignes.add(ligne);
            }
            Map<String, Object> totaux = new LinkedHashMap<>();
            for (String p : PREFIXES) {
                totaux.put(nom(p, TAUX_VENTE), taux(somme(lignes, nom(p, NB_VENDUS)),
                        somme(lignes, nom(p, NB_ARTICLES))));
                totaux.put(nom(p, TAUX_SURFACE), taux(somme(lignes, nom(p, SURFACE_VENDUE)),
                        somme(lignes, nom(p, SURFACE))));
            }
            return new SectionRapport("stock", "Stock par tranche", colonnes(), lignes, null, totaux,
                    List.of(new GraphiqueRapport(TypeGraphique.COLONNES, "Articles, vendus et libres par tranche",
                            TRANCHE_CODE, List.of(NB_ARTICLES, NB_VENDUS, NB_LIBRES))));
        }

        private static void remplir(Map<String, Object> ligne, String p, List<Article> articles) {
            long nb = articles.size();
            long vendus = articles.stream().filter(Article::vendu).count();
            BigDecimal surface = articles.stream().map(Article::surface).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal surfaceVendue = articles.stream().filter(Article::vendu).map(Article::surface)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ligne.put(nom(p, NB_ARTICLES), nb);
            ligne.put(nom(p, NB_VENDUS), vendus);
            ligne.put(nom(p, NB_LIBRES), nb - vendus);
            ligne.put(nom(p, TAUX_VENTE), taux(BigDecimal.valueOf(vendus), BigDecimal.valueOf(nb)));
            ligne.put(nom(p, SURFACE), surface);
            ligne.put(nom(p, SURFACE_VENDUE), surfaceVendue);
            ligne.put(nom(p, SURFACE_LIBRE), surface.subtract(surfaceVendue));
            ligne.put(nom(p, TAUX_SURFACE), taux(surfaceVendue, surface));
        }

        private static List<ColonneRapport> colonnes() {
            List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(TRANCHE_CODE, "Code"),
                    ColonneRapport.texte(TRANCHE_LIBELLE, TRANCHE)));
            for (String p : PREFIXES) {
                String groupe = p.isEmpty() ? ARTICLES : "Sous-articles";
                colonnes.add(ColonneRapport.effectif(nom(p, NB_ARTICLES), ARTICLES, groupe));
                colonnes.add(ColonneRapport.effectif(nom(p, NB_VENDUS), VENDUS, groupe));
                colonnes.add(ColonneRapport.effectif(nom(p, NB_LIBRES), LIBRES, groupe));
                colonnes.add(ColonneRapport.pourcentage(nom(p, TAUX_VENTE), "% vendus", groupe));
                String groupeSurface = p.isEmpty() ? "Surfaces" : groupe;
                colonnes.add(surface(nom(p, SURFACE), "Surface", groupeSurface));
                colonnes.add(surface(nom(p, SURFACE_VENDUE), "Surface vendue", groupeSurface));
                colonnes.add(surface(nom(p, SURFACE_LIBRE), "Surface libre", groupeSurface));
                colonnes.add(ColonneRapport.pourcentage(nom(p, TAUX_SURFACE), "% surface vendue", groupeSurface));
            }
            return colonnes;
        }

        private static ColonneRapport surface(String nom, String libelle, String groupe) {
            return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, groupe, FonctionTotal.SOMME, true, 2);
        }

        /** {@code nbArticles} → {@code sousNbArticles}. */
        private static String nom(String prefixe, String nom) {
            return prefixe.isEmpty() ? nom : prefixe + Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
        }

        private static BigDecimal somme(List<Map<String, Object>> lignes, String nom) {
            return lignes.stream().map(l -> new BigDecimal(l.get(nom).toString())).reduce(BigDecimal.ZERO,
                    BigDecimal::add);
        }
    }

    /**
     * Section {@code statuts} : « LIBRE » (aucun dossier), puis statuts par {@code Sorting}, puis « Sans statut »
     * (dossier sans {@code StrStatusId}, que le modèle perd par jointure interne).
     */
    static final class StatutsParTranche {

        private StatutsParTranche() {
        }

        private record Cle(String libelle, Integer ordre, int rang) {
        }

        static SectionRapport section(List<Tranche> tranches, List<Article> principaux) {
            Map<UUID, List<Article>> parTranche = parTranche(principaux);
            List<Map<String, Object>> lignes = new ArrayList<>();
            for (Tranche t : tranches) {
                List<Article> ceux = de(parTranche, t);
                Map<Cle, Long> parStatut = new LinkedHashMap<>();
                ceux.forEach(a -> parStatut.merge(cle(a), 1L, Long::sum));
                parStatut.entrySet().stream()
                        .sorted(Comparator.comparingInt((Map.Entry<Cle, Long> e) -> e.getKey().rang())
                                .thenComparing(e -> e.getKey().ordre(), Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(e -> e.getKey().libelle()))
                        .forEach(e -> {
                            Map<String, Object> ligne = new LinkedHashMap<>();
                            ligne.put(TRANCHE_LIBELLE, t.libelle());
                            ligne.put("statutLibelle", e.getKey().libelle());
                            ligne.put("ordreStatut", e.getKey().ordre());
                            ligne.put(NB_ARTICLES, e.getValue());
                            ligne.put("tauxTranche", taux(BigDecimal.valueOf(e.getValue()),
                                    BigDecimal.valueOf(ceux.size())));
                            lignes.add(ligne);
                        });
            }
            List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(TRANCHE_LIBELLE, TRANCHE),
                    ColonneRapport.texte("statutLibelle", "Statut"),
                    new ColonneRapport("ordreStatut", "Ordre", TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE, false, 0),
                    ColonneRapport.effectif(NB_ARTICLES, ARTICLES, null),
                    ColonneRapport.pourcentage("tauxTranche", "% de la tranche", null));
            return new SectionRapport("statuts", "Statuts par tranche", colonnes, lignes, TRANCHE_LIBELLE, null,
                    List.of());
        }

        private static Cle cle(Article a) {
            if (!a.retenu()) {
                return new Cle("LIBRE", 0, 0);
            }
            if (a.statutId() == null) {
                return new Cle("Sans statut", null, 2);
            }
            return new Cle(Objects.toString(a.statut(), ""), a.statutOrdre(), 1);
        }
    }
}
