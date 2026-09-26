package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmAlertDataRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cycle de vie d'une alerte de relance, fidèle au logiciel de bureau — voir
 * {@code instructions/FEATURE-alerte-admalertdata.md} et {@code AlerteRelanceService}.
 *
 * <p>La règle, rejouée en direct dans OMRAN le 26/09/2026 sur la prospection {@code 26/05941} :
 * <b>c'est la prochaine interaction porteuse d'une date d'alerte qui éteint la précédente</b>.
 * Une note sans alerte ne touche à rien ; la date de l'alerte ne joue aucun rôle. Et l'alerte
 * n'existe pour l'écran du légataire que par sa ligne {@code AdmAlertData} active.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionInteractionAlerteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private AdmAlertDataRepository alerteRepository;

    @Autowired
    private EntityManager em;

    private ComProspection newProspection(String prefixe) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(prefixe + "-" + UUID.randomUUID());
        p.setDocDate(LocalDateTime.now());
        return prospectionRepository.save(p);
    }

    /** Une relance « à la OMRAN » : l'interaction datée d'une alerte ET sa ligne AdmAlertData active. */
    private ComActionMessage relanceExistante(ComProspection p, LocalDateTime dateAlerte, boolean impayee) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setDocDate(LocalDateTime.now().minusDays(3));
        m.setAlertValidityDate(dateAlerte);
        m.setAlertMessage("relance existante");
        m.setDescription("libelle");
        m.setWithoutRevival(false);
        m.setIsUnpaid(impayee);
        m = actionMessageRepository.saveAndFlush(m);

        AdmAlertData a = new AdmAlertData();
        a.setId(UUID.randomUUID());
        a.setCode(a.getId().toString());
        a.setDescription(AdmAlertData.DESCRIPTION_ALERTE);
        a.setGroupping(AdmAlertData.GROUPE_PROSPECTION);
        a.setMessage("relance existante");
        a.setAlertDate(dateAlerte);
        a.setRelatedKey(m.getId());
        a.setRightKey(AdmAlertData.DROIT_COMMUNICATION);
        a.setActive(true);
        // saveAndFlush : le désarmement cible les alertes par une requête NATIVE, que Hibernate
        // ne précède d'aucun auto-flush. Même piège que ProspectionListeContactAlerteTest.
        alerteRepository.saveAndFlush(a);
        return m;
    }

    private UUID poster(ComProspection p, String corps) throws Exception {
        String reponse = mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush();
        return UUID.fromString(reponse.replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1"));
    }

    // --- Une interaction datée d'une alerte ---

    @Test
    void interactionAvecAlerte_poseLaLigneAdmAlertDataAuFormatDuLegataire() throws Exception {
        ComProspection p = newProspection("ALERTE-POSE");
        LocalDateTime demain = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);

        UUID id = poster(p, """
                {"description":"rappeler le prospect demain","dateAlerte":"%s"}""".formatted(demain));

        AdmAlertData alerte = alerteRepository.findByRelatedKey(id).orElseThrow();
        assertThat(alerte.getActive()).isTrue();
        assertThat(alerte.getAlertDate()).isEqualTo(demain);
        assertThat(alerte.getMessage()).isEqualTo("rappeler le prospect demain");
        assertThat(alerte.getDescription()).isEqualTo("Alerte");
        assertThat(alerte.getGroupping()).isEqualTo("Prospection");
        assertThat(alerte.getRightKey()).isEqualTo("69f8e7d6-1111-1111-1111-1aa8b1f02008");
        assertThat(alerte.getCode()).isEqualTo(alerte.getId().toString().toLowerCase());
        assertThat(alerte.getLabel()).isNull();
        assertThat(actionMessageRepository.findById(id).orElseThrow().getWithoutRevival()).isFalse();
    }

    @Test
    void interactionAvecAlerte_eteintLaRelancePrecedente() throws Exception {
        ComProspection p = newProspection("ALERTE-ETEINT");
        ComActionMessage ancienne = relanceExistante(p, LocalDateTime.now().plusDays(10), false);

        UUID nouvelle = poster(p, """
                {"description":"nouvelle relance","dateAlerte":"%s"}"""
                .formatted(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MINUTES)));

        assertThat(actionMessageRepository.findById(ancienne.getId()).orElseThrow().getWithoutRevival()).isTrue();
        assertThat(alerteRepository.findByRelatedKey(ancienne.getId()).orElseThrow().getActive()).isFalse();
        assertThat(alerteRepository.findByRelatedKey(nouvelle).orElseThrow().getActive()).isTrue();
        assertThat(actionMessageRepository.findById(nouvelle).orElseThrow().getWithoutRevival()).isFalse();
    }

    /** Le prédicat du légataire porte {@code IsUnpaid = 0} : un rappel d'impayé n'est pas désarmé. */
    @Test
    void interactionAvecAlerte_epargneLesRappelsDImpaye() throws Exception {
        ComProspection p = newProspection("ALERTE-IMPAYE");
        ComActionMessage impaye = relanceExistante(p, LocalDateTime.now().plusDays(10), true);

        poster(p, """
                {"description":"relance commerciale","dateAlerte":"%s"}"""
                .formatted(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MINUTES)));

        assertThat(actionMessageRepository.findById(impaye.getId()).orElseThrow().getWithoutRevival()).isFalse();
    }

    // --- Une interaction sans alerte ---

    /** La note de 14:35 du 26/09 : elle a laissé l'alerte de 14:33 active. */
    @Test
    void interactionSansAlerte_neToucheARien() throws Exception {
        ComProspection p = newProspection("ALERTE-NOTE");
        ComActionMessage ancienne = relanceExistante(p, LocalDateTime.now().minusDays(1), false);

        UUID note = poster(p, """
                {"description":"simple note, l'alerte est pourtant echue"}""");

        assertThat(actionMessageRepository.findById(ancienne.getId()).orElseThrow().getWithoutRevival()).isFalse();
        assertThat(alerteRepository.findByRelatedKey(ancienne.getId()).orElseThrow().getActive()).isTrue();
        assertThat(alerteRepository.findByRelatedKey(note)).isEmpty();
    }

    // --- Ce que la liste en montre ---

    @Test
    void liste_remonteLaNouvelleAlerteEtPlusLAncienne() throws Exception {
        ComProspection p = newProspection("ALERTE-LISTE");
        relanceExistante(p, LocalDateTime.now().plusDays(30), false);

        poster(p, """
                {"description":"relance qui remplace","dateAlerte":"%s"}"""
                .formatted(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MINUTES)));

        mockMvc.perform(get("/api/prospections").param("search", p.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].derniereAlerteMessage").value("relance qui remplace"));
    }

    /** Une relance désarmée n'est plus une alerte : la liste ne la montre plus. */
    @Test
    void liste_ignoreUneRelanceDesarmee() throws Exception {
        ComProspection p = newProspection("ALERTE-DESARMEE");
        ComActionMessage m = relanceExistante(p, LocalDateTime.now().plusDays(5), false);
        m.setWithoutRevival(true);
        actionMessageRepository.saveAndFlush(m);

        mockMvc.perform(get("/api/prospections").param("search", p.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].derniereAlerteDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].derniereAlerteMessage").doesNotExist());
    }

    // --- Archivage ---

    @Test
    void archiver_eteintLesAlertesDeLaProspection() throws Exception {
        ComProspection p = newProspection("ALERTE-ARCHIVE");
        ComActionMessage m = relanceExistante(p, LocalDateTime.now().plusDays(5), false);

        mockMvc.perform(patch("/api/prospections/{id}/archiver", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"raisonTexte":"ne repond plus"}"""))
                .andExpect(status().isOk());
        em.flush();

        List<AdmAlertData> actives = alerteRepository.alertesActivesDeLaProspection(p.getId());
        assertThat(actives).isEmpty();
        assertThat(alerteRepository.findByRelatedKey(m.getId()).orElseThrow().getActive()).isFalse();
    }
}
