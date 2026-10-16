package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.mail.EnvoiEmailMemoire;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.repositories.com.ActionDossierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actions et instructions du dossier — FEATURE-vente-fiche.md §B1, §B2. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierActionsTest {

    private static final String ACTIONS = "/api/dossiers/{id}/actions";
    private static final String INSTRUCTIONS = "/api/dossiers/{id}/instructions";
    private static final String LUE = "/api/dossiers/{id}/instructions/{instructionId}/lue";
    private static final String FICHE = "/api/dossiers/{id}/fiche";
    private static final String COMMERCIAL = "Commercial";
    private static final String ADMIN = "Administrateur";
    private static final String NOTE_X = "{\"note\":\"x\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureInteraction fixtureInteraction;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ActionDossierRepository actionRepository;
    @Autowired private EnvoiEmailMemoire envoiEmail;

    private ComFolder dossier;
    private AdmUser commercial;

    @BeforeEach
    void setUp() {
        envoiEmail.vider();
        fixture.categoriesAction();
        fixture.typeInstruction();
        dossier = fixture.dossier("DOS-ACT", fixture.tranche("ACT-TR", true), StatutDossier.RESERVATION);
        commercial = fixture.compte("COM-ACT", COMMERCIAL);
        commercial.getCfgEmployee().setEMail("commercial.test@ayad.test");
        tierRepository.save(commercial.getCfgEmployee());
        dossier.setCfgCommercial(commercial.getCfgEmployee());
        folderRepository.save(dossier);
    }

    @Test
    @WithMockUser
    void creerPuisLister_actionDeLaCategorieDuStatut_etCompteurParType() throws Exception {
        UUID typeNote = fixtureInteraction.typeNote().getId();
        String corps = "{\"typeId\":\"" + typeNote + "\",\"note\":\"Client rappelé\"}";

        mockMvc.perform(post(ACTIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("Client rappelé"))
                .andExpect(jsonPath("$.instruction").value(false));
        mockMvc.perform(post(ACTIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ACTIONS, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions.length()").value(2))
                .andExpect(jsonPath("$.parType[0].typeId").value(typeNote.toString()))
                .andExpect(jsonPath("$.parType[0].nombre").value(2));

        ComActionMessage cree = actionRepository.actionsDuDossier(dossier.getId()).get(0);
        assertThat(cree.getComActionMessageCategory().getId()).isEqualTo(ActionsDossier.CATEGORIE_RESERVATION);
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getUserUpdateDateTime()).isNotNull();
    }

    @Test
    @WithMockUser
    void creer_sansType_400() throws Exception {
        mockMvc.perform(post(ACTIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(NOTE_X))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void creer_dossierSansStatut_409() throws Exception {
        dossier.setStrStatus(null);
        folderRepository.save(dossier);
        String corps = "{\"typeId\":\"" + fixtureInteraction.typeNote().getId() + "\",\"note\":\"x\"}";

        mockMvc.perform(post(ACTIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void lister_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(ACTIONS, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void instruction_creeNonLuePourLeCommercial_etEnvoieLeMail() throws Exception {
        AdmUser chef = fixture.compte("CHEF-ACT", "Commercial Chef");

        mockMvc.perform(post(INSTRUCTIONS, dossier.getId()).with(connecte(chef, "Commercial Chef"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Rappeler le notaire\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                .andExpect(jsonPath("$.instruction.instruction").value(true))
                .andExpect(jsonPath("$.instruction.nonLue").value(true))
                .andExpect(jsonPath("$.instruction.destinataireId").value(commercial.getCfgEmployee().getId().toString()));

        assertThat(envoiEmail.dernier()).isNotNull()
                .extracting(MessageEmail::destinataire).isEqualTo("commercial.test@ayad.test");

        mockMvc.perform(get(FICHE, dossier.getId()).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructionNonLue").value(true));
    }

    @Test
    void instruction_parUnCommercialSimple_403() throws Exception {
        mockMvc.perform(post(INSTRUCTIONS, dossier.getId()).with(connecte(commercial, COMMERCIAL))
                        .contentType(MediaType.APPLICATION_JSON).content(NOTE_X))
                .andExpect(status().isForbidden());
    }

    @Test
    void instruction_dossierSansCommercial_409() throws Exception {
        dossier.setCfgCommercial(null);
        folderRepository.save(dossier);
        AdmUser admin = fixture.compte("ADM-ACT", ADMIN);

        mockMvc.perform(post(INSTRUCTIONS, dossier.getId()).with(connecte(admin, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(NOTE_X))
                .andExpect(status().isConflict());
    }

    @Test
    void marquerLue_parLeDestinataire_effaceLIcone_etRefuseUnTiers() throws Exception {
        AdmUser admin = fixture.compte("ADM-LUE", ADMIN);
        String reponse = mockMvc.perform(post(INSTRUCTIONS, dossier.getId()).with(connecte(admin, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"À lire\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String instructionId = com.jayway.jsonpath.JsonPath.read(reponse, "$.instruction.id");
        AdmUser autre = fixture.compte("AUTRE-LUE", COMMERCIAL);

        mockMvc.perform(patch(LUE, dossier.getId(), instructionId).with(connecte(autre, COMMERCIAL)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(LUE, dossier.getId(), instructionId).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonLue").value(false));

        mockMvc.perform(get(FICHE, dossier.getId()).with(connecte(commercial, COMMERCIAL)))
                .andExpect(jsonPath("$.instructionNonLue").value(false));
    }

    @Test
    void marquerLue_instructionInconnue_404() throws Exception {
        mockMvc.perform(patch(LUE, dossier.getId(), UUID.randomUUID()).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isNotFound());
    }
}
