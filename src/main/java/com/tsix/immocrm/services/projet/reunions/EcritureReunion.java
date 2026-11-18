package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.bul.ContactReunionOmran;
import com.tsix.immocrm.security.UtilisateurAffiche;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

/** Seul écrivain des tables {@code Bul*} d'une réunion, dans la transaction de l'appelant. */
@Component
class EcritureReunion {

    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    EcritureReunion(UtilisateurAffiche utilisateur, EntityManager em, Clock clock) {
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    <T extends LigneOmran> T persister(T ligne) {
        String auteur = utilisateur.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        ligne.setId(UUID.randomUUID());
        ligne.setUserCreate(auteur);
        ligne.setUserCreateDateTime(maintenant);
        ligne.setUserUpdate(auteur);
        ligne.setUserUpdateDateTime(maintenant);
        em.persist(ligne);
        return ligne;
    }

    // BulMeetingContact / BulDelegateContact n'ont pas de colonnes d'audit.
    <T extends ContactReunionOmran> T persister(T cible, UUID contactId, Integer ordre) {
        cible.setId(UUID.randomUUID());
        cible.setContactId(contactId);
        cible.setSorting(ordre);
        em.persist(cible);
        return cible;
    }

    void modifier(LigneOmran ligne) {
        ligne.setUserUpdate(utilisateur.courant().nom());
        ligne.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    void supprimer(Collection<?> lignes) {
        lignes.forEach(em::remove);
    }
}
