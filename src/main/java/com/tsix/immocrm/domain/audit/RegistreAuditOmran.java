package com.tsix.immocrm.domain.audit;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entités dont chaque écriture produit une ligne {@code AdmWebHistorics} au format OMRAN ; absente d'ici, une entité
 * n'écrit rien. Un lot qui écrit une nouvelle table l'ajoute ici avec l'{@code ObjectType} mesuré en production
 * (FEATURE-patrimoine-audit-actions.md §1, §9). Libellés : ceux que les lignes OMRAN portent réellement (§ rapport).
 */
public final class RegistreAuditOmran {

    static final String NATURE_TERRAIN = "TER";

    static final TypeObjetAudit PROGRAMME = new TypeObjetAudit("Yes.Cfg.CfgProject", "Programme immobilier");
    static final TypeObjetAudit TRANCHE = new TypeObjetAudit("Yes.Cfg.CfgTranche", "Tranche");
    static final TypeObjetAudit ARTICLE = new TypeObjetAudit("Yes.Stk.Building.Item", "Article");
    static final TypeObjetAudit SOUS_ARTICLE = new TypeObjetAudit("Yes.Stk.Building.UnderItem", "Sous article");
    static final TypeObjetAudit TERRAIN = new TypeObjetAudit("Yes.Stk.LandProjectItem", "Terrain");
    static final TypeObjetAudit ENTITE_JURIDIQUE = new TypeObjetAudit("Yes.Cfg.CfgCompany", "Entité juridique");
    static final TypeObjetAudit TERRAIN_DE_TRANCHE = new TypeObjetAudit("Yes.Cfg.CfgTrancheStkItem", "Tranche");

    private static final Map<Class<?>, DescripteurAudit<?>> DESCRIPTEURS = Stream.of(
            racine(CfgProject.class, p -> PROGRAMME, CfgProject::getCode),
            // OMRAN libelle une tranche par sa Description (60 lignes sur 84), pas par son Code.
            racine(CfgTranche.class, t -> TRANCHE, t -> premierRenseigne(t.getDescription(), t.getCode())),
            racine(StkItem.class, RegistreAuditOmran::typeArticle, RegistreAuditOmran::libelleArticle),
            racine(CfgCompany.class, c -> ENTITE_JURIDIQUE, CfgCompany::getCode),
            new DescripteurAudit<>(CfgTrancheStkItem.class, l -> TERRAIN_DE_TRANCHE,
                    l -> l.getSorting() == null ? "" : l.getSorting().toString(),
                    l -> l.getCfgTranche() == null ? null
                            : new ParentAudit(TRANCHE.objectType(), l.getCfgTranche().getId())))
            .collect(Collectors.toUnmodifiableMap(DescripteurAudit::classe, d -> d));

    private static final Set<String> OBJECT_TYPES = Stream.of(PROGRAMME, TRANCHE, ARTICLE, SOUS_ARTICLE, TERRAIN,
                    ENTITE_JURIDIQUE, TERRAIN_DE_TRANCHE)
            .map(TypeObjetAudit::objectType)
            .collect(Collectors.toUnmodifiableSet());

    private RegistreAuditOmran() {
    }

    public static Optional<DescripteurAudit<?>> pour(Class<?> classe) {
        return Optional.ofNullable(DESCRIPTEURS.get(classe));
    }

    /** {@code ObjectType} que ce registre écrit : seules lignes que la lecture du journal sert (§10). */
    public static Set<String> objectTypes() {
        return OBJECT_TYPES;
    }

    private static <T> DescripteurAudit<?> racine(Class<T> classe, Function<T, TypeObjetAudit> type,
                                                  Function<T, String> libelle) {
        return new DescripteurAudit<>(classe, type, libelle, e -> null);
    }

    /** Nature « TER » d'abord (un terrain n'est jamais sous-article), puis {@code IsUnderItem}. */
    static TypeObjetAudit typeArticle(StkItem article) {
        if (article.getStkKind() != null && NATURE_TERRAIN.equalsIgnoreCase(article.getStkKind().getCode())) {
            return TERRAIN;
        }
        return Boolean.TRUE.equals(article.getIsUnderItem()) ? SOUS_ARTICLE : ARTICLE;
    }

    /** Article : son Code ; sous-article et terrain : leur Description, comme les lignes OMRAN. */
    static String libelleArticle(StkItem article) {
        if (typeArticle(article) == ARTICLE) {
            return premierRenseigne(article.getCode(), article.getDescription());
        }
        return premierRenseigne(article.getDescription(), article.getCode());
    }

    private static String premierRenseigne(String premier, String second) {
        return premier != null && !premier.isBlank() ? premier : second;
    }
}
