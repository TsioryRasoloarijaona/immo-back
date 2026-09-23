package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Ce qui fait d'une {@link ComProspection} une <b>pré-prospection</b> : son type de document
 * légataire, {@code StrEntity.Code = 'ComPreProspection'} — le discriminant d'OMRAN lui-même
 * (sa vue {@code ComSalePreProspectionView} filtre sur ce seul critère).
 *
 * <p>Remplace l'heuristique {@code CfgProspectId IS NULL AND CfgCommercialId IS NULL}, qui
 * classait en prospection ~830 pré-prospections OMRAN ayant déjà un prospect ou un commercial —
 * décision #44, voir {@code instructions/FEATURE-creation-pre-prospection.md} §4.
 *
 * <p>Point unique : les listes, les groupes, le dashboard et les relances doivent tous tomber
 * d'accord, sinon une même ligne serait comptée des deux côtés. Les requêtes JPQL de
 * {@code ComProspectionRepository} répètent le même prédicat en toutes lettres.
 */
public final class DiscriminantPreProspection {

    /** {@code StrEntity.Code} du type « Pré-Prospection ». */
    public static final String CODE_PRE_PROSPECTION = "ComPreProspection";

    /**
     * Identifiant du type, identique dans yesRems, yesRemsProd et yesRemsTest (référentiel
     * légataire restauré de la même source). Sert au test en mémoire d'une ligne déjà chargée ;
     * les requêtes, elles, résolvent par code.
     */
    public static final UUID ID_PRE_PROSPECTION = UUID.fromString("B92DF3D8-158C-4E1E-827A-70FEB172A264");

    private DiscriminantPreProspection() {
    }

    /** Les pré-prospections. Une ligne sans {@code StrEntityId} n'en est pas une. */
    public static Specification<ComProspection> estPreProspection() {
        return (root, query, cb) -> root.get("strEntityId").in(idsPreProspection(query, cb));
    }

    /**
     * Le complément exact de {@link #estPreProspection()} : {@code NULL} compris, sans quoi les
     * lignes sans type disparaîtraient des deux listes à la fois.
     */
    public static Specification<ComProspection> nEstPasPreProspection() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("strEntityId")),
                cb.not(root.get("strEntityId").in(idsPreProspection(query, cb))));
    }

    /** Même test, sur une ligne déjà chargée. */
    public static boolean estPreProspection(ComProspection p) {
        return ID_PRE_PROSPECTION.equals(p.getStrEntityId());
    }

    private static Subquery<UUID> idsPreProspection(CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<StrEntity> type = ids.from(StrEntity.class);
        return ids.select(type.get("id")).where(cb.equal(type.get("code"), CODE_PRE_PROSPECTION));
    }
}
