package com.tsix.immocrm.domain.audit;

import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
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
    // Libellés OMRAN tels quels (FEATURE-patrimoine-tranche-onglets.md §0), « Prospection de projet » compris.
    static final TypeObjetAudit ETAGE_DE_TRANCHE = new TypeObjetAudit("Yes.Cfg.CfgTrancheLevel",
            "Prospection de projet");
    static final TypeObjetAudit PLANCHER_DE_TRANCHE = new TypeObjetAudit("Yes.Cfg.CfgTrancheCoveredFloor",
            "Prospection de projet");
    static final TypeObjetAudit FRAIS_DE_TRANCHE = new TypeObjetAudit("Yes.Cfg.CfgTrancheSettlementCategory",
            "Frais de vente");
    static final TypeObjetAudit HABILITATION = new TypeObjetAudit("Yes.Adm.AdmUserHabilitation", "Habilitation");

    private static final Map<Class<?>, DescripteurAudit<?>> DESCRIPTEURS = Stream.of(
            racine(CfgProject.class, p -> PROGRAMME, CfgProject::getCode),
            // OMRAN libelle une tranche par sa Description (60 lignes sur 84), pas par son Code.
            racine(CfgTranche.class, t -> TRANCHE, t -> premierRenseigne(t.getDescription(), t.getCode())),
            racine(StkItem.class, RegistreAuditOmran::typeArticle, RegistreAuditOmran::libelleArticle),
            racine(CfgCompany.class, c -> ENTITE_JURIDIQUE, CfgCompany::getCode),
            enfantDeTranche(CfgTrancheStkItem.class, TERRAIN_DE_TRANCHE, l -> texte(l.getSorting()),
                    CfgTrancheStkItem::getCfgTranche),
            enfantDeTranche(CfgTrancheLevel.class, ETAGE_DE_TRANCHE, l -> texte(l.getId()),
                    CfgTrancheLevel::getCfgTranche),
            enfantDeTranche(CfgTrancheCoveredFloor.class, PLANCHER_DE_TRANCHE, l -> texte(l.getId()),
                    CfgTrancheCoveredFloor::getCfgTranche),
            enfantDeTranche(CfgTrancheSettlementCategory.class, FRAIS_DE_TRANCHE, l -> texte(l.getSorting()),
                    CfgTrancheSettlementCategory::getCfgTranche),
            // Racine comme en production ; libellé = AdmUserId.
            racine(AdmUserHabilitation.class, h -> HABILITATION,
                    h -> h.getAdmUser() == null ? "" : texte(h.getAdmUser().getId())))
            .collect(Collectors.toUnmodifiableMap(DescripteurAudit::classe, d -> d));

    private static final Set<String> OBJECT_TYPES = Stream.of(PROGRAMME, TRANCHE, ARTICLE, SOUS_ARTICLE, TERRAIN,
                    ENTITE_JURIDIQUE, TERRAIN_DE_TRANCHE, ETAGE_DE_TRANCHE, PLANCHER_DE_TRANCHE, FRAIS_DE_TRANCHE,
                    HABILITATION)
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

    /** Ligne d'un onglet de tranche : enfant de la tranche ({@code ParentObjectType = Yes.Cfg.CfgTranche}). */
    private static <T> DescripteurAudit<?> enfantDeTranche(Class<T> classe, TypeObjetAudit type,
                                                           Function<T, String> libelle,
                                                           Function<T, CfgTranche> tranche) {
        return new DescripteurAudit<>(classe, e -> type, libelle, e -> {
            CfgTranche parent = tranche.apply(e);
            return parent == null ? null : new ParentAudit(TRANCHE.objectType(), parent.getId());
        });
    }

    private static String texte(Object valeur) {
        return valeur == null ? "" : valeur.toString();
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
