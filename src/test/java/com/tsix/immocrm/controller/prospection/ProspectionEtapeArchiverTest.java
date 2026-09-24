package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionEtapeArchiverTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;

    @Autowired
    private ComProspectionClosingReasonRepository closingReasonRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private EntityManager entityManager;

    private ComProspection newProspection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        return prospectionRepository.save(p);
    }

    @Test
    void changerEtape_happyPath() throws Exception {
        ComProspection prospection = newProspection("PSN-ETAPE-A");
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("ETAPE-A-TEST");
        kind.setDescription("ETAPE-A-TEST");
        prospectionKindRepository.save(kind);

        String body = "{\"etapeId\":\"" + kind.getId() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/etape", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etape").value("ETAPE-A-TEST"));
    }

    @Test
    void changerEtape_unknownProspection_returns404() throws Exception {
        String body = "{\"etapeId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/etape", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void changerEtape_unknownEtapeId_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-ETAPE-B");
        String body = "{\"etapeId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/etape", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archiver_happyPath_withReasonText() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-A");
        String body = "{\"raisonTexte\":\"Ne répond plus\"}";

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(true))
                .andExpect(jsonPath("$.raisonFermetureTexte").value("Ne répond plus"))
                .andExpect(jsonPath("$.dateFermeture").exists());
    }

    @Test
    void archiver_happyPath_withReasonId() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-B");
        ComProspectionClosingReason reason = new ComProspectionClosingReason();
        reason.setId(UUID.randomUUID());
        reason.setCode("RAISON-TEST");
        reason.setDescription("Raison de test");
        closingReasonRepository.save(reason);

        String body = "{\"raisonId\":\"" + reason.getId() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(true))
                .andExpect(jsonPath("$.raisonFermeture").value("Raison de test"));
    }

    @Test
    void archiver_unknownProspection_returns404() throws Exception {
        mockMvc.perform(patch("/api/prospections/{id}/archiver", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"raisonTexte\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiver_missingReason_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-C");

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Séquence OMRAN (trace d'audit du 24/09/2026) : l'archivage désarme les relances de la
     * prospection — même prédicat que la transformation en vente. Une interaction sans alerte
     * n'est pas touchée.
     */
    @Test
    void archiver_desarmeLesRelancesDeLaProspection() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-D");

        ComActionMessage relance = new ComActionMessage();
        relance.setId(UUID.randomUUID());
        relance.setComProspection(prospection);
        relance.setDocDate(LocalDateTime.now());
        relance.setAlertValidityDate(LocalDateTime.now().plusDays(7));
        relance.setWithoutRevival(false);
        actionMessageRepository.save(relance);

        ComActionMessage sansAlerte = new ComActionMessage();
        sansAlerte.setId(UUID.randomUUID());
        sansAlerte.setComProspection(prospection);
        sansAlerte.setDocDate(LocalDateTime.now());
        sansAlerte.setWithoutRevival(false);
        actionMessageRepository.save(sansAlerte);

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"raisonTexte\":\"Sans suite\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(true));

        entityManager.flush();
        entityManager.clear();

        assertThat(actionMessageRepository.findById(relance.getId()).orElseThrow().getWithoutRevival())
                .as("la relance en cours doit être désarmée à l'archivage")
                .isTrue();
        assertThat(actionMessageRepository.findById(sansAlerte.getId()).orElseThrow().getWithoutRevival())
                .as("une interaction sans alerte est hors du prédicat")
                .isFalse();
        // UserUpdate reste null sous @WithMockUser (principal sans AdmUser = auteur anonyme,
        // comportement voulu de UtilisateurAffiche) ; seule la date est vérifiable ici.
        ComProspection archivee = prospectionRepository.findById(prospection.getId()).orElseThrow();
        assertThat(archivee.getUserUpdateDateTime()).isNotNull();
    }

    /**
     * Fidèle au légataire : le désarchivage ne bascule que {@code IsClosed}. Date et raison de
     * clôture sont conservées ({@code AdmWebHistorics} du 24/09/2026 :
     * {@code [["IsClosed","True","False"]]}, rien d'autre).
     */
    @Test
    void desarchiver_happyPath_conserveDateEtRaisonDeCloture() throws Exception {
        ComProspection prospection = newProspection("PSN-DESARCH-A");
        ComProspectionClosingReason reason = new ComProspectionClosingReason();
        reason.setId(UUID.randomUUID());
        reason.setCode("RAISON-DESARCH");
        reason.setDescription("Attend appartement témoin");
        closingReasonRepository.save(reason);

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"raisonId\":\"" + reason.getId() + "\",\"raisonTexte\":\"texte libre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(true));

        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(false))
                .andExpect(jsonPath("$.dateFermeture").exists())
                .andExpect(jsonPath("$.raisonFermetureId").value(reason.getId().toString()))
                .andExpect(jsonPath("$.raisonFermeture").value("Attend appartement témoin"))
                .andExpect(jsonPath("$.raisonFermetureTexte").value("texte libre"));

        entityManager.flush();
        entityManager.clear();

        ComProspection rouverte = prospectionRepository.findById(prospection.getId()).orElseThrow();
        assertThat(rouverte.getIsClosed()).isFalse();
        assertThat(rouverte.getClosingDate()).as("OMRAN conserve la date de clôture").isNotNull();
        assertThat(rouverte.getComProspectionClosingReason()).as("OMRAN conserve la raison").isNotNull();
        assertThat(rouverte.getUserUpdateDateTime()).isNotNull();
    }

    /** Une prospection rouverte doit ressortir dans la liste des ouvertes. */
    @Test
    void desarchiver_reapparaitDansLesOuvertes() throws Exception {
        ComProspection prospection = newProspection("PSN-DESARCH-B");
        prospection.setIsClosed(true);
        prospection.setClosingDate(LocalDateTime.now());
        prospectionRepository.save(prospection);

        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", prospection.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/prospections").param("ouvertes", "true").param("search", "PSN-DESARCH-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code == 'PSN-DESARCH-B')].fermee").value(false));
    }

    @Test
    void desarchiver_unknownProspection_returns404() throws Exception {
        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void desarchiver_prospectionNonArchivee_returns409() throws Exception {
        ComProspection ouverte = newProspection("PSN-DESARCH-C");
        ouverte.setIsClosed(false);
        prospectionRepository.save(ouverte);

        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", ouverte.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // IsClosed NULL (saisie OMRAN ancienne) vaut « ouverte » : même refus.
        ComProspection nulle = newProspection("PSN-DESARCH-D");
        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", nulle.getId()))
                .andExpect(status().isConflict());
    }
}
