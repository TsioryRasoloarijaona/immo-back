package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.config.audit.EcouteurAuditOmran;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import jakarta.persistence.EntityManager;
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
        return em.createQuery("select x from %s x where x.%s = :p".formatted(g.type().getSimpleName(), g.parent()), g.type())
                .setParameter("p", parent).getResultList().stream()
                .collect(Collectors.toMap(g.cle(), Function.identity(), (a, b) -> a, HashMap::new));
    }

    /** 400 si un id est envoyé deux fois, ou si un id inconnu du parent existe ailleurs (autre fiche, autre lot). */
    <S, T> void refuserIds(Grille<S, T> g, List<S> envoi, Map<UUID, T> enBase, String champ) {
        Set<UUID> vus = new HashSet<>();
        List<UUID> ids = envoi.stream().map(g.id()).filter(Objects::nonNull).toList();
        ValidationBordereau.exiger(ids.stream().allMatch(vus::add), champ + " : id en double");
        Collection<UUID> nouveaux = ids.stream().filter(i -> !enBase.containsKey(i)).toList();
        boolean pris = !nouveaux.isEmpty() && em.createQuery("select count(x) from %s x where x.id in :ids"
                .formatted(g.type().getSimpleName()), Long.class).setParameter("ids", nouveaux).getSingleResult() > 0;
        if (pris) {
            throw new InvalidRequestException(champ + " : id d'une autre fiche ou d'un autre lot");
        }
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
