package com.tsix.immocrm.controller.hotline;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/hotline/recherche} — phone-number-first lookup of everything known about a
 * caller. See {@code HotlineService}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class HotlineControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComActionMessageRepository actionMessageRepository;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private AdmUserRepository admUserRepository;

    @Test
    void recherche_matchesTierRegardlessOfPhoneFormat_andReturnsItsHistory() throws Exception {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("HOTLINE-TIER-1");
        tier.setFirstName("Amina");
        tier.setLastName("HOTLINE-TEST");
        // Stored with the country code and spaces, like a chunk of the real legacy data.
        tier.setMobile("+212 6 12 34 56 78");
        tierRepository.save(tier);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-HOTLINE-TEST");
        vocation = vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-HOTLINE-TEST");
        folder.setCfgCustomer(tier);
        folder.setStkVocation(vocation);
        folder.setDocDate(LocalDateTime.now());
        folderRepository.save(folder);

        ComActionMessage interaction = new ComActionMessage();
        interaction.setId(UUID.randomUUID());
        interaction.setCfgTier(tier);
        interaction.setDocDate(LocalDateTime.now());
        // Fixture alignée sur la base réelle : le texte du commercial va dans AlertMessage,
        // Description ne porte que le libellé « date + auteur ».
        interaction.setAlertMessage("Appel entrant test hotline");
        interaction.setDescription("08/09/2026 11:38 Testeur");
        interaction.setIsIn(true);
        actionMessageRepository.save(interaction);

        // Search with a totally different format (no "+", no country code, no spaces).
        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0612345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connu").value(true))
                .andExpect(jsonPath("$.fiches.length()").value(1))
                .andExpect(jsonPath("$.fiches[0].tier.id").value(tier.getId().toString()))
                .andExpect(jsonPath("$.fiches[0].dossiers.length()").value(1))
                .andExpect(jsonPath("$.fiches[0].dossiers[0].code").value("DOS-HOTLINE-TEST"))
                .andExpect(jsonPath("$.fiches[0].interactions.length()").value(1))
                .andExpect(jsonPath("$.fiches[0].interactions[0].note").value("Appel entrant test hotline"))
                .andExpect(jsonPath("$.fiches[0].interactions[0].description").value("08/09/2026 11:38 Testeur"));
    }

    /** Même règle que la fiche prospection : l'auteur vient du compte AdmUser, pas de CfgEmployeeId. */
    @Test
    void recherche_auteurDeLInteraction_estLEmployeDuCompte_pasLeCommercialConcerne() throws Exception {
        CfgTier tier = newTier("HOTLINE-AUTEUR", "+212 6 77 31 42 59");
        CfgTier auteur = newTier("EMP-HOTLINE-AUTEUR", null);
        auteur.setFirstName("Othman");
        auteur.setLastName("DIOURI");
        CfgTier commercial = newTier("EMP-HOTLINE-COMMERCIAL", null);
        commercial.setFirstName("Sabah");
        commercial.setLastName("HANINI");

        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode("tst.hotline." + UUID.randomUUID());
        compte.setDescription("o.diouri");
        compte.setDisabled(false);
        compte.setCfgEmployee(auteur);
        admUserRepository.save(compte);

        ComActionMessage interaction = new ComActionMessage();
        interaction.setId(UUID.randomUUID());
        interaction.setCfgTier(tier);
        interaction.setDocDate(LocalDateTime.now());
        interaction.setAdmUser(compte);
        interaction.setCfgEmployee(commercial);
        interaction.setUserName("Othman DIOURI");
        actionMessageRepository.save(interaction);

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0677314259"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions.length()").value(1))
                .andExpect(jsonPath("$.fiches[0].interactions[0].employeNom").value("Othman DIOURI"));
    }

    private CfgTier newTier(String code, String mobile) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName(code);
        tier.setMobile(mobile);
        return tierRepository.save(tier);
    }

    @Test
    void recherche_withNoMatchingTier_butMatchingPreProspection_returnsUnknownWithAppelsSansFiche() throws Exception {
        ComProspection preProspection = new ComProspection();
        preProspection.setId(UUID.randomUUID());
        preProspection.setCode("PREP-HOTLINE-TEST");
        preProspection.setDocDate(LocalDateTime.now());
        preProspection.setTempDescription("Appelant jamais rappelé - test hotline");
        preProspection.setTempTel("661998877");
        prospectionRepository.save(preProspection);

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "+212 661998877"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connu").value(false))
                .andExpect(jsonPath("$.fiches.length()").value(0))
                .andExpect(jsonPath("$.appelsSansFiche.length()").value(1))
                .andExpect(jsonPath("$.appelsSansFiche[0].code").value("PREP-HOTLINE-TEST"));
    }

    @Test
    void recherche_withBlankTelephone_returns400() throws Exception {
        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "   "))
                .andExpect(status().isBadRequest());
    }
}
