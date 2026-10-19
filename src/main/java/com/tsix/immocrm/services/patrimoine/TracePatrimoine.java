package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/** Trace {@code UserCreate*}/{@code UserUpdate*} et habilitation du créateur (FEATURE-patrimoine-ecriture.md §2.3, §2.7). */
@Component
public class TracePatrimoine {

    private final UtilisateurAffiche utilisateurAffiche;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final Clock clock;

    public TracePatrimoine(UtilisateurAffiche utilisateurAffiche, AdmUserHabilitationRepository habilitationRepository,
                           Clock clock) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.habilitationRepository = habilitationRepository;
        this.clock = clock;
    }

    public record Trace(String auteur, LocalDateTime le) {
    }

    public Trace maintenant() {
        return new Trace(utilisateurAffiche.courant().nom(), LocalDateTime.now(clock));
    }

    /**
     * OMRAN {@code SetDefalutHabilitation} : une ligne au créateur, sans quoi l'objet est invisible dans OMRAN. Un compte
     * absent de la base d'écriture (deux bases) ne peut pas porter la clé étrangère : pas de ligne.
     */
    public void habiliterCreateur(CfgProject projet, CfgTranche tranche) {
        AdmUser compte = utilisateurAffiche.courant().compte();
        if (compte == null) {
            return;
        }
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(projet);
        habilitation.setCfgTranche(tranche);
        habilitationRepository.save(habilitation);
    }
}
