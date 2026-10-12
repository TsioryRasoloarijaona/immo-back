package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ActionAudit;
import org.hibernate.persister.entity.EntityPersister;

import java.util.UUID;

/**
 * Un objet écrit dans la transaction : état chargé au début ({@code avant}), état final ({@code apres}). Plusieurs
 * écritures du même objet se fusionnent en une seule ligne d'audit (FEATURE-patrimoine-audit-actions.md §2.2 r4).
 */
final class ObjetModifie {

    private final Object entite;
    private final UUID id;
    private final EntityPersister persister;
    private final ActionAudit action;
    private final Object[] avant;
    private final Object[] apres;

    ObjetModifie(Object entite, UUID id, EntityPersister persister, ActionAudit action, Object[] avant, Object[] apres) {
        this.entite = entite;
        this.id = id;
        this.persister = persister;
        this.action = action;
        this.avant = copie(avant);
        this.apres = copie(apres);
    }

    /** {@code null} : l'objet créé puis supprimé dans la même transaction n'a jamais existé pour OMRAN. */
    ObjetModifie suivi(ActionAudit suivante, Object entiteCourante, Object[] etat) {
        return switch (action) {
            case AJOUT -> suivante == ActionAudit.SUPPRESSION
                    ? null : new ObjetModifie(entiteCourante, id, persister, ActionAudit.AJOUT, null, etat);
            case MODIFICATION -> suivante == ActionAudit.SUPPRESSION
                    ? new ObjetModifie(entiteCourante, id, persister, ActionAudit.SUPPRESSION, avant, null)
                    : new ObjetModifie(entiteCourante, id, persister, ActionAudit.MODIFICATION, avant, etat);
            case SUPPRESSION -> new ObjetModifie(entiteCourante, id, persister, ActionAudit.MODIFICATION, avant, etat);
        };
    }

    private static Object[] copie(Object[] etat) {
        return etat == null ? null : etat.clone();
    }

    Object entite() {
        return entite;
    }

    UUID id() {
        return id;
    }

    EntityPersister persister() {
        return persister;
    }

    ActionAudit action() {
        return action;
    }

    Object[] avant() {
        return avant;
    }

    Object[] apres() {
        return apres;
    }
}
