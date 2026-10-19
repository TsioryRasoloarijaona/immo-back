package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.cfg.CfgProjectZoneRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Suppressions du programme, de ses zones (FEATURE-patrimoine-tranche-fiche.md §3.3, §3.4) et de la tranche
 * (FEATURE-patrimoine-tranche-onglets.md §4), dans la transaction de l'appelant. Les cascades OMRAN passent par les
 * entités pour être tracées et rapatriées dans la copie.
 */
@Component
public record SuppressionProjet(EntityManager em, CfgProjectZoneRepository zones) {

    private static final String ID = "id";
    private static final List<String> PIECES_ET_NOTES = List.of(
            "SELECT a FROM AdmAttachedFile a WHERE a.categoryId = :id",
            "SELECT m FROM AdmMessage m WHERE m.categoryId = :id");

    /** Table qui bloque la suppression : requête COUNT et libellé du message 409. */
    private record Bloquant(String jpql, String libelle) {
    }

    void projet(CfgProject projet) {
        supprimer(projet, projet.getId(), "le programme", List.of(
                new Bloquant("SELECT COUNT(t) FROM CfgTranche t WHERE t.cfgProject.id = :id", "tranche(s)"),
                new Bloquant("SELECT COUNT(i) FROM StkItem i WHERE i.cfgProject.id = :id", "article(s)")), List.of(
                "SELECT h FROM AdmUserHabilitation h WHERE h.cfgProject.id = :id",
                "SELECT z FROM CfgProjectZone z WHERE z.cfgProject.id = :id"));
    }

    /** {@code CfgTrancheStkItem} (FK NO_ACTION) est supprimée par l'application, comme OMRAN. */
    void tranche(CfgTranche tranche) {
        supprimer(tranche, tranche.getId(), "la tranche", List.of(
                new Bloquant("SELECT COUNT(i) FROM StkItem i WHERE i.stkHierarchy.id = :id", "article(s)"),
                new Bloquant("SELECT COUNT(p) FROM ComProspection p WHERE p.stkHierarchy.id = :id", "prospection(s)"),
                new Bloquant("SELECT COUNT(p) FROM ComProspectionProject p WHERE p.stkHierarchy.id = :id",
                        "projet(s) de prospection"),
                new Bloquant("SELECT COUNT(t) FROM StkPresentationType t WHERE t.stkHierarchy.id = :id",
                        "type(s) de présentation"),
                new Bloquant("SELECT COUNT(q) FROM StlQuote q WHERE q.cfgTranche.id = :id", "offre(s)")), List.of(
                "SELECT l FROM CfgTrancheStkItem l WHERE l.cfgTranche.id = :id",
                "SELECT l FROM CfgTrancheLevel l WHERE l.cfgTranche.id = :id",
                "SELECT l FROM CfgTrancheCoveredFloor l WHERE l.cfgTranche.id = :id",
                "SELECT l FROM CfgTrancheSettlementCategory l WHERE l.cfgTranche.id = :id",
                "SELECT h FROM AdmUserHabilitation h WHERE h.cfgTranche.id = :id"));
    }

    void zone(CfgProjectZone zone) {
        if (zones.countByParentId(zone.getId()) > 0) {
            throw new ConflictException("Suppression impossible : la zone %s a des sous-zones.".formatted(zone.getCode()));
        }
        zones.delete(zone);
        flushOuConflit("la zone");
    }

    void ajouterSiPresent(List<String> bloquants, String jpql, UUID id, String libelle) {
        long n = compter(jpql, id);
        if (n > 0) {
            bloquants.add(n + " " + libelle);
        }
    }

    /** 409 listant chaque bloquant présent avec son nombre de lignes, sinon cascades puis suppression. */
    private void supprimer(Object entite, UUID id, String objet, List<Bloquant> bloquants, List<String> cascades) {
        List<String> presents = bloquants.stream()
                .map(b -> Map.entry(compter(b.jpql(), id), b.libelle()))
                .filter(b -> b.getKey() > 0)
                .map(b -> b.getKey() + " " + b.getValue())
                .toList();
        if (!presents.isEmpty()) {
            throw new ConflictException("Suppression impossible : %s porte encore %s.".formatted(objet, String.join(", ", presents)));
        }
        cascades.forEach(jpql -> supprimerTout(jpql, id));
        PIECES_ET_NOTES.forEach(jpql -> supprimerTout(jpql, id));
        em.remove(entite);
        flushOuConflit(objet);
    }

    private long compter(String jpql, UUID id) {
        return em.createQuery(jpql, Long.class).setParameter(ID, id).getSingleResult();
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
