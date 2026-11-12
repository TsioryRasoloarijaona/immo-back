package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;

/** Seul écrivain des tables {@code PrjWorkingOut*} (FEATURE-decomptes-parite.md), dans la transaction de l'appelant. */
@Component
public class EcritureDecompte {

    // Séquence OMRAN des décomptes (AdmSequence.Code), codes « yy/nnnnn ».
    private static final String SEQUENCE = "Décompte entrant";

    private final GenerateurCodeSequence sequences;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public EcritureDecompte(GenerateurCodeSequence sequences, UtilisateurAffiche utilisateur, EntityManager em,
                            Clock clock) {
        this.sequences = sequences;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String codeSuivant() {
        return sequences.suivant(SEQUENCE, 2, 5);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void creer(AuditOmran entite) {
        String auteur = utilisateur.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        entite.setUserCreate(auteur);
        entite.setUserCreateDateTime(maintenant);
        entite.setUserUpdate(auteur);
        entite.setUserUpdateDateTime(maintenant);
        em.persist(entite);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void modifier(AuditOmran entite) {
        entite.setUserUpdate(utilisateur.courant().nom());
        entite.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void supprimer(Collection<?> entites) {
        entites.forEach(em::remove);
    }
}
