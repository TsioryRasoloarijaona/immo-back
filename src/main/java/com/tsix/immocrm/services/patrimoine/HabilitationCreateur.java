package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * La ligne {@code AdmUserHabilitation} qu'OMRAN insère pour le créateur d'un programme ou d'une tranche
 * ({@code AdmUserHabilitation.SetDefalutHabilitation}) : sans elle, l'objet est invisible dans OMRAN.
 * Une ligne par objet, une seule portée renseignée.
 */
@Component
class HabilitationCreateur {

    private static final Logger log = LoggerFactory.getLogger(HabilitationCreateur.class);

    private final AdmUserHabilitationRepository habilitationRepository;

    HabilitationCreateur(AdmUserHabilitationRepository habilitationRepository) {
        this.habilitationRepository = habilitationRepository;
    }

    void surProjet(AdmUser createur, CfgProject projet) {
        if (absent(createur, projet.getCode())) {
            return;
        }
        AdmUserHabilitation habilitation = nouvelle(createur);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);
    }

    void surTranche(AdmUser createur, CfgTranche tranche) {
        if (absent(createur, tranche.getCode())) {
            return;
        }
        AdmUserHabilitation habilitation = nouvelle(createur);
        habilitation.setCfgTranche(tranche);
        habilitationRepository.save(habilitation);
    }

    /** {@code CfgCompany.AfterInsert} : sans elle, l'entité n'apparaît ni dans OMRAN ni dans l'écran du CRM. */
    void surEntite(AdmUser createur, CfgCompany entite) {
        if (absent(createur, entite.getCode())) {
            return;
        }
        AdmUserHabilitation habilitation = nouvelle(createur);
        habilitation.setCfgCompany(entite);
        habilitationRepository.save(habilitation);
    }

    // Compte absent de la base d'écriture (UtilisateurAffiche) : pas de clé à poser, l'objet reste invisible dans OMRAN.
    private static boolean absent(AdmUser createur, String code) {
        if (createur == null) {
            log.warn("Création de {} sans compte AdmUser : aucune habilitation OMRAN posée.", code);
            return true;
        }
        return false;
    }

    private static AdmUserHabilitation nouvelle(AdmUser createur) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(createur);
        return habilitation;
    }
}
