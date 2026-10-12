package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderLineRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgShareholderRepository;
import com.tsix.immocrm.services.referentiel.ReferencesOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Lot P5 (FEATURE-entites-juridiques.md) : actionnaires, groupes, répartitions, habilitations société, devise MAD. */
@Component
public class FixtureEntites {

    private final EntityManager em;
    private final CfgShareholderRepository actionnaireRepository;
    private final CfgGroupShareholderRepository groupeRepository;
    private final CfgGroupShareholderLineRepository ligneGroupeRepository;
    private final CfgCompanyShareholderRepository repartitionRepository;
    private final AdmUserHabilitationRepository habilitationRepository;

    public FixtureEntites(EntityManager em, CfgShareholderRepository actionnaireRepository,
                          CfgGroupShareholderRepository groupeRepository,
                          CfgGroupShareholderLineRepository ligneGroupeRepository,
                          CfgCompanyShareholderRepository repartitionRepository,
                          AdmUserHabilitationRepository habilitationRepository) {
        this.em = em;
        this.actionnaireRepository = actionnaireRepository;
        this.groupeRepository = groupeRepository;
        this.ligneGroupeRepository = ligneGroupeRepository;
        this.repartitionRepository = repartitionRepository;
        this.habilitationRepository = habilitationRepository;
    }

    /** {@code StlCurrency} est {@code @Immutable} : MAD (PKey de production, semée en CI) insérée si absente. */
    public UUID deviseMad() {
        Number existe = (Number) em.createNativeQuery("SELECT COUNT(*) FROM StlCurrency WHERE PKey = :id")
                .setParameter("id", ReferencesOmran.ID_DEVISE_MAD).getSingleResult();
        if (existe.intValue() == 0) {
            em.createNativeQuery("INSERT INTO StlCurrency (PKey, Code, Description) VALUES (:id, 'MAD', 'Dirham')")
                    .setParameter("id", ReferencesOmran.ID_DEVISE_MAD).executeUpdate();
        }
        return ReferencesOmran.ID_DEVISE_MAD;
    }

    public CfgShareholder actionnaire(String code) {
        CfgShareholder actionnaire = new CfgShareholder();
        actionnaire.setId(UUID.randomUUID());
        actionnaire.setCode(code);
        actionnaire.setDescription("Actionnaire " + code);
        actionnaire.setNotes("");
        return actionnaireRepository.save(actionnaire);
    }

    public CfgGroupShareholder groupe(String code) {
        CfgGroupShareholder groupe = new CfgGroupShareholder();
        groupe.setId(UUID.randomUUID());
        groupe.setCode(code);
        groupe.setDescription("Groupe " + code);
        return groupeRepository.save(groupe);
    }

    public CfgGroupShareholderLine ligneGroupe(CfgGroupShareholder groupe, CfgShareholder actionnaire, String fraction) {
        CfgGroupShareholderLine ligne = new CfgGroupShareholderLine();
        ligne.setId(UUID.randomUUID());
        ligne.setCfgGroupShareholder(groupe);
        ligne.setCfgShareholder(actionnaire);
        ligne.setSharingNumber(new BigDecimal(fraction));
        return ligneGroupeRepository.save(ligne);
    }

    public CfgCompanyShareholder repartition(CfgCompany entite, CfgShareholder actionnaire, int parts) {
        CfgCompanyShareholder ligne = new CfgCompanyShareholder();
        ligne.setId(UUID.randomUUID());
        ligne.setCfgCompany(entite);
        ligne.setCfgShareholder(actionnaire);
        ligne.setSharingNumber(parts);
        return repartitionRepository.save(ligne);
    }

    /** Rôle lu dans {@code AdmUserRole} : c'est lui, pas l'autorité Spring, qui exempte du filtre d'habilitation. */
    public void donnerRole(AdmUser compte, String code) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        em.persist(role);
        AdmUserRole lien = new AdmUserRole();
        lien.setId(UUID.randomUUID());
        lien.setAdmUser(compte);
        lien.setAdmRole(role);
        em.persist(lien);
        em.flush();
    }

    public AdmUserHabilitation habilitation(AdmUser compte, CfgCompany entite) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgCompany(entite);
        return habilitationRepository.save(habilitation);
    }
}
