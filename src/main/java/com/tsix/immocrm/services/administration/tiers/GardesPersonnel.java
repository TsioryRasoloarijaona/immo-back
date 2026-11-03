package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.services.dossier.DossierSorti;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/** Règles 7 et 9 de FEATURE-admin-personnels.md : contrôle explicite, yesRemsTest n'a pas les clés étrangères. */
@Component
record GardesPersonnel(AdmUserRepository comptes, EntityManager em) {

    void refuserCompteLie(CfgTier employe) {
        if (!comptes.findByCfgEmployeeIdIn(List.of(employe.getId())).isEmpty()) {
            throw new ConflictException("Cet employé est lié à un compte utilisateur : détachez d'abord le compte.");
        }
    }

    void refuserTransformation(CfgTier employe) {
        refuserCompteLie(employe);
        boolean commercialActif = em.createQuery("""
                        select d from ComFolder d left join fetch d.strStatus where d.cfgCommercial = :employe""", ComFolder.class)
                .setParameter("employe", employe).getResultList().stream().anyMatch(Predicate.not(DossierSorti::estSorti));
        if (commercialActif) {
            throw new ConflictException("Cet employé est le commercial d'un dossier actif : réaffectez d'abord le dossier.");
        }
    }
}
