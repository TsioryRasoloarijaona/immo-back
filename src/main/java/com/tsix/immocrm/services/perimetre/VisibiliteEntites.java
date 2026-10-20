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
 * Société active ou non résoluble, pour tous les appelants, exemptés compris
 * ({@code FEATURE-visibilite-transversale-entites.md} R1–R3). {@code get("cfgCompany")} ferait une
 * jointure interne implicite qui masquerait les lignes sans société : jointures LEFT explicites ici.
 */
public final class VisibiliteEntites {

    private static final String CFG_COMPANY = "cfgCompany";
    private static final String IS_ACTIVE = "isActive";

    private VisibiliteEntites() {
    }

    /** {@code projet} doit être déjà joint en LEFT par l'appelant, qui le partage avec le périmètre. */
    public static Predicate depuisProjet(From<?, CfgProject> projet, CriteriaBuilder cb) {
        Join<CfgProject, CfgCompany> societe = projet.join(CFG_COMPANY, JoinType.LEFT);
        return actifOuNonResolu(societe.get(IS_ACTIVE), societe.get("id"), cb);
    }

    /**
     * FK société directe ({@code StlSettlement.CfgCompanyId}) : concorde toujours avec la chaîne
     * dossier → … → projet, d'où pas de sous-requête {@code EXISTS} (§2 de la spec).
     */
    public static Predicate depuisSocieteDirecte(From<?, ?> racine, String attribut, CriteriaBuilder cb) {
        Join<?, CfgCompany> societe = racine.join(attribut, JoinType.LEFT);
        return actifOuNonResolu(societe.get(IS_ACTIVE), societe.get("id"), cb);
    }

    /**
     * Interactions : prospection d'abord, dossier ensuite — même ordre que
     * {@code PerimetreCommercialService.filtreInteractions()}, pour désigner la même société.
     */
    public static Predicate depuisDeuxProjets(From<?, CfgProject> projetPrincipal,
                                              From<?, CfgProject> projetSecondaire,
                                              CriteriaBuilder cb) {
        Join<CfgProject, CfgCompany> principale = projetPrincipal.join(CFG_COMPANY, JoinType.LEFT);
        Join<CfgProject, CfgCompany> secondaire = projetSecondaire.join(CFG_COMPANY, JoinType.LEFT);
        return actifOuNonResolu(
                cb.coalesce(principale.get(IS_ACTIVE), secondaire.get(IS_ACTIVE)),
                cb.coalesce(principale.get("id"), secondaire.get("id")),
                cb);
    }

    /**
     * {@code IsActive NULL} sur une société existante ⇒ masquée (R3). {@code cb.equal(…, TRUE)} et non
     * {@code isTrue} : sur un {@code coalesce}, Hibernate émet l'expression nue et SQL Server la refuse
     * (« expression de type non booléen »).
     */
    private static Predicate actifOuNonResolu(Expression<Boolean> actif,
                                              Expression<?> identifiantSociete,
                                              CriteriaBuilder cb) {
        return cb.or(cb.equal(actif, Boolean.TRUE), cb.isNull(identifiantSociete));
    }

    /**
     * Même règle que {@link #actifOuNonResolu}, pour les {@code @Query} ; l'appelant doit nommer
     * {@code soc} la société jointe en LEFT. Les deux formulations doivent rester alignées.
     */
    public static final String CLAUSE_JPQL = "(soc.isActive = true OR soc.id IS NULL)";}
