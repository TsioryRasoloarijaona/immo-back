package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
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
 * Revue FEATURE-patrimoine-audit-actions.md §3 : GET /api/articles/{id}/actions rend toute action portant StkItemId,
 * y compris celles d'un dossier ou d'une prospection. Un commercial restreint ne doit pas y lire les actions d'un
 * dossier hors de son périmètre, que GET /api/dossiers/{id}/actions lui refuse (404).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsArticlePerimetreRevueTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String NOTE_CONFIDENTIELLE = "Note confidentielle AUDACT-REV";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private ComActionMessageRepository actionRepository;

    private ComFolder horsPerimetre;
    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() {
        CfgTranche maTranche = fixture.tranche("AUDACT-REV-A", true);
        CfgTranche autreTranche = fixture.tranche("AUDACT-REV-B", true);
        horsPerimetre = fixture.dossier("AUDACT-REV-DOS-B", autreTranche, StatutDossier.RESERVATION);

        AdmUser compte = fixture.compte("AUDACT-REV-COM", COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(maTranche.getCfgProject());
        habilitationRepository.save(habilitation);
        commercial = connecte(compte, COMMERCIAL);

        ComActionMessage action = new ComActionMessage();
        action.setId(UUID.randomUUID());
        action.setComFolder(horsPerimetre);
        action.setStkItem(horsPerimetre.getStkItem());
        action.setDocDate(LocalDateTime.of(2026, 3, 1, 10, 0));
        action.setAlertMessage(NOTE_CONFIDENTIELLE);
        action.setUserName("Auteur AUDACT-REV");
        actionRepository.saveAndFlush(action);
    }

    @Test
    void temoin_actionsDuDossierHorsPerimetre_404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/actions", horsPerimetre.getId()).with(commercial))
                .andExpect(status().isNotFound());
    }

    @Test
    void actionsDeLArticle_neRevelentPasLesActionsDUnDossierHorsPerimetre() throws Exception {
        String corps = mockMvc.perform(get("/api/articles/{id}/actions", horsPerimetre.getStkItem().getId())
                        .with(commercial))
                .andReturn().getResponse().getContentAsString();

        assertThat(corps).doesNotContain(NOTE_CONFIDENTIELLE);
    }
}
