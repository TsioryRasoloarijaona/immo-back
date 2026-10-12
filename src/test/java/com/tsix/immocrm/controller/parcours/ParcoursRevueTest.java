package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.mail.VisiteEffectuee;
import com.tsix.immocrm.mail.VisitePlanifiee;
import com.tsix.immocrm.repositories.adm.AdmAlertDataRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Relecture du parcours commercial : chemins chef / périmètre projet, AdmAlertData, e-mails de visite. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class ParcoursRevueTest {

    private static final String CHEF = "Commercial Chef";
    private static final String COMMERCIAL = "Commercial";

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
    private AdmAlertDataRepository alerteRepository;

    @Autowired
    private ApplicationEvents evenements;

    @Autowired
    private EntityManager em;

    @Autowired
    private Clock clock;

    private LocalDate aujourdhui;
    private AdmUser moi;
    private AdmUser chef;
    private CfgTranche t1;
    private CfgTranche t2;
    private ComProspectionKind qualifie;
    private ComActionMessageType telephone;
    private ComActionMessageSubCategory rappel;

    @BeforeEach
    void setUp() {
        aujourdhui = LocalDate.now(clock);
        t1 = outils.tranche("RV-T1");
        t2 = outils.tranche("RV-T2");
        moi = outils.compte("RV-MOI", COMMERCIAL);
        chef = outils.compte("RV-CHEF", CHEF);
        outils.affecter(chef, t1);
        outils.affecter(moi, t1);
        qualifie = outils.etape("TST 3 QUALIFIE RV", false);
        telephone = fixtureInteraction.typeTelephone();
        rappel = outils.resultat("A rappeler par le commercial", true);
        outils.resultat("Prospect injoignable ou Boite vocale", true);
    }

    private String corps(UUID etapeId, UUID resultatId) {
        return """
                {"etapeId":"%s","prochaineAction":{"typeId":"%s","sousCategorieId":"%s","date":"%s","message":"m"}}"""
                .formatted(etapeId, telephone.getId(), resultatId, aujourdhui.plusDays(1).atTime(10, 0));
    }

    private ResultActions avancer(ComProspection p, String corps, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post("/api/prospections/{id}/avancer", p.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corps).with(qui));
    }

    // ---------------------------------------------------------------- chef

    @Test
    void chef_avanceLaLigneDUnMembreDansSesProjets_200() throws Exception {
        ComProspection p = outils.prospection(moi, t1, aujourdhui.atStartOfDay());

        avancer(p, corps(qualifie.getId(), rappel.getId()), ParcoursOutils.connecte(chef, CHEF))
                .andExpect(status().isOk());
    }

    @Test
    void chef_avanceLaLigneDUnMembreHorsDeSesProjets_404() throws Exception {
        outils.affecter(moi, t2);
        ComProspection p = outils.prospection(moi, t2, aujourdhui.atStartOfDay());

        avancer(p, corps(qualifie.getId(), rappel.getId()), ParcoursOutils.connecte(chef, CHEF))
                .andExpect(status().isNotFound());
    }

    @Test
    void chef_journeeDUnMembre_masqueSesLeadsHorsDesProjetsDuChef() throws Exception {
        outils.affecter(moi, t2);
        ComProspection visible = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        visible.setStkHierarchy(t1);
        ComProspection cache = outils.preProspection(moi, aujourdhui.atTime(10, 0));
        cache.setStkHierarchy(t2);
        prospectionRepository.saveAndFlush(visible);
        prospectionRepository.saveAndFlush(cache);

        mockMvc.perform(get("/api/journee").param("commercialId", moi.getCfgEmployee().getId().toString())
                        .with(ParcoursOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(1))
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].prospectionId").value(visible.getId().toString()));
    }

    // ---------------------------------------------------------------- « Pas joignable »

    @Test
    void injoignable_surSaLigneHorsDeSonPerimetreProjet_404() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        lead.setStkHierarchy(t2);
        prospectionRepository.saveAndFlush(lead);

        mockMvc.perform(post("/api/boite-de-tri/{id}/injoignable", lead.getId())
                        .with(ParcoursOutils.connecte(moi, COMMERCIAL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void injoignable_surUnePreProspectionArchivee_409() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        lead.setIsClosed(true);
        prospectionRepository.saveAndFlush(lead);

        mockMvc.perform(post("/api/boite-de-tri/{id}/injoignable", lead.getId())
                        .with(ParcoursOutils.connecte(moi, COMMERCIAL)))
                .andExpect(status().isConflict());
    }

    @Test
    void injoignable_poseUneSeuleAlerteActiveSurLaNouvelleInteraction_sansVisite() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        outils.relance(lead, aujourdhui.atTime(9, 0), null);

        mockMvc.perform(post("/api/boite-de-tri/{id}/injoignable", lead.getId())
                        .with(ParcoursOutils.connecte(moi, COMMERCIAL)))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
        ComActionMessage nouvelle = messageRepository.findByComProspectionIdOrderByDocDateDesc(lead.getId()).stream()
                .filter(m -> "Pas joignable".equals(m.getAlertMessage()))
                .findFirst().orElseThrow();
        assertThat(nouvelle.getAlertValidityDate()).isEqualTo(aujourdhui.plusDays(1).atTime(10, 0));
        assertThat(alerteRepository.alertesActivesDeLaProspection(lead.getId()))
                .singleElement().extracting(AdmAlertData::getRelatedKey).isEqualTo(nouvelle.getId());
        assertThat(evenements.stream(VisitePlanifiee.class)).isEmpty();
    }

    // ---------------------------------------------------------------- e-mails de visite

    @Test
    void avancer_prochaineActionAppel_nePlanifieAucuneVisite() throws Exception {
        ComProspection p = outils.prospection(moi, t1, aujourdhui.atStartOfDay());

        avancer(p, corps(qualifie.getId(), rappel.getId()), ParcoursOutils.connecte(moi, COMMERCIAL))
                .andExpect(status().isOk());

        assertThat(evenements.stream(VisitePlanifiee.class)).isEmpty();
    }

    @Test
    void avancer_prochaineActionVisite_publieUneSeuleVisitePlanifiee() throws Exception {
        ComProspection p = outils.prospection(moi, t1, aujourdhui.atStartOfDay());
        ComActionMessageSubCategory rdv = outils.resultat("Rendez vous pris", true);

        avancer(p, corps(qualifie.getId(), rdv.getId()), ParcoursOutils.connecte(moi, COMMERCIAL))
                .andExpect(status().isOk());

        assertThat(evenements.stream(VisitePlanifiee.class)).hasSize(1);
    }

    @Test
    void avancer_visiteRefuseeEn409_nePublieAucunEvenement() throws Exception {
        ComProspection p = outils.prospection(moi, t1, aujourdhui.atStartOfDay());
        p.setStkHierarchy(null);
        prospectionRepository.saveAndFlush(p);
        ComProspectionKind apresVisites = outils.etape("4 APRES VISITES", true);
        ComActionMessageSubCategory rdv = outils.resultat("Rendez vous pris", true);

        avancer(p, corps(apresVisites.getId(), rdv.getId()), ParcoursOutils.connecte(moi, COMMERCIAL))
                .andExpect(status().isConflict());

        assertThat(evenements.stream(VisitePlanifiee.class)).isEmpty();
        assertThat(evenements.stream(VisiteEffectuee.class)).isEmpty();
    }

    /** Effet du PATCH /etape repris tel quel : glisser en « 4 APRES VISITES » déclenche le remerciement. */
    @Test
    void avancer_versApresVisites_publieLeRemerciement() throws Exception {
        ComProspection p = outils.prospection(moi, t1, aujourdhui.atStartOfDay());
        ComProspectionKind apresVisites = outils.etape("4 APRES VISITES", false);

        avancer(p, corps(apresVisites.getId(), rappel.getId()), ParcoursOutils.connecte(moi, COMMERCIAL))
                .andExpect(status().isOk());

        assertThat(evenements.stream(VisiteEffectuee.class)).hasSize(1);
    }
}
