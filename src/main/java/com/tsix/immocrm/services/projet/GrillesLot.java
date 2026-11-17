package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.config.audit.EcouteurAuditOmran;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grilles envoyées entières des fiches du lot (FEATURE-lot-avenants-avances.md §3, R17) : id inconnu = insertion avec cet id,
 * présent = modification (UserUpdate si une valeur change), absent de l'envoi = suppression.
 */
@Component
class GrillesLot {

    private final EntityManager em;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;
    private final EcouteurAuditOmran audit;

    GrillesLot(EntityManager em, UtilisateurAffiche utilisateur, Clock clock, EcouteurAuditOmran audit) {
        this.em = em;
        this.utilisateur = utilisateur;
        this.clock = clock;
        this.audit = audit;
    }

    /** Comment lire, créer, remplir et comparer une ligne {@code T} depuis son envoi {@code S}. */
    record Grille<S, T>(Class<T> type, String parent, Function<S, UUID> id, Function<T, UUID> cle, Function<UUID, T> nouvelle,
                        BiConsumer<T, S> remplir, Function<T, List<Object>> etat) {
    }

    /** Auteur et instant d'un enregistrement ; pose les colonnes d'audit des tables qui en ont. */
    record Ecriture(String auteur, LocalDateTime maintenant) {
        void cree(Object ligne) {
            if (ligne instanceof LigneOmran l) {
                l.setUserCreate(auteur);
                l.setUserCreateDateTime(maintenant);
            } else if (ligne instanceof AuditOmran a) {
                a.setUserCreate(auteur);
                a.setUserCreateDateTime(maintenant);
            }
        }

        void modifie(Object ligne) {
            if (ligne instanceof LigneOmran l) {
                l.setUserUpdate(auteur);
                l.setUserUpdateDateTime(maintenant);
            } else if (ligne instanceof AuditOmran a) {
                a.setUserUpdate(auteur);
                a.setUserUpdateDateTime(maintenant);
            }
        }
    }

    Ecriture ecriture() {
        return new Ecriture(utilisateur.courant().nom(), LocalDateTime.now(clock));
    }

    /** Lignes en base du parent, par id. */
    <S, T> Map<UUID, T> enBase(Grille<S, T> g, UUID parent) {
        CriteriaQuery<T> q = em.getCriteriaBuilder().createQuery(g.type());
        Root<T> x = q.from(g.type());
        q.select(x).where(em.getCriteriaBuilder().equal(x.get(g.parent()), parent));
        return em.createQuery(q).getResultList().stream()
                .collect(Collectors.toMap(g.cle(), Function.identity(), (a, b) -> a, HashMap::new));
    }

    /** 400 si un id est envoyé deux fois, ou si un id inconnu du parent existe ailleurs (autre fiche, autre lot). */
    <S, T> void refuserIds(Grille<S, T> g, List<S> envoi, Map<UUID, T> enBase, String champ) {
        Set<UUID> vus = new HashSet<>();
        List<UUID> ids = envoi.stream().map(g.id()).filter(Objects::nonNull).toList();
        ValidationBordereau.exiger(ids.stream().allMatch(vus::add), champ + " : id en double");
        Collection<UUID> nouveaux = ids.stream().filter(i -> !enBase.containsKey(i)).toList();
        if (!nouveaux.isEmpty() && nombrePris(g.type(), nouveaux) > 0) {
            throw new InvalidRequestException(champ + " : id d'une autre fiche ou d'un autre lot");
        }
    }

    private <T> long nombrePris(Class<T> type, Collection<UUID> ids) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<T> x = q.from(type);
        q.select(cb.count(x)).where(x.get("id").in(ids));
        return em.createQuery(q).getSingleResult();
    }

    /** Applique l'envoi ; {@code enBase} est vidé de ses lignes gardées, les autres sont supprimées. */
    <S, T> void enregistrer(Grille<S, T> g, List<S> envoi, Map<UUID, T> enBase, Ecriture e) {
        for (S s : envoi) {
            UUID id = Objects.requireNonNullElseGet(g.id().apply(s), UUID::randomUUID);
            T existant = enBase.remove(id);
            if (existant == null) {
                T cree = g.nouvelle().apply(id);
                g.remplir().accept(cree, s);
                e.cree(cree);
                em.persist(cree);
            } else {
                List<Object> avant = g.etat().apply(existant);
                g.remplir().accept(existant, s);
                if (!avant.equals(g.etat().apply(existant))) {
                    e.modifie(existant);
                }
            }
        }
        enBase.values().forEach(em::remove);
    }

    /** Écrit en base puis ajoute la ligne d'historique du lot (R17 : ObjectId = le lot). */
    void tracer(PrjMarket lot, String resume) {
        em.flush();
        em.persist(audit.resume(lot, lot.getId(), resume));
    }
}
