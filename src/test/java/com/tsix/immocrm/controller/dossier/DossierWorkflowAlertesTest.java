package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-vente-workflow.md §11 : le changement de statut n'éteint que les alertes hors échéance de la
 * prospection du dossier, d'ordre inférieur ou égal au nouveau statut ; les impayés (rattachés à une échéance)
 * et les alertes d'une autre prospection restent actifs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierWorkflowAlertesTest {

    private static final String ADMIN = "Administrateur";
    private static final LocalDateTime ECHEANCE = LocalDateTime.of(2026, 1, 10, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StrStatusRepository statusRepository;
    @Autowired private EntityManager em;

    @Test
    void changementDeStatut_eteintLesRelancesDeLaProspection_epargneImpayesAutresProspectionsEtStatutsSuperieurs()
            throws Exception {
        AdmUser admin = fixture.compte("TST-ADM-AL", ADMIN);
        ordre(StatutDossier.RESERVATION, 1);
        ordre(StatutDossier.CONCRETISATION, 2);
        StrStatus formalise = ordre(StatutDossier.FORMALISE, 10);
        ComProspection prospection = prospection("PSN-WF-AL-1");
        ComProspection autre = prospection("PSN-WF-AL-2");
        ComFolder dossier = fixture.dossier("TST-WF-AL", fixture.tranche("TST-WF-AL", true), null);
        dossier.setStrStatus(statuts.statut(StatutDossier.RESERVATION));
        dossier.setComProspection(prospection);
        folderRepository.save(dossier);
        ComDocument echeance = fixture.echeance(dossier,
                fixture.categorieEcheance("TST-AVANCE-AL", true, false, false, null), new BigDecimal("1000"), ECHEANCE);

        AdmAlertData relance = alerte(message(prospection, null, false, null));
        AdmAlertData impaye = alerte(message(prospection, echeance, true, null));
        AdmAlertData autreProspection = alerte(message(autre, null, false, null));
        AdmAlertData statutSuperieur = alerte(message(prospection, null, false, formalise.getId()));
        em.flush();

        mockMvc.perform(patch("/api/dossiers/{id}/statut", dossier.getId())
                        .with(FixtureDossierVente.connecte(admin, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CONCRETISATION\"}"))
                .andExpect(status().isOk());
        em.flush();
        em.clear();

        assertAll(
                () -> assertEquals(Boolean.FALSE, actif(relance), "relance de la prospection éteinte"),
                () -> assertEquals(Boolean.TRUE, actif(impaye), "impayé épargné"),
                () -> assertEquals(Boolean.TRUE, actif(autreProspection), "autre prospection intacte"),
                () -> assertEquals(Boolean.TRUE, actif(statutSuperieur), "alerte d'un statut d'ordre supérieur intacte"));
    }

    private StrStatus ordre(StatutDossier code, int ordre) {
        StrStatus s = statuts.statut(code);
        s.setSorting(ordre);
        return statusRepository.save(s);
    }

    private ComProspection prospection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        em.persist(p);
        return p;
    }

    private ComActionMessage message(ComProspection prospection, ComDocument echeance, boolean impaye, UUID statutId) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(ECHEANCE);
        m.setDescription("TST-ALERTE");
        m.setIsIn(false);
        m.setComProspection(prospection);
        m.setComDocument(echeance);
        m.setIsUnpaid(impaye);
        m.setWithoutRevival(false);
        m.setAlertValidityDate(ECHEANCE);
        m.setStrFolderStatusId(statutId);
        em.persist(m);
        return m;
    }

    private AdmAlertData alerte(ComActionMessage message) {
        AdmAlertData a = new AdmAlertData();
        a.setId(UUID.randomUUID());
        a.setCode(a.getId().toString());
        a.setDescription("TST-ALERTE");
        a.setRelatedKey(message.getId());
        a.setAlertDate(ECHEANCE);
        a.setActive(true);
        em.persist(a);
        return a;
    }

    private Boolean actif(AdmAlertData alerte) {
        return em.find(AdmAlertData.class, alerte.getId()).getActive();
    }
}
