package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ActionAudit;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Objets écrits par une session dans la transaction en cours, un par (entité, id), dans l'ordre d'écriture. */
final class TamponAudit {

    private record Cle(String entite, UUID id) {
    }

    private final Map<Cle, ObjetModifie> objets = new LinkedHashMap<>();

    void noter(Object entite, UUID id, EntityPersister persister, ActionAudit action, Object[] avant, Object[] apres) {
        Cle cle = new Cle(persister.getEntityName(), id);
        ObjetModifie existant = objets.get(cle);
        if (existant == null) {
            objets.put(cle, new ObjetModifie(entite, id, persister, action, avant, apres));
            return;
        }
        ObjetModifie fusion = existant.suivi(action, entite, apres);
        if (fusion == null) {
            objets.remove(cle);
        } else {
            objets.put(cle, fusion);
        }
    }

    void vider() {
        objets.clear();
    }

    Collection<ObjetModifie> objets() {
        return List.copyOf(objets.values());
    }
}
