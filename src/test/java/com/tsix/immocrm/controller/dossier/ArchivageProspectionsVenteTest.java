package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.dossier.DossierService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Archivage des autres prospections ouvertes de l'acquéreur à la création du dossier et au changement de client
 * ({@code Sale.cs:ManageProspectionToArchive}, FEATURE-dossier-finitions.md §2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ArchivageProspectionsVenteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureStatutsDossier fixtureStatuts;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private StrHistoryRepository historyRepository;

    @Test
    void creation_archiveLAutreProspectionDuClient_pasCelleDOrigine_niCelleDUnAutreClient() throws Exception {
        fixtureStatuts.reservation();
        CfgTier client = fixture.tier("CLI-ARCH");
        ComProspection origine = prospection("PSN-ARCH-ORIG", client);
        ComProspection autre = prospection("PSN-ARCH-AUTRE", client);
        ComProspection dUnAutreClient = prospection("PSN-ARCH-TIERS", fixture.tier("CLI-ARCH-2"));

        String json = creerDossier(origine, "LOT-ARCH-1");

        ComProspection autreRelue = relue(autre);
        assertAll(
                () -> assertThat(JsonPath.<List<String>>read(json, "$.prospectionsArchivees"))
                        .containsExactly("PSN-ARCH-AUTRE"),
                () -> assertThat(autreRelue.getIsClosed()).isTrue(),
                () -> assertThat(autreRelue.getClosingReasonText()).isEqualTo(DossierService.TEXTE_CLOTURE_VENTE),
                () -> assertThat(autreRelue.getClosingDate()).isNotNull(),
                () -> assertThat(historyRepository.findAll()).anyMatch(h -> autre.getId().equals(h.getObjectId())),
                () -> assertThat(relue(origine).getIsClosed()).isTrue(),
                () -> assertThat(relue(dUnAutreClient).getIsClosed()).isFalse());
    }

    @Test
    void creation_sansAutreProspection_listeVide() throws Exception {
        fixtureStatuts.reservation();
        ComProspection origine = prospection("PSN-ARCH-SEULE", fixture.tier("CLI-ARCH-SEUL"));

        String json = creerDossier(origine, "LOT-ARCH-2");

        assertThat(JsonPath.<List<String>>read(json, "$.prospectionsArchivees")).isEmpty();
    }

    @Test
    void changementDeClient_archiveLesProspectionsOuvertesDuNouveau_pasCellesDeLAncien() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-ARCH", fixture.tranche("TR-ARCH", true), StatutDossier.RESERVATION);
        ComProspection deLAncien = prospection("PSN-ARCH-ANCIEN", dossier.getCfgCustomer());
        CfgTier nouveau = fixture.tier("CLI-ARCH-NOUVEAU");
        ComProspection duNouveau = prospection("PSN-ARCH-NOUVEAU", nouveau);

        String json = mockMvc.perform(patch("/api/dossiers/{id}", dossier.getId())
                        .with(user("admin").roles("Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"%s\"}".formatted(nouveau.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(JsonPath.<List<String>>read(json, "$.prospectionsArchivees"))
                        .containsExactly("PSN-ARCH-NOUVEAU"),
                () -> assertThat(relue(duNouveau).getIsClosed()).isTrue(),
                () -> assertThat(relue(deLAncien).getIsClosed()).isFalse());
    }

    @Test
    void modificationSansChangementDeClient_rienNEstArchive() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-ARCH-NOTE", fixture.tranche("TR-ARCH-NOTE", true),
                StatutDossier.RESERVATION);
        ComProspection ouverte = prospection("PSN-ARCH-NOTE", dossier.getCfgCustomer());

        String json = mockMvc.perform(patch("/api/dossiers/{id}", dossier.getId())
                        .with(user("admin").roles("Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"%s\"}".formatted(dossier.getCfgCustomer().getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(JsonPath.<List<String>>read(json, "$.prospectionsArchivees")).isEmpty(),
                () -> assertThat(relue(ouverte).getIsClosed()).isFalse());
    }

    private String creerDossier(ComProspection origine, String codeLot) throws Exception {
        StkItem lot = fixture.dossier("DOS-" + codeLot, fixture.tranche("TR-" + codeLot, true), null).getStkItem();
        lot.setComFolderId(null);
        stkItemRepository.save(lot);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-" + codeLot);
        paymentMethodRepository.save(modePaie);
        String corps = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"ARCH-CIN"}
                """.formatted(origine.getId(), lot.getId(), modePaie.getId());
        return mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ComProspection prospection(String code, CfgTier tiers) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(tiers);
        p.setIsClosed(false);
        return prospectionRepository.save(p);
    }

    private ComProspection relue(ComProspection p) {
        entityManager.flush();
        entityManager.clear();
        return prospectionRepository.findById(p.getId()).orElseThrow();
    }
}
