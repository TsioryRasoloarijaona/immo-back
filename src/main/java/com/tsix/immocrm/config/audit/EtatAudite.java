package com.tsix.immocrm.config.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

/**
 * « Inchangé » au sens de l'audit : aucune colonne tracée ne diffère après normalisation ({@link DetailsAudit}), donc
 * aucune ligne {@code Modified} au commit. Sert au bilan d'import (FEATURE-patrimoine-import-export.md §1.3).
 */
@Component
public class EtatAudite {

    private final EntityManager em;
    private final DetailsAudit details;

    EtatAudite(EntityManager em, EntityManagerFactory fabrique) {
        this.em = em;
        this.details = new DetailsAudit(fabrique.getPersistenceUnitUtil());
    }

    /** État courant d'une entité gérée, à repasser à {@link #modifie}. */
    public Object[] etat(Object entite) {
        Object reel = Hibernate.unproxy(entite);
        return persister(reel).getValues(reel);
    }

    public boolean modifie(Object entite, Object[] avant) {
        Object reel = Hibernate.unproxy(entite);
        EntityPersister persister = persister(reel);
        return !details.modification(persister, avant, persister.getValues(reel)).isEmpty();
    }

    private EntityPersister persister(Object entite) {
        return em.unwrap(SessionImplementor.class).getEntityPersister(null, entite);
    }
}
