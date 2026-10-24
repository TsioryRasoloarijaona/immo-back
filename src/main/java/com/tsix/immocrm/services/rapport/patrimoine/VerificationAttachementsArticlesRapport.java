package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TEXTE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHES;

/**
 * VÉRIFICATION DES ATTACHEMENTS — articles (FEATURE-patrimoine-rapports.md §6) : pièces d'article par type, une ligne par
 * article VENTE ou LOCATION des tranches en vente (Q6), article sans pièce compris.
 */
@Component
public class VerificationAttachementsArticlesRapport implements Rapport {

    static final String CODE = "StkVerifAttachment";

    private static final String AVEC_SOUS_ARTICLES = "avecSousArticles";
    private static final String SANS_TYPE = "sansType";
    private static final String TOTAL = "total";
    private static final String COCHES = "coches";

    private static final String PIECES = """
            SELECT i.PKey AS ArticleId, t.Description AS Tranche, i.Code, i.Description, ty.PKey AS TypeId,
                   ty.Description AS Type, COUNT(a.PKey) AS Fichiers,
                   SUM(CASE WHEN a.IsCheckedButNotAttached = 1 THEN 1 ELSE 0 END) AS Coches
            FROM StkItem i
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId AND t.IsForSale = 1
            JOIN StkVocation v ON v.PKey = i.StkVocationId AND v.Code IN ('VENTE', 'LOCATION')
            LEFT JOIN AdmAttachedFile a ON a.CategoryId = i.PKey
            LEFT JOIN AdmAttachedFileType ty ON ty.PKey = a.AdmAttachedFileTypeId
            WHERE i.StkHierarchyId IN (:tranches) AND (:avecSousArticles = 1 OR ISNULL(i.IsUnderItem, 0) = 0)
            GROUP BY i.PKey, t.Description, i.Code, i.Description, ty.PKey, ty.Description""";

    private final NamedParameterJdbcTemplate jdbc;

    public VerificationAttachementsArticlesRapport(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record Article(String id, String tranche, String code, String libelle) {
    }

    /** Pièces d'un article pour un type ({@code typeId} nul = sans type, ou aucune pièce si {@code fichiers} = 0). */
    record Comptage(Article article, String typeId, String type, long fichiers, long coches) {
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
        return List.of(ParametreRapport.tranches(), new ParametreRapport(AVEC_SOUS_ARTICLES, "Avec les sous-articles",
                TypeParametre.BOOLEEN, false, false, "false", List.of(), null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int avecSousArticles = Boolean.TRUE.equals(contexte.booleen(AVEC_SOUS_ARTICLES)) ? 1 : 0;
        List<Comptage> comptages = LotsTranches.lire(contexte.tranchesVisibles(), lot -> jdbc.query(PIECES,
                Map.of(TRANCHES, LotsTranches.textes(lot), AVEC_SOUS_ARTICLES, avecSousArticles),
                (rs, n) -> new Comptage(new Article(rs.getString("ArticleId"), rs.getString("Tranche"),
                        rs.getString("Code"), rs.getString("Description")), rs.getString("TypeId"),
                        rs.getString("Type"), rs.getLong("Fichiers"), rs.getLong("Coches"))));
        return new ResultatRapport(List.of(section(comptages)), List.of());
    }

    /** Article → (id de type, {@code sansType}, {@code total}, {@code coches}) → nombre ; remplit {@code types}. */
    private static Map<Article, Map<String, Long>> compter(List<Comptage> comptages, Map<String, String> types) {
        Map<Article, Map<String, Long>> parArticle = new LinkedHashMap<>();
        for (Comptage c : comptages) {
            Map<String, Long> cases = parArticle.computeIfAbsent(c.article(), a -> new HashMap<>());
            cases.merge(COCHES, c.coches(), Long::sum);
            if (c.fichiers() > 0) {
                cases.merge(c.typeId() == null ? SANS_TYPE : c.typeId(), c.fichiers(), Long::sum);
                cases.merge(TOTAL, c.fichiers(), Long::sum);
            }
            if (c.fichiers() > 0 && c.typeId() != null) {
                types.put(c.typeId(), c.type());
            }
        }
        return parArticle;
    }

    private static Map<String, Object> ligne(Article article, Map<String, Long> cases,
                                             List<Map.Entry<String, String>> typesTries, boolean sansType) {
        Map<String, Object> ligne = ColonnesStock.ligne(TRANCHE, article.tranche(), "code", article.code(),
                "article", article.libelle());
        for (int i = 0; i < typesTries.size(); i++) {
            ligne.put("type" + (i + 1), cases.getOrDefault(typesTries.get(i).getKey(), 0L));
        }
        if (sansType) {
            ligne.put(SANS_TYPE, cases.getOrDefault(SANS_TYPE, 0L));
        }
        ligne.put(TOTAL, cases.getOrDefault(TOTAL, 0L));
        ligne.put(COCHES, cases.getOrDefault(COCHES, 0L));
        return ligne;
    }

    private static SectionRapport section(List<Comptage> comptages) {
        Map<String, String> types = new HashMap<>();
        Map<Article, Map<String, Long>> parArticle = compter(comptages, types);
        // Colonnes dynamiques : une par type présent, triées par libellé, nommées type1, type2…
        List<Map.Entry<String, String>> typesTries = types.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(TEXTE)).toList();
        boolean sansType = parArticle.values().stream().anyMatch(cases -> cases.containsKey(SANS_TYPE));
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonnesStock.COLONNE_TRANCHE,
                ColonneRapport.texte("code", "Code"), ColonneRapport.texte("article", "Article")));
        for (int i = 0; i < typesTries.size(); i++) {
            colonnes.add(ColonneRapport.effectif("type" + (i + 1), typesTries.get(i).getValue(), null));
        }
        if (sansType) {
            colonnes.add(ColonneRapport.effectif(SANS_TYPE, "Sans type", null));
        }
        colonnes.add(ColonneRapport.effectif(TOTAL, "Fichiers attachés", null));
        colonnes.add(ColonneRapport.effectif(COCHES, "dont vérifiées non jointes", null));

        List<Map<String, Object>> lignes = parArticle.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Article::tranche, TEXTE)
                        .thenComparing(Article::code, TEXTE)))
                .map(e -> ligne(e.getKey(), e.getValue(), typesTries, sansType)).toList();
        return new SectionRapport("pieces", "Pièces des articles", colonnes, lignes, TRANCHE, null, null);
    }
}
