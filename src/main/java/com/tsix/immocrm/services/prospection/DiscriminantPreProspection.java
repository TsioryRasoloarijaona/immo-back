package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Une pré-prospection = {@code StrEntity.Code = 'ComPreProspection'}, le discriminant d'OMRAN
 * (voir {@code instructions/FEATURE-creation-pre-prospection.md} §4). Point unique, sinon une
 * ligne serait comptée des deux côtés ; les JPQL de {@code ComProspectionRepository} le répètent.
 */
public final class DiscriminantPreProspection {

    public static final String CODE_PRE_PROSPECTION = "ComPreProspection";

    // Même PKey dans toutes les bases (référentiel restauré d'une même source). Sert au test en
    // mémoire ; les requêtes, elles, résolvent par code.
    public static final UUID ID_PRE_PROSPECTION = UUID.fromString("B92DF3D8-158C-4E1E-827A-70FEB172A264");

    private static final String STR_ENTITY_ID = "strEntityId";

    private DiscriminantPreProspection() {
    }

    /** Les pré-prospections. Une ligne sans {@code StrEntityId} n'en est pas une. */
    public static Specification<ComProspection> estPreProspection() {
        return (root, query, cb) -> root.get(STR_ENTITY_ID).in(idsPreProspection(query, cb));
    }

    /**
     * Le complément exact de {@link #estPreProspection()} : {@code NULL} compris, sans quoi les
     * lignes sans type disparaîtraient des deux listes à la fois.
     */
    public static Specification<ComProspection> nEstPasPreProspection() {
        return (root, query, cb) -> nEstPasPreProspection(root, query, cb);
    }

    /** Même prédicat quand la prospection est une <b>jointure</b> (relances, depuis {@code ComActionMessage}). */
    public static Predicate nEstPasPreProspection(From<?, ComProspection> prospection,
                                                  CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.or(
                cb.isNull(prospection.get(STR_ENTITY_ID)),
                cb.not(prospection.get(STR_ENTITY_ID).in(idsPreProspection(query, cb))));
    }

    /** Même test, sur une ligne déjà chargée. */
    public static boolean estPreProspection(ComProspection p) {
        return p != null && ID_PRE_PROSPECTION.equals(p.getStrEntityId());
    }

    private static Subquery<UUID> idsPreProspection(CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<StrEntity> type = ids.from(StrEntity.class);
        return ids.select(type.get("id")).where(cb.equal(type.get("code"), CODE_PRE_PROSPECTION));
    }
}
