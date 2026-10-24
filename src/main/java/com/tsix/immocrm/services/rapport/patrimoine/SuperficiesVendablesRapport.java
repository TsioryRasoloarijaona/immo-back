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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TEXTE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHES;

/**
 * ÉTAT DES SUPERFICIES VENDABLES (FEATURE-patrimoine-rapports.md §4) : articles VENTE et leurs sous-articles toujours
 * affectés ; « Vendu » = article lié à un dossier quel que soit son statut. Chaque bien est filtré sur son propre état.
 */
@Component
public class SuperficiesVendablesRapport implements Rapport {

    static final String CODE = "StkTdbAreaForSale";

    private static final String ETAT = "etat";
    private static final String LIBRE = "LIBRE";
    private static final String VENDU = "VENDU";
    private static final String CATEGORIE = "categorie";
    private static final String SURFACE = "surface";
    private static final String SURFACE_LIBELLE = "Sup. (m²)";

    private static final String BIENS = """
            SELECT t.Description AS Tranche, i.Code, i.Description, ISNULL(i.IsUnderItem, 0) AS SousArticle,
                   pa.Description AS Parent, c.Description AS Categorie, ty.Description AS Type, i.Area,
                   fl.Code AS Etage, fl.Sorting AS OrdreEtage, b.Code AS Bloc,
                   CASE WHEN i.ComFolderId IS NULL THEN 0 ELSE 1 END AS Vendu
            FROM StkItem i
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId
            JOIN StkVocation v ON v.PKey = i.StkVocationId AND v.Code = 'VENTE'
            LEFT JOIN StkItem pa ON pa.PKey = i.ParentId
            LEFT JOIN StkItemCategory c ON c.PKey = i.StkItemCategoryId
            LEFT JOIN StkItemType ty ON ty.PKey = i.StkItemTypeId
            LEFT JOIN StkFloor fl ON fl.PKey = i.StkFloorId
            LEFT JOIN StkBuilding b ON b.PKey = i.StkBuildingId
            WHERE i.StkHierarchyId IN (:tranches)
              AND (ISNULL(i.IsUnderItem, 0) = 0 OR i.AlwaysAffected = 1)
              AND ((:libre = 1 AND i.ComFolderId IS NULL) OR (:vendu = 1 AND i.ComFolderId IS NOT NULL))""";

    private static final Comparator<Bien> ORDRE = Comparator.comparing((Bien b) -> (String) b.ligne().get(TRANCHE), TEXTE)
            .thenComparing(Bien::ordreEtage, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Bien::libelle, TEXTE);

    private final NamedParameterJdbcTemplate jdbc;

    public SuperficiesVendablesRapport(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ligne de section déjà formée, avec ses clés de tri. */
    record Bien(Map<String, Object> ligne, boolean sousArticle, Integer ordreEtage, String libelle) {
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
        return List.of(ParametreRapport.tranches(), ColonnesStock.choixMultiple(ETAT, "État",
                List.of(new ParametreRapport.Option(LIBRE, "Libre"), new ParametreRapport.Option(VENDU, "Vendu"))));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<String> etats = contexte.choix(ETAT);
        Map<String, Object> filtres = Map.of("libre", etats.isEmpty() || etats.contains(LIBRE) ? 1 : 0,
                "vendu", etats.isEmpty() || etats.contains(VENDU) ? 1 : 0);
        List<Bien> biens = LotsTranches.lire(contexte.tranchesVisibles(), lot -> {
            Map<String, Object> parametres = new HashMap<>(filtres);
            parametres.put(TRANCHES, LotsTranches.textes(lot));
            return jdbc.query(BIENS, parametres, (rs, n) -> {
                boolean sousArticle = rs.getBoolean("SousArticle");
                String etat = rs.getInt("Vendu") == 1 ? "Vendu" : "Libre";
                String tranche = rs.getString("Tranche");
                String categorie = rs.getString("Categorie");
                Map<String, Object> ligne = sousArticle
                        ? ColonnesStock.ligne(TRANCHE, tranche, "article", rs.getString("Parent"),
                        CATEGORIE, categorie, SURFACE, rs.getBigDecimal("Area"), ETAT, etat)
                        : ColonnesStock.ligne(TRANCHE, tranche, "code", rs.getString("Code"),
                        "bien", rs.getString("Description"), CATEGORIE, categorie,
                        "type", rs.getString("Type"), SURFACE, rs.getBigDecimal("Area"), "etage", rs.getString("Etage"),
                        "bloc", rs.getString("Bloc"), ETAT, etat);
                return new Bien(ligne, sousArticle, rs.getObject("OrdreEtage", Integer.class),
                        rs.getString("Description"));
            });
        }).stream().sorted(ORDRE).toList();
        return new ResultatRapport(List.of(articles(biens), sousArticles(biens)), List.of());
    }

    private static SectionRapport articles(List<Bien> biens) {
        List<ColonneRapport> colonnes = List.of(ColonnesStock.COLONNE_TRANCHE,
                ColonnesStock.texteMasque("code", "Code"), ColonneRapport.texte("bien", "Bien"),
                ColonneRapport.texte(CATEGORIE, "Catégorie"), ColonneRapport.texte("type", "Type"),
                ColonnesStock.surface(SURFACE, SURFACE_LIBELLE), ColonneRapport.texte("etage", "Étage"),
                ColonneRapport.texte("bloc", "Bloc"), ColonneRapport.texte(ETAT, "État"));
        return new SectionRapport("articles", "Articles", colonnes,
                biens.stream().filter(b -> !b.sousArticle()).map(Bien::ligne).toList(), TRANCHE, null, null);
    }

    private static SectionRapport sousArticles(List<Bien> biens) {
        List<ColonneRapport> colonnes = List.of(ColonnesStock.COLONNE_TRANCHE,
                ColonneRapport.texte("article", "Article"), ColonneRapport.texte(CATEGORIE, "Catégorie"),
                ColonnesStock.surface(SURFACE, SURFACE_LIBELLE), ColonneRapport.texte(ETAT, "État"));
        return new SectionRapport("sous-articles", "Sous-articles toujours affectés", colonnes,
                biens.stream().filter(Bien::sousArticle).map(Bien::ligne).toList(), TRANCHE, null, null);
    }
}
