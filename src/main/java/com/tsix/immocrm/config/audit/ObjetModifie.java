package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ActionAudit;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Un objet écrit dans la transaction : état chargé au début ({@code avant}), état final ({@code apres}). Plusieurs
 * écritures du même objet se fusionnent en une seule ligne d'audit (FEATURE-patrimoine-audit-actions.md §2.2 r4).
 */
record ObjetModifie(Object entite, UUID id, EntityPersister persister, ActionAudit action, Object[] avant,
                    Object[] apres) {

    ObjetModifie {
        avant = copie(avant);
        apres = copie(apres);
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

    @Override
    public boolean equals(Object autre) {
        return autre instanceof ObjetModifie o
                && Objects.equals(entite, o.entite) && Objects.equals(id, o.id)
                && Objects.equals(persister, o.persister) && action == o.action
                && Arrays.equals(avant, o.avant) && Arrays.equals(apres, o.apres);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entite, id, persister, action, Arrays.hashCode(avant), Arrays.hashCode(apres));
    }

    @Override
    public String toString() {
        return "ObjetModifie[id=%s, action=%s, avant=%s, apres=%s]"
                .formatted(id, action, Arrays.toString(avant), Arrays.toString(apres));
    }
}
