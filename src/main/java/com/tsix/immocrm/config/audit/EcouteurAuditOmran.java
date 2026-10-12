package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ActionAudit;
import com.tsix.immocrm.domain.audit.RegistreAuditOmran;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit OMRAN générique : note chaque écriture d'une entité du {@link RegistreAuditOmran} dans un tampon de la
 * session, puis écrit les lignes {@code AdmWebHistorics} avant la fin de la transaction, <b>après</b> son flush — jamais
 * de persist pendant un flush. Rollback : le rappel « avant » n'est pas appelé, aucune ligne.
 */
public class EcouteurAuditOmran implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final ThreadLocal<Map<SharedSessionContractImplementor, TamponAudit>> TAMPONS =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private final RedacteurAuditOmran redacteur;

    public EcouteurAuditOmran(RedacteurAuditOmran redacteur) {
        this.redacteur = redacteur;
    }

    @Override
    public void onPostInsert(PostInsertEvent evenement) {
        noter(evenement.getSession(), evenement.getEntity(), evenement.getId(), evenement.getPersister(),
                ActionAudit.AJOUT, null, evenement.getState());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent evenement) {
        noter(evenement.getSession(), evenement.getEntity(), evenement.getId(), evenement.getPersister(),
                ActionAudit.MODIFICATION, evenement.getOldState(), evenement.getState());
    }

    @Override
    public void onPostDelete(PostDeleteEvent evenement) {
        noter(evenement.getSession(), evenement.getEntity(), evenement.getId(), evenement.getPersister(),
                ActionAudit.SUPPRESSION, evenement.getDeletedState(), null);
    }

    private void noter(SharedSessionContractImplementor session, Object entite, Object id, EntityPersister persister,
                       ActionAudit action, Object[] avant, Object[] apres) {
        if (entite == null || !(id instanceof UUID pkey) || !RegistreAuditOmran.trace(entite.getClass(), action)) {
            return;
        }
        tampon(session).noter(entite, pkey, persister, action, avant, apres);
    }

    /** Visible pour les tests : oublie ce que le fil a noté, comme si la préparation avait été validée avant. */
    static void viderTampons() {
        TAMPONS.get().values().forEach(TamponAudit::vider);
    }

    private TamponAudit tampon(SharedSessionContractImplementor session) {
        Map<SharedSessionContractImplementor, TamponAudit> tampons = TAMPONS.get();
        TamponAudit tampon = tampons.get(session);
        if (tampon != null) {
            return tampon;
        }
        // Filet : une session fermée sans rappel « après » ne doit pas rester retenue par le fil.
        tampons.keySet().removeIf(SharedSessionContractImplementor::isClosed);
        TamponAudit nouveau = new TamponAudit();
        tampons.put(session, nouveau);
        session.getTransactionCompletionCallbacks().registerCallback(
                (BeforeCompletionCallback) s -> redacteur.ecrire(s, nouveau.objets()));
        session.getTransactionCompletionCallbacks().registerCallback(
                (AfterCompletionCallback) (succes, s) -> TAMPONS.get().remove(s));
        return nouveau;
    }
}
