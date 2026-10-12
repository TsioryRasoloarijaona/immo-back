package com.tsix.immocrm.domain.audit;

import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;

import java.math.BigDecimal;
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
    // Lot P5 : Habilitation et Séquence mesurés en production ; les tables d'actionnaires y sont vides, d'où le nom de
    // classe et le DisplayName OMRAN (Yes.Cfg/CfgShareholder.cs…), libellé = DefaultLabel.
    static final TypeObjetAudit HABILITATION = new TypeObjetAudit("Yes.Adm.AdmUserHabilitation", "Habilitation");
    static final TypeObjetAudit SEQUENCE = new TypeObjetAudit("Yes.Adm.Sequence", "Séquence");
    static final TypeObjetAudit ACTIONNAIRE = new TypeObjetAudit("Yes.Cfg.CfgShareholder", "Actionnaire");
    static final TypeObjetAudit GROUPE_ACTIONNAIRES =
            new TypeObjetAudit("Yes.Cfg.CfgGroupShareholder", "Groupe d'actionnaire");
    static final TypeObjetAudit LIGNE_GROUPE = new TypeObjetAudit("Yes.Cfg.CfgGroupShareholderLine", "Répartition");
    static final TypeObjetAudit REPARTITION_ENTITE = new TypeObjetAudit("Yes.Cfg.CfgCompanyShareholder", "Répartition");

    private static final Map<Class<?>, DescripteurAudit<?>> DESCRIPTEURS = Stream.of(
            racine(CfgProject.class, p -> PROGRAMME, CfgProject::getCode),
            // OMRAN libelle une tranche par sa Description (60 lignes sur 84), pas par son Code.
            racine(CfgTranche.class, t -> TRANCHE, t -> premierRenseigne(t.getDescription(), t.getCode())),
            racine(StkItem.class, RegistreAuditOmran::typeArticle, RegistreAuditOmran::libelleArticle),
            racine(CfgCompany.class, c -> ENTITE_JURIDIQUE, c -> premierRenseigne(c.getDescription(), c.getCode())),
            new DescripteurAudit<>(CfgTrancheStkItem.class, l -> TERRAIN_DE_TRANCHE,
                    l -> l.getSorting() == null ? "" : l.getSorting().toString(),
                    l -> l.getCfgTranche() == null ? null
                            : new ParentAudit(TRANCHE.objectType(), l.getCfgTranche().getId())),
            racine(AdmUserHabilitation.class, h -> HABILITATION,
                    h -> h.getAdmUser() == null ? "" : h.getAdmUser().getId().toString()),
            racine(AdmSequence.class, s -> SEQUENCE, AdmSequence::getCode),
            racine(CfgShareholder.class, a -> ACTIONNAIRE, CfgShareholder::getDescription),
            racine(CfgGroupShareholder.class, g -> GROUPE_ACTIONNAIRES, CfgGroupShareholder::getDescription),
            new DescripteurAudit<>(CfgGroupShareholderLine.class, l -> LIGNE_GROUPE,
                    l -> partDe(l.getCfgShareholder(), l.getSharingNumber()),
                    l -> l.getCfgGroupShareholder() == null ? null
                            : new ParentAudit(GROUPE_ACTIONNAIRES.objectType(), l.getCfgGroupShareholder().getId())),
            new DescripteurAudit<>(CfgCompanyShareholder.class, l -> REPARTITION_ENTITE,
                    l -> partDe(l.getCfgShareholder(), l.getSharingNumber()),
                    l -> l.getCfgCompany() == null ? null
                            : new ParentAudit(ENTITE_JURIDIQUE.objectType(), l.getCfgCompany().getId())))
            .collect(Collectors.toUnmodifiableMap(DescripteurAudit::classe, d -> d));

    private static final Set<String> OBJECT_TYPES = Stream.of(PROGRAMME, TRANCHE, ARTICLE, SOUS_ARTICLE, TERRAIN,
                    ENTITE_JURIDIQUE, TERRAIN_DE_TRANCHE, HABILITATION, SEQUENCE, ACTIONNAIRE, GROUPE_ACTIONNAIRES,
                    LIGNE_GROUPE, REPARTITION_ENTITE)
            .map(TypeObjetAudit::objectType)
            .collect(Collectors.toUnmodifiableSet());

    private RegistreAuditOmran() {
    }

    public static Optional<DescripteurAudit<?>> pour(Class<?> classe) {
        return Optional.ofNullable(DESCRIPTEURS.get(classe));
    }

    /**
     * Entité du registre et action tracée. Un compteur {@code AdmSequence} qui avance n'est pas tracé : OMRAN
     * l'incrémente par un UPDATE brut ({@code Sequence.GetNewValue}), seules création et suppression le sont.
     */
    public static boolean trace(Class<?> classe, ActionAudit action) {
        return DESCRIPTEURS.containsKey(classe)
                && !(classe == AdmSequence.class && action == ActionAudit.MODIFICATION);
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

    /** {@code CfgShareholderDescriptionHistory} d'OMRAN : libellé de l'actionnaire, espace, part. */
    static String partDe(CfgShareholder actionnaire, Object part) {
        String nom = actionnaire == null || actionnaire.getDescription() == null ? "" : actionnaire.getDescription();
        String valeur = part instanceof BigDecimal decimal ? decimal.toPlainString() : String.valueOf(part);
        return nom + " " + (part == null ? "" : valeur);
    }

    private static String premierRenseigne(String premier, String second) {
        return premier != null && !premier.isBlank() ? premier : second;
    }
}
