package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Visibilité par entité juridique — « ne montrer que ce qui vient d'une société en exploitation »
 * ({@code instructions/FEATURE-visibilite-transversale-entites.md}).
 *
 * <p>L'application de bureau légataire n'expose que deux entités, <b>ADMOD</b> et <b>GHRIZA</b> ;
 * ce filtre reproduit ce comportement côté API. La sélection vient de {@code CfgCompany.IsActive},
 * jamais d'une liste de codes figée dans le code.
 *
 * <p><b>Trois différences avec le périmètre commercial</b>, avec lequel ces prédicats se composent
 * dans {@link PerimetreCommercialService} :
 * <ul>
 *   <li><b>Global et permanent</b>, là où le périmètre est opt-in par compte. Il s'applique donc
 *       aussi aux rôles de {@code RolesExemptesPerimetre}, {@code Administrateur} compris : une
 *       entité inactive n'est pas une donnée « qu'on n'a pas le droit de voir », c'est une donnée
 *       qui n'est plus en exploitation.</li>
 *   <li><b>Sans état</b> — il ne dépend pas de l'appelant, d'où une classe statique plutôt qu'un
 *       bean.</li>
 *   <li>Il porte sur la <b>société</b>, pas sur le projet ; la chaîne de résolution est la même,
 *       prolongée d'un cran ({@code … → CfgProject → CfgCompany}).</li>
 * </ul>
 *
 * <p><b>Entité non résoluble ⇒ ligne visible</b> (règle R2 de la spec, identique au §6.4 du
 * périmètre). Le prédicat est toujours {@code isActive = true OR société IS NULL}, jamais une
 * jointure interne : une prospection sans tranche (192 lignes en base réelle) ou une interaction
 * rattachée ni à une prospection ni à un dossier (1 644 lignes) n'appartient à aucune entité, et la
 * masquer perdrait le lead pour protéger une donnée que personne ne revendique.
 *
 * <p><b>Le piège, une fois pour toutes.</b> {@code path.get("cfgCompany").get("isActive")} produit
 * une <b>jointure interne implicite</b> en Criteria API : les lignes sans société disparaîtraient
 * silencieusement, en violation de R2. Toutes les jointures posées ici sont donc des
 * {@link JoinType#LEFT} explicites — c'est la raison d'être de cette classe plutôt qu'un
 * {@code cb.isTrue(...)} recopié sur cinq sites.
 *
 * <p>{@code isActive = true} et non {@code != false} : une société dont le drapeau n'a jamais été
 * renseigné reste invisible (R3, convention déjà posée par
 * {@code CfgCompanyRepository.findActivesOrderByDescriptionAsc()}).
 */
public final class VisibiliteEntites {

    private VisibiliteEntites() {
    }

    /**
     * Le prédicat de visibilité à partir d'un {@code CfgProject} <b>déjà joint en LEFT</b> par
     * l'appelant : « la société de ce projet est en exploitation, ou aucune société ne se résout ».
     *
     * <p>L'appelant fournit la jointure du projet parce qu'il l'a en général déjà posée pour le
     * périmètre commercial — les deux prédicats partagent ainsi la même chaîne au lieu de la
     * dupliquer en SQL.
     */
    public static Predicate depuisProjet(From<?, CfgProject> projet, CriteriaBuilder cb) {
        Join<CfgProject, CfgCompany> societe = projet.join("cfgCompany", JoinType.LEFT);
        return actifOuNonResolu(societe.get("isActive"), societe.get("id"), cb);
    }

    /**
     * Variante pour une racine portant une FK <b>directe</b> vers la société — le cas de
     * {@code StlSettlement.CfgCompanyId}.
     *
     * <p>Mesuré sur {@code yesRemsProd} avant de s'y fier : sur les 644 règlements de vente, la
     * société directe concorde avec celle déduite de la chaîne dossier → article → tranche →
     * projet dans <b>644 cas sur 644</b>, aucun règlement ne touchant deux sociétés. La FK directe
     * est donc exacte, et évite ici une sous-requête {@code EXISTS}.
     */
    public static Predicate depuisSocieteDirecte(From<?, ?> racine, String attribut, CriteriaBuilder cb) {
        Join<?, CfgCompany> societe = racine.join(attribut, JoinType.LEFT);
        return actifOuNonResolu(societe.get("isActive"), societe.get("id"), cb);
    }

    /**
     * Variante à deux chemins de résolution, pour {@code ComActionMessage} : une interaction porte
     * une prospection <i>et/ou</i> un dossier, et la société peut venir de l'un ou de l'autre.
     *
     * <p>L'ordre {@code coalesce} — prospection d'abord, dossier ensuite — reproduit exactement
     * celui de {@code PerimetreCommercialService.filtreInteractions()}, pour que les deux filtres
     * désignent toujours la même société. La règle « non résoluble ⇒ visible » ne s'applique
     * qu'une fois les <b>deux</b> chemins épuisés, ce qu'exprime le {@code coalesce} sur l'id.
     */
    public static Predicate depuisDeuxProjets(From<?, CfgProject> projetPrincipal,
                                              From<?, CfgProject> projetSecondaire,
                                              CriteriaBuilder cb) {
        Join<CfgProject, CfgCompany> principale = projetPrincipal.join("cfgCompany", JoinType.LEFT);
        Join<CfgProject, CfgCompany> secondaire = projetSecondaire.join("cfgCompany", JoinType.LEFT);
        return actifOuNonResolu(
                cb.coalesce(principale.get("isActive"), secondaire.get("isActive")),
                cb.coalesce(principale.get("id"), secondaire.get("id")),
                cb);
    }

    /**
     * Le prédicat lui-même, en un seul endroit : {@code actif = true OR identifiant IS NULL}.
     *
     * <p>Une société existante dont {@code IsActive} vaut {@code NULL} tombe donc du bon côté —
     * masquée — puisque {@code NULL = true} n'est pas vrai et que son id, lui, n'est pas nul.
     *
     * <p><b>{@code cb.equal(…, TRUE)} et non {@code cb.isTrue(…)}</b>, qui a cassé neuf tests.
     * SQL Server n'a pas de type booléen : sur un chemin simple, Hibernate sait rendre
     * {@code isTrue} en {@code = 1}, mais sur une expression composée — le {@code coalesce} de
     * {@link #depuisDeuxProjets} — il émet l'expression nue, et le serveur répond « expression de
     * type non booléen spécifiée dans un contexte où une condition est attendue ». La comparaison
     * explicite ne dépend pas de cette subtilité, quelle que soit la forme de l'expression.
     */
    private static Predicate actifOuNonResolu(Expression<Boolean> actif,
                                              Expression<?> identifiantSociete,
                                              CriteriaBuilder cb) {
        return cb.or(cb.equal(actif, Boolean.TRUE), cb.isNull(identifiantSociete));
    }

    /**
     * La clause JPQL équivalente, pour les requêtes {@code @Query} qui ne passent pas par une
     * {@code Specification} (tableau de bord, contrôles d'existence par id). {@code :alias} est
     * l'alias de la société, obtenu par un {@code LEFT JOIN} chez l'appelant.
     *
     * <p>Constante et non chaîne recopiée : c'est la même règle que
     * {@link #actifOuNonResolu}, et les deux formulations doivent rester alignées.
     */
    public static final String CLAUSE_JPQL = "(soc.isActive = true OR soc.id IS NULL)";
}
