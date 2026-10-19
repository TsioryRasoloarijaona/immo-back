package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.cfg.CfgProjectZoneRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Suppressions du programme et de ses zones (FEATURE-patrimoine-tranche-fiche.md §3.3, §3.4), dans la transaction
 * de {@link ProjetService}. Les cascades OMRAN passent par les entités pour être rapatriées dans la copie.
 */
@Component
public record SuppressionProjet(EntityManager em, CfgProjectZoneRepository zones) {

    private static final String ID = "id";

    void projet(CfgProject projet) {
        UUID id = projet.getId();
        List<String> bloquants = new ArrayList<>();
        ajouterSiPresent(bloquants, "SELECT COUNT(t) FROM CfgTranche t WHERE t.cfgProject.id = :id", id, "tranche(s)");
        ajouterSiPresent(bloquants, "SELECT COUNT(i) FROM StkItem i WHERE i.cfgProject.id = :id", id, "article(s)");
        if (!bloquants.isEmpty()) {
            throw new ConflictException("Suppression impossible : le programme porte encore "
                    + String.join(", ", bloquants) + ".");
        }
        supprimerTout("SELECT h FROM AdmUserHabilitation h WHERE h.cfgProject.id = :id", id);
        supprimerTout("SELECT z FROM CfgProjectZone z WHERE z.cfgProject.id = :id", id);
        supprimerTout("SELECT a FROM AdmAttachedFile a WHERE a.categoryId = :id", id);
        supprimerTout("SELECT m FROM AdmMessage m WHERE m.categoryId = :id", id);
        em.remove(projet);
        flushOuConflit("le programme");
    }

    void zone(CfgProjectZone zone) {
        if (zones.countByParentId(zone.getId()) > 0) {
            throw new ConflictException("Suppression impossible : la zone %s a des sous-zones.".formatted(zone.getCode()));
        }
        zones.delete(zone);
        flushOuConflit("la zone");
    }

    void ajouterSiPresent(List<String> bloquants, String jpql, UUID id, String libelle) {
        long n = em.createQuery(jpql, Long.class).setParameter(ID, id).getSingleResult();
        if (n > 0) {
            bloquants.add(n + " " + libelle);
        }
    }

    void supprimerTout(String jpql, UUID id) {
        em.createQuery(jpql, Object.class).setParameter(ID, id).getResultList().forEach(em::remove);
    }

    /** Toute autre clé étrangère NO_ACTION (SavClaim, StlItemPricing…) bloque en base : 409 nommant la contrainte. */
    void flushOuConflit(String objet) {
        try {
            em.flush();
        } catch (ConstraintViolationException e) {
            throw new ConflictException("Suppression impossible : %s est encore référencé(e) ailleurs (%s)."
                    .formatted(objet, e.getConstraintName()));
        }
    }
}
