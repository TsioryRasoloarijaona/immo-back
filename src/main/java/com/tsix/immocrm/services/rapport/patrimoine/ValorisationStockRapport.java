package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TEXTE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHES;

/**
 * VALORISATION DU STOCK (FEATURE-patrimoine-rapports.md §2) : une seule requête agrégée ; les trois sections en sont
 * dérivées, leurs totaux sont donc égaux par construction. Prix = valeurs stockées des articles, sans recalcul.
 */
@Component
public class ValorisationStockRapport implements Rapport {

    static final String CODE = "StkTdbValorisation";
    static final String NON_DEFINIE = "Non définie";

    private static final String ENTITE_ID = "entiteId";
    private static final String VOCATION = "vocation";
    private static final String NATURE = "nature";
    private static final String ARTICLE = "ARTICLE";
    private static final String SOUS_ARTICLE = "SOUS_ARTICLE";
    private static final String PROGRAMME = "programme";
    private static final String CATEGORIE = "categorie";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String SURFACE_BASE = "surfaceBase";
    private static final String VALORISATION_BASE = "valorisationBase";
    private static final String SURFACE_FINALE = "surfaceFinale";
    private static final String VALORISATION_FINALE = "valorisationFinale";
    private static final ColonneRapport COLONNE_PROGRAMME = ColonneRapport.texte(PROGRAMME, "Programme");

    private static final String VOCATIONS = "SELECT DISTINCT Code, Description FROM StkVocation "
            + "WHERE Code IS NOT NULL ORDER BY Description";

    private static final String STOCK = """
            SELECT v.Description AS Vocation, pr.PKey AS ProgrammeId, pr.Description AS Programme,
                   t.PKey AS TrancheId, t.Description AS Tranche, ISNULL(i.IsUnderItem, 0) AS SousArticle,
                   c.Description AS Categorie, COUNT(*) AS NbArticles,
                   SUM(ISNULL(i.AreaFloor, 0)) AS SurfaceBase, SUM(ISNULL(i.SalePriceFloor, 0)) AS ValorisationBase,
                   SUM(ISNULL(i.Area, 0)) AS SurfaceFinale, SUM(ISNULL(i.SalePrice, 0)) AS ValorisationFinale
            FROM StkItem i
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId
            JOIN CfgProject pr ON pr.PKey = t.CfgProjectId
            JOIN StkVocation v ON v.PKey = i.StkVocationId
            LEFT JOIN StkItemCategory c ON c.PKey = i.StkItemCategoryId
            WHERE i.StkHierarchyId IN (:tranches)
              AND (:toutesEntites = 1 OR pr.CfgCompanyId IN (:entites))
              AND (:toutesVocations = 1 OR v.Code IN (:vocations))
              AND (:toutesNatures = 1 OR ISNULL(i.IsUnderItem, 0) = :sousArticle)
            GROUP BY v.Description, pr.PKey, pr.Description, t.PKey, t.Description, ISNULL(i.IsUnderItem, 0),
                     c.Description""";

    private final NamedParameterJdbcTemplate jdbc;
    private final EntitesActives entites;

    public ValorisationStockRapport(NamedParameterJdbcTemplate jdbc, EntitesActives entites) {
        this.jdbc = jdbc;
        this.entites = entites;
    }

    record Groupe(String id, String libelle) {
    }

    record Mesures(long nbArticles, BigDecimal surfaceBase, BigDecimal valorisationBase, BigDecimal surfaceFinale,
                   BigDecimal valorisationFinale) {

        Mesures plus(Mesures autre) {
            return new Mesures(nbArticles + autre.nbArticles, surfaceBase.add(autre.surfaceBase),
                    valorisationBase.add(autre.valorisationBase), surfaceFinale.add(autre.surfaceFinale),
                    valorisationFinale.add(autre.valorisationFinale));
        }

        Map<String, Object> dans(Map<String, Object> ligne) {
            ligne.put(NB_ARTICLES, nbArticles);
            ligne.put(SURFACE_BASE, surfaceBase);
            ligne.put(VALORISATION_BASE, valorisationBase);
            ligne.put(SURFACE_FINALE, surfaceFinale);
            ligne.put(VALORISATION_FINALE, valorisationFinale);
            return ligne;
        }
    }

    record Agregat(String vocation, Groupe programme, Groupe tranche, boolean sousArticle, String categorie,
                   Mesures mesures) {
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.PATRIMOINE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        List<ParametreRapport.Option> vocations = jdbc.query(VOCATIONS,
                (rs, n) -> new ParametreRapport.Option(rs.getString("Code"), rs.getString("Description")));
        return List.of(ColonnesStock.choixMultiple(ENTITE_ID, "Entités", entites.lire()),
                ColonnesStock.choixMultiple(VOCATION, "Vocations", vocations),
                ColonnesStock.choixMultiple(NATURE, "Nature", List.of(new ParametreRapport.Option(ARTICLE, "Article"),
                        new ParametreRapport.Option(SOUS_ARTICLE, "Sous-article"))),
                ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Map<String, Object> filtres = filtres(contexte);
        List<Agregat> agregats = LotsTranches.lire(contexte.tranchesVisibles(), lot -> {
            Map<String, Object> parametres = new LinkedHashMap<>(filtres);
            parametres.put(TRANCHES, LotsTranches.textes(lot));
            return jdbc.query(STOCK, parametres, (rs, n) -> new Agregat(rs.getString("Vocation"),
                    new Groupe(rs.getString("ProgrammeId"), rs.getString("Programme")),
                    new Groupe(rs.getString("TrancheId"), rs.getString("Tranche")), rs.getBoolean("SousArticle"),
                    Objects.requireNonNullElse(rs.getString("Categorie"), NON_DEFINIE),
                    new Mesures(rs.getLong("NbArticles"), rs.getBigDecimal("SurfaceBase"),
                            rs.getBigDecimal("ValorisationBase"), rs.getBigDecimal("SurfaceFinale"),
                            rs.getBigDecimal("ValorisationFinale"))));
        });
        return new ResultatRapport(List.of(
                new SectionRapport("detail", "Valorisation du stock", colonnes(ColonneRapport.texte(VOCATION, "Vocation"),
                        COLONNE_PROGRAMME, ColonnesStock.COLONNE_TRANCHE,
                        ColonneRapport.texte(NATURE, "Nature"), ColonneRapport.texte(CATEGORIE, "Catégorie")),
                        detail(agregats), TRANCHE, null, null),
                new SectionRapport("programmes", "Valorisation par programme", colonnes(COLONNE_PROGRAMME),
                        parGroupe(agregats, Agregat::programme, PROGRAMME), null, null, null),
                new SectionRapport(TRANCHES, "Valorisation par tranche", colonnes(ColonnesStock.COLONNE_TRANCHE),
                        parGroupe(agregats, Agregat::tranche, TRANCHE), null, null, null)), List.of());
    }

    private static Map<String, Object> filtres(ContexteRapport contexte) {
        List<String> entitesDemandees = contexte.choix(ENTITE_ID);
        List<String> vocations = contexte.choix(VOCATION);
        Set<String> natures = Set.copyOf(contexte.choix(NATURE));
        Map<String, Object> filtres = new LinkedHashMap<>();
        filtres.put("toutesEntites", entitesDemandees.isEmpty() ? 1 : 0);
        // SQL Server n'arrête pas l'évaluation au « OR » : la valeur de remplissage doit rester un uniqueidentifier valide.
        filtres.put("entites", entitesDemandees.isEmpty() ? List.of(new UUID(0, 0).toString()) : entitesDemandees);
        filtres.put("toutesVocations", vocations.isEmpty() ? 1 : 0);
        filtres.put("vocations", vocations.isEmpty() ? List.of("") : vocations);
        filtres.put("toutesNatures", natures.size() == 1 ? 0 : 1);
        filtres.put("sousArticle", natures.contains(SOUS_ARTICLE) ? 1 : 0);
        return filtres;
    }

    private static List<ColonneRapport> colonnes(ColonneRapport... libelles) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(libelles));
        colonnes.add(ColonneRapport.effectif(NB_ARTICLES, "Nombre d'articles", null));
        colonnes.add(ColonnesStock.surface(SURFACE_BASE, "Superficie de base"));
        colonnes.add(ColonneRapport.montant(VALORISATION_BASE, "Valorisation de base", null));
        colonnes.add(ColonnesStock.surface(SURFACE_FINALE, "Superficie finale"));
        colonnes.add(ColonneRapport.montant(VALORISATION_FINALE, "Valorisation finale", null));
        return colonnes;
    }

    private static List<Map<String, Object>> detail(List<Agregat> agregats) {
        return agregats.stream()
                .sorted(Comparator.comparing(Agregat::vocation, TEXTE)
                        .thenComparing(a -> a.programme().libelle(), TEXTE)
                        .thenComparing(a -> a.tranche().libelle(), TEXTE)
                        .thenComparing(Agregat::sousArticle)
                        .thenComparing(Agregat::categorie, TEXTE))
                .map(a -> a.mesures().dans(ColonnesStock.ligne(VOCATION, a.vocation(),
                        PROGRAMME, a.programme().libelle(), TRANCHE, a.tranche().libelle(),
                        NATURE, a.sousArticle() ? "Sous-article" : "Article", CATEGORIE, a.categorie())))
                .toList();
    }

    private static List<Map<String, Object>> parGroupe(List<Agregat> agregats, Function<Agregat, Groupe> groupe,
                                                       String colonne) {
        Map<Groupe, Mesures> sommes = new LinkedHashMap<>();
        agregats.forEach(a -> sommes.merge(groupe.apply(a), a.mesures(), Mesures::plus));
        return sommes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Groupe::libelle, TEXTE)))
                .map(e -> e.getValue().dans(ColonnesStock.ligne(colonne, e.getKey().libelle())))
                .toList();
    }
}
