package com.tsix.immocrm.controller.notification;

import com.tsix.immocrm.controller.equipe.EquipeOutils;
import com.tsix.immocrm.domain.TypeNotification;
import com.tsix.immocrm.dto.notification.EvenementNotificationDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import com.tsix.immocrm.services.notification.FluxNotifications;
import com.tsix.immocrm.services.notification.LeadsAttribues;
import com.tsix.immocrm.services.notification.LeadsAttribues.Lead;
import com.tsix.immocrm.services.notification.NotificationService;
import com.tsix.immocrm.services.notification.ResumeNotificationsService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cloche : créations, lectures du seul compte appelant, flux SSE, résumé (FEATURE-repartition-notifications.md §3.3). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationsTest {

    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;
    private static final String URL = "/api/notifications";

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private NotificationService notificationService;
    @Autowired private ResumeNotificationsService resumeService;
    @Autowired private FluxNotifications flux;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgTranche trancheA;
    private AdmUser commercialA;
    private AdmUser commercialB;
    private AdmUser chefA;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("NOT-A");
        trancheA = outils.tranche("NOT-TA", projetA);
        commercialA = outils.compte("not.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);
        commercialB = outils.compte("not.comb", COMMERCIAL);
        outils.affecter(commercialB, projetA);
        chefA = outils.compte("not.chefa", RolesEquipe.CHEF);
        outils.affecter(chefA, projetA);
        // Chef d'un autre projet : ne doit rien recevoir des leads de NOT-A.
        AdmUser chefAutre = outils.compte("not.chefz", RolesEquipe.CHEF);
        outils.affecter(chefAutre, outils.projet("NOT-Z"));
        em.flush();
    }

    // ---------------------------------------------------------------- créations

    @Test
    void attributions_regroupeesParCompte_etJamaisALActeur() {
        UUID a = employe(commercialA);
        UUID b = employe(commercialB);
        LeadsAttribues lot = new LeadsAttribues(commercialB.getId(),
                List.of(lead("Un", a), lead("Deux", a), lead("Trois", a), lead("Quatre", a), lead("Cinq", b)),
                List.of());

        List<EvenementNotificationDto> evenements = notificationService.notifierAttributions(lot);

        assertThat(evenements).singleElement().satisfies(e -> {
            assertThat(e.admUserId()).isEqualTo(commercialA.getId());
            assertThat(e.notification().type()).isEqualTo(TypeNotification.LEADS_ATTRIBUES);
            assertThat(e.notification().titre()).isEqualTo("4 nouveaux leads à trier");
            assertThat(e.notification().message()).isEqualTo("Lead Un, Lead Deux, Lead Trois et 1 autre.");
            assertThat(e.notification().lien()).isEqualTo("/tri");
            assertThat(e.nonLues()).isEqualTo(1);
        });
    }

    @Test
    void attributionUnique_nommeLeLeadEtSonProjet() {
        Lead lead = lead("Seul", employe(commercialA));

        List<EvenementNotificationDto> evenements = notificationService.notifierAttributions(
                new LeadsAttribues(null, List.of(lead), List.of()));

        assertThat(evenements).singleElement().satisfies(e -> {
            assertThat(e.notification().titre()).isEqualTo("Nouveau lead : Lead Seul · NOT-A");
            assertThat(e.notification().lien()).isEqualTo("/pre-prospections/" + lead.id());
        });
    }

    @Test
    void aDistribuer_auxChefsDuProjetSeulement() {
        Lead sansCommercial = new Lead(UUID.randomUUID(), "26/9", "Orphelin", projetA.getId(), "NOT-A", null, true);

        List<EvenementNotificationDto> evenements = notificationService.notifierAttributions(
                new LeadsAttribues(null, List.of(), List.of(sansCommercial, sansCommercial)));

        assertThat(evenements).singleElement().satisfies(e -> {
            assertThat(e.admUserId()).isEqualTo(chefA.getId());
            assertThat(e.notification().type()).isEqualTo(TypeNotification.LEADS_A_DISTRIBUER);
            assertThat(e.notification().titre()).isEqualTo("2 leads importés à distribuer");
            assertThat(e.notification().lien()).isEqualTo("/equipe");
        });
    }

    // ---------------------------------------------------------------- lectures du compte appelant

    @Test
    void lister_seulementLesSiennes_plusRecentesDAbord() throws Exception {
        notifier(commercialA, "Première");
        notifier(commercialA, "Seconde");
        notifier(commercialB, "Pour B");

        mockMvc.perform(get(URL).with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.nonLues").value(2))
                .andExpect(jsonPath("$.notifications.length()").value(2));
    }

    @Test
    void marquerLue_notificationDUnAutreCompte_404() throws Exception {
        UUID id = notifier(commercialA, "Privée");

        mockMvc.perform(post(URL + "/" + id + "/lue").with(EquipeOutils.connecte(commercialB, COMMERCIAL)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL).with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(jsonPath("$.nonLues").value(1));
    }

    @Test
    void marquerLue_puisToutLu() throws Exception {
        UUID id = notifier(commercialA, "Une");
        notifier(commercialA, "Deux");
        notifier(commercialB, "Pour B");

        mockMvc.perform(post(URL + "/" + id + "/lue").with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonLues").value(1));
        mockMvc.perform(post(URL + "/tout-lu").with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonLues").value(0));
        mockMvc.perform(get(URL).with(EquipeOutils.connecte(commercialB, COMMERCIAL)))
                .andExpect(jsonPath("$.nonLues").value(1));
    }

    @Test
    void marquerLue_idInconnu_404() throws Exception {
        mockMvc.perform(post(URL + "/" + UUID.randomUUID() + "/lue")
                        .with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void lister_tailleHorsBornes_400() throws Exception {
        mockMvc.perform(get(URL).param("size", "0").with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void lister_sansCompteApplicatif_403() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- flux SSE

    @Test
    void flux_recoitLesNotificationsDeSonCompteSeulement() throws Exception {
        MvcResult fluxA = mockMvc.perform(get(URL + "/flux").accept(MediaType.TEXT_EVENT_STREAM)
                        .with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(request().asyncStarted())
                .andReturn();

        notificationsVersFlux(commercialB, "Pour B");
        notificationsVersFlux(commercialA, "Pour A");

        String recu = fluxA.getResponse().getContentAsString();
        assertThat(recu).contains("event:notification").contains("Pour A").doesNotContain("Pour B");
        assertThat(fluxA.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    // ---------------------------------------------------------------- résumé

    @Test
    void resume_auxSeulsCommerciauxQuiOntQuelqueChose() {
        ComProspection lead = outils.preProspection("NOT-R1", trancheA, LocalDateTime.now().minusDays(1));
        lead.setCfgCommercial(commercialA.getCfgEmployee());
        em.flush();

        List<EvenementNotificationDto> evenements = resumeService.envoyerResumes();

        assertThat(evenements).filteredOn(e -> e.admUserId().equals(commercialA.getId()))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.notification().type()).isEqualTo(TypeNotification.RESUME_DEMI_JOURNEE);
                    assertThat(e.notification().message()).isEqualTo("1 lead à trier");
                    assertThat(e.notification().lien()).isEqualTo("/journee");
                });
        assertThat(evenements).noneMatch(e -> e.admUserId().equals(commercialB.getId()));
    }

    // ---------------------------------------------------------------- outils

    private void notificationsVersFlux(AdmUser compte, String titre) {
        flux.diffuser(notificationService.creer(List.of(nouvelle(compte, titre))));
    }

    private UUID notifier(AdmUser compte, String titre) {
        return notificationService.creer(List.of(nouvelle(compte, titre))).getFirst().notification().id();
    }

    private static NotificationService.NouvelleNotification nouvelle(AdmUser compte, String titre) {
        return new NotificationService.NouvelleNotification(compte.getId(), TypeNotification.LEADS_ATTRIBUES,
                titre, "Message", "/tri");
    }

    private Lead lead(String nom, UUID commercialId) {
        return new Lead(UUID.randomUUID(), "26/" + nom, "Lead " + nom, projetA.getId(), "NOT-A", commercialId, true);
    }

    private static UUID employe(AdmUser user) {
        return user.getCfgEmployee().getId();
    }
}
