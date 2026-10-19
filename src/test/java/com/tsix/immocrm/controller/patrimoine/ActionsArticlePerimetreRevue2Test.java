package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 2e passe de revue sur le correctif périmètre de GET /api/articles/{id}/actions
 * (FEATURE-patrimoine-audit-actions.md §3, §11.10) : chemins prospection seule, ni dossier ni prospection,
 * prospection sans tranche + dossier hors périmètre, et société inactive.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsArticlePerimetreRevue2Test {

    private static final String COMMERCIAL = "Commercial";
    private static final String PURE = "AUDACT-REV2 action pure article";
    private static final String PROSP_HORS = "AUDACT-REV2 prospection hors perimetre";
    private static final String PROSP_INACTIVE = "AUDACT-REV2 prospection societe inactive";
    private static final String PROSP_SANS_TRANCHE_DOS_HORS = "AUDACT-REV2 prospection sans tranche dossier hors";
    private static final String PROSP_MIENNE = "AUDACT-REV2 prospection du perimetre";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private ComActionMessageRepository actionRepository;
    @Autowired private ComProspectionRepository prospectionRepository;

    private StkItem article;
    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() {
        CfgTranche maTranche = fixture.tranche("AUDACT-REV2-A", true);
        CfgTranche autreTranche = fixture.tranche("AUDACT-REV2-B", true);
        CfgTranche trancheInactive = fixture.tranche("AUDACT-REV2-C", false);
        ComFolder monDossier = fixture.dossier("AUDACT-REV2-DOS-A", maTranche, StatutDossier.RESERVATION);
        ComFolder dossierHors = fixture.dossier("AUDACT-REV2-DOS-B", autreTranche, StatutDossier.RESERVATION);
        article = monDossier.getStkItem();

        AdmUser compte = fixture.compte("AUDACT-REV2-COM", COMMERCIAL);
        habiliter(compte, maTranche.getCfgProject());
        habiliter(compte, trancheInactive.getCfgProject());
        commercial = connecte(compte, COMMERCIAL);

        action(null, null, PURE);
        action(prospection("AUDACT-REV2-PSN-B", autreTranche), null, PROSP_HORS);
        action(prospection("AUDACT-REV2-PSN-C", trancheInactive), null, PROSP_INACTIVE);
        action(prospection("AUDACT-REV2-PSN-0", null), dossierHors, PROSP_SANS_TRANCHE_DOS_HORS);
        action(prospection("AUDACT-REV2-PSN-A", maTranche), null, PROSP_MIENNE);
    }

    private void habiliter(AdmUser compte, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);
    }

    private ComProspection prospection(String code, CfgTranche tranche) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(fixture.tier("CLI-" + code));
        p.setStkHierarchy(tranche);
        return prospectionRepository.save(p);
    }

    private void action(ComProspection prospection, ComFolder dossier, String message) {
        ComActionMessage action = new ComActionMessage();
        action.setId(UUID.randomUUID());
        action.setStkItem(article);
        action.setComProspection(prospection);
        action.setComFolder(dossier);
        action.setDocDate(LocalDateTime.of(2026, 3, 1, 10, 0));
        action.setAlertMessage(message);
        action.setUserName("Auteur AUDACT-REV2");
        actionRepository.saveAndFlush(action);
    }

    @Test
    void commercialRestreint_voitActionsPuresEtDuPerimetre_pasLesAutres() throws Exception {
        String corps = mockMvc.perform(get("/api/articles/{id}/actions", article.getId()).with(commercial))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps).contains(PURE, PROSP_MIENNE)
                .doesNotContain(PROSP_HORS, PROSP_INACTIVE, PROSP_SANS_TRANCHE_DOS_HORS);
    }
}
