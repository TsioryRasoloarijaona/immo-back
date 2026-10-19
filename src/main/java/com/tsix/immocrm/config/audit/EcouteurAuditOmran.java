package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.security.UtilisateurAffiche;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Écrit une ligne {@code AdmWebHistorics} par objet du registre écrit dans une transaction (FEATURE-patrimoine-audit-actions.md
 * §2.2). Les POST_* d'un flush alimentent un tampon par session ; il est écrit après le flush du commit, avant le COMMIT
 * JDBC ({@link BeforeCompletionCallback}) : un rollback n'écrit rien. Enregistré sans condition, mono-base comprise.
 */
@Component
public class EcouteurAuditOmran implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    static final String AJOUT = "Added";
    static final String MODIFICATION = "Modified";
    static final String SUPPRESSION = "Deleted";
    private static final String VERSION = "Web: AyadERP";

    private final Map<SharedSessionContractImplementor, Map<Object, Changement>> tampons =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;
    private final JsonMapper json;
    private final DetailsAudit details;

    EcouteurAuditOmran(EntityManagerFactory fabrique, UtilisateurAffiche utilisateurAffiche, Clock clock, JsonMapper json) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
        this.json = json;
        this.details = new DetailsAudit(fabrique.getPersistenceUnitUtil());
        EventListenerRegistry registre = fabrique.unwrap(SessionFactoryImplementor.class).getEventListenerRegistry();
        registre.appendListeners(EventType.POST_INSERT, this);
        registre.appendListeners(EventType.POST_UPDATE, this);
        registre.appendListeners(EventType.POST_DELETE, this);
    }

    /** Ancien état = celui du premier flush de la transaction ; nouvel état = celui du dernier. */
    private static final class Changement {
        private final UUID id;
        private final Object entite;
        private final EntityPersister persister;
        private String action;
        private final Object[] ancien;
        private Object[] nouveau;

        private Changement(UUID id, Object entite, EntityPersister persister, String action, Object[] ancien,
                           Object[] nouveau) {
            this.id = id;
            this.entite = entite;
            this.persister = persister;
            this.action = action;
            this.ancien = ancien;
            this.nouveau = nouveau;
        }
    }

    @Override
    public void onPostInsert(PostInsertEvent e) {
        noter(e.getSession(), e.getPersister(), e.getId(), e.getEntity(), AJOUT, null, e.getState());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent e) {
        noter(e.getSession(), e.getPersister(), e.getId(), e.getEntity(), MODIFICATION, e.getOldState(), e.getState());
    }

    @Override
    public void onPostDelete(PostDeleteEvent e) {
        noter(e.getSession(), e.getPersister(), e.getId(), e.getEntity(), SUPPRESSION, e.getDeletedState(), null);
    }

    private void noter(SharedSessionContractImplementor session, EntityPersister persister, Object id, Object entite,
                       String action, Object[] ancien, Object[] nouveau) {
        if (!(id instanceof UUID pkey) || !RegistreAuditOmran.trace(persister.getMappedClass())) {
            return;
        }
        Map<Object, Changement> tampon = tampons.computeIfAbsent(session, s -> {
            // La session de l'évènement, clé du tampon ; asSessionImplementor() lève sur SessionImpl (Hibernate 7).
            s.getTransactionCompletionCallbacks().registerCallback((BeforeCompletionCallback) ignore -> ecrire(s));
            s.getTransactionCompletionCallbacks().registerCallback((AfterCompletionCallback) (ok, ignore) -> tampons.remove(s));
            return new LinkedHashMap<>();
        });
        List<Object> cle = List.of(persister.getMappedClass(), pkey);
        Changement precedent = tampon.get(cle);
        if (precedent == null) {
            tampon.put(cle, new Changement(pkey, entite, persister, action, ancien, nouveau));
        } else if (SUPPRESSION.equals(action) && AJOUT.equals(precedent.action)) {
            tampon.remove(cle);
        } else if (SUPPRESSION.equals(action)) {
            precedent.action = SUPPRESSION;
        } else {
            precedent.nouveau = nouveau;
        }
    }

    private void ecrire(SharedSessionContractImplementor session) {
        Map<Object, Changement> tampon = tampons.remove(session);
        if (tampon == null) {
            return;
        }
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        List<AdmWebHistorics> lignes = tampon.values().stream()
                .map(c -> ligne(c, auteur, maintenant)).filter(Objects::nonNull).toList();
        if (lignes.isEmpty() || !(session instanceof Session hibernate)) {
            return;
        }
        lignes.forEach(hibernate::persist);
        hibernate.flush();
    }

    private List<List<String>> changements(Changement c, Trace trace) {
        if (SUPPRESSION.equals(c.action)) {
            return List.of(List.of(trace.nomCourt(), "", Objects.toString(trace.libelleObjet(), "")));
        }
        if (c.ancien == null) {
            return details.ajout(c.persister, c.nouveau);
        }
        return details.modification(c.persister, c.ancien, c.nouveau);
    }

    private AdmWebHistorics ligne(Changement c, String auteur, LocalDateTime maintenant) {
        Trace trace = RegistreAuditOmran.tracer(c.entite).orElse(null);
        if (trace == null) {
            return null;
        }
        List<List<String>> lignes = changements(c, trace);
        if (lignes.isEmpty()) {
            return null;
        }
        AdmWebHistorics h = new AdmWebHistorics();
        h.setId(UUID.randomUUID());
        h.setUserDescription(auteur);
        h.setActionDateTime(maintenant);
        h.setObjectId(c.id);
        h.setAction(c.action);
        h.setObjectType(trace.type());
        h.setObjectTypeLabel(trace.libelleType());
        h.setObjectLabel(trace.libelleObjet());
        h.setDescription(trace.libelleObjet());
        h.setParentObjectType(trace.typeParent());
        h.setParentObjectId(trace.parentId());
        h.setSystemVersion(VERSION);
        h.setDetails(json.writeValueAsString(lignes));
        return h;
    }
}
