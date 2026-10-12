package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code POST /api/prospections/{id}/avancer} — FEATURE-parcours-commercial.md §3. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProspectionAvancerTest {

    private static final String AVANCER = "/api/prospections/{id}/avancer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParcoursOutils outils;

    @Autowired
    private FixtureInteraction fixtureInteraction;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository messageRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private Clock clock;

    private LocalDate aujourdhui;
    private AdmUser moi;
    private CfgTranche tranche;
    private ComProspectionKind qualifie;
    private ComProspectionKind projetObligatoire;
    private ComActionMessageType telephone;
    private ComActionMessageSubCategory rappel;

    @BeforeEach
    void setUp() {
        aujourdhui = LocalDate.now(clock);
        moi = outils.compte("AV-MOI", "Commercial");
        tranche = outils.tranche("AV-T1");
        qualifie = outils.etape("TST 3 QUALIFIE", false);
        projetObligatoire = outils.etape("TST 2 EN COURS", true);
        telephone = fixtureInteraction.typeTelephone();
        rappel = outils.resultat("A rappeler par le commercial", true);
    }

    private String corps(UUID etapeId, UUID typeId, UUID resultatId, LocalDateTime date) {
        String action = typeId == null ? "null" : """
                {"typeId":"%s","sousCategorieId":%s,"date":%s,"message":"Rappeler mardi"}"""
                .formatted(typeId, resultatId == null ? "null" : "\"" + resultatId + "\"",
                        date == null ? "null" : "\"" + date + "\"");
        return """
                {"etapeId":"%s","prochaineAction":%s}""".formatted(etapeId, action);
    }

    private ResultActions avancer(ComProspection p, String corps) throws Exception {
        return mockMvc.perform(post(AVANCER, p.getId()).contentType(MediaType.APPLICATION_JSON).content(corps)
                .with(ParcoursOutils.connecte(moi, "Commercial")));
    }

    @Test
    void avancer_changeLEtapeEtPoseLaProchaineAction() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        p.setNotes("notes du commercial");
        prospectionRepository.saveAndFlush(p);
        ComActionMessage precedente = outils.relance(p, aujourdhui.atTime(9, 0), null);
        LocalDateTime demain = aujourdhui.plusDays(1).atTime(11, 0);

        avancer(p, corps(projetObligatoire.getId(), telephone.getId(), rappel.getId(), demain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapeId").value(projetObligatoire.getId().toString()))
                .andExpect(jsonPath("$.notes").value("notes du commercial"));

        em.flush();
        em.clear();
        List<ComActionMessage> messages = messageRepository.findByComProspectionIdOrderByDocDateDesc(p.getId());
        assertThat(messages).hasSize(2)
                .anySatisfy(m -> assertThat(m.getAlertValidityDate()).isEqualTo(demain))
                .filteredOn(m -> m.getId().equals(precedente.getId()))
                .singleElement().extracting(ComActionMessage::getWithoutRevival).isEqualTo(true);
    }

    @Test
    void avancer_resultatVisite_creeLeRendezVous() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        ComActionMessageSubCategory visite = outils.resultat("Rendez vous pris", true);
        LocalDateTime mardi = aujourdhui.plusDays(3).atTime(16, 0);

        avancer(p, corps(qualifie.getId(), telephone.getId(), visite.getId(), mardi))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
        assertThat(messageRepository.findByComProspectionIdOrderByDocDateDesc(p.getId()))
                .singleElement()
                .satisfies(m -> assertThat(m.getAdmEvent()).isNotNull()
                        .extracting(AdmEvent::getStartHour).isEqualTo(mardi));
    }

    // ---------------------------------------------------------------- refus

    @Test
    void avancer_sansProchaineAction_400() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());

        avancer(p, corps(qualifie.getId(), null, null, null)).andExpect(status().isBadRequest());
    }

    @Test
    void avancer_dateDansLePasse_400() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());

        avancer(p, corps(qualifie.getId(), telephone.getId(), rappel.getId(), aujourdhui.minusDays(1).atTime(10, 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void avancer_sansDate_400() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());

        avancer(p, corps(qualifie.getId(), telephone.getId(), rappel.getId(), null)).andExpect(status().isBadRequest());
    }

    @Test
    void avancer_etapeExigeantUnProjetSurUneLigneSansProjet_409() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        p.setStkHierarchy(null);
        prospectionRepository.saveAndFlush(p);

        avancer(p, corps(projetObligatoire.getId(), telephone.getId(), rappel.getId(), aujourdhui.plusDays(1).atTime(10, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void avancer_prospectionArchivee_409() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        p.setIsClosed(true);
        prospectionRepository.saveAndFlush(p);

        avancer(p, corps(qualifie.getId(), telephone.getId(), rappel.getId(), aujourdhui.plusDays(1).atTime(10, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void avancer_preProspection_409() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));

        avancer(lead, corps(qualifie.getId(), telephone.getId(), rappel.getId(), aujourdhui.plusDays(1).atTime(10, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void avancer_ligneDUnAutreCommercial_404() throws Exception {
        ComProspection sienne = outils.prospection(outils.compte("AV-AUTRE", "Commercial"), tranche,
                aujourdhui.atStartOfDay());

        avancer(sienne, corps(qualifie.getId(), telephone.getId(), rappel.getId(), aujourdhui.plusDays(1).atTime(10, 0)))
                .andExpect(status().isNotFound());
    }

    @Test
    void avancer_idInconnu_404() throws Exception {
        mockMvc.perform(post(AVANCER, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(qualifie.getId(), telephone.getId(), rappel.getId(), aujourdhui.plusDays(1).atTime(10, 0)))
                        .with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isNotFound());
    }
}
