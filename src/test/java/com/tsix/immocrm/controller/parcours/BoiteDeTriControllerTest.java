package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComActionMessage;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/boite-de-tri} et {@code POST /api/boite-de-tri/{id}/injoignable} — FEATURE-parcours-commercial.md §3. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoiteDeTriControllerTest {

    private static final String BOITE = "/api/boite-de-tri";
    private static final String INJOIGNABLE = "/api/boite-de-tri/{id}/injoignable";

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

    @BeforeEach
    void setUp() {
        aujourdhui = LocalDate.now(clock);
        moi = outils.compte("BT-MOI", "Commercial");
        fixtureInteraction.typeTelephone();
        outils.resultat("Prospect injoignable ou Boite vocale", true);
    }

    // ---------------------------------------------------------------- liste

    @Test
    void liste_mesPreProspectionsOuvertesRecentes_plusRecentesDAbord() throws Exception {
        ComProspection hier = outils.preProspection(moi, aujourdhui.minusDays(1).atTime(9, 0));
        ComProspection recente = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        outils.preProspection(moi, aujourdhui.minusMonths(7).atStartOfDay());
        outils.preProspection(outils.compte("BT-AUTRE", "Commercial"), aujourdhui.atTime(9, 0));
        ComProspection fermee = outils.preProspection(moi, aujourdhui.atTime(8, 0));
        fermee.setIsClosed(true);
        prospectionRepository.saveAndFlush(fermee);

        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].prospectionId").value(recente.getId().toString()))
                .andExpect(jsonPath("$.content[1].prospectionId").value(hier.getId().toString()));
    }

    @Test
    void liste_toutesLeveLaLimiteDesSixMois() throws Exception {
        ComProspection ancienne = outils.preProspection(moi, aujourdhui.minusMonths(7).atStartOfDay());

        mockMvc.perform(get(BOITE).param("toutes", "true").with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].prospectionId").value(ancienne.getId().toString()));
    }

    @Test
    void liste_ecarteLesProspectionsQualifiees() throws Exception {
        outils.prospection(moi, outils.tranche("BT-T1"), aujourdhui.atStartOfDay());

        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** Numéro saisi autrement (indicatif, espaces) : même 9 derniers chiffres. */
    @Test
    void liste_signaleUneAutreProspectionAuMemeNumero() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        ComProspection autre = outils.preProspection(null, aujourdhui.minusDays(40).atStartOfDay());
        autre.setTempTel("+212 661-248-735");
        prospectionRepository.saveAndFlush(autre);

        String doublon = "$.content[?(@.prospectionId=='" + lead.getId() + "')].doublons[?(@.id=='" + autre.getId() + "')]";
        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(doublon + ".nature").value(hasItem("PROSPECTION")))
                .andExpect(jsonPath(doublon + ".criteres[0]").value(hasItem("TELEPHONE")));
    }

    @Test
    @WithMockUser
    void liste_sansCompteApplicatif_403() throws Exception {
        mockMvc.perform(get(BOITE)).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- « Pas joignable »

    @Test
    void injoignable_poseUneRelanceDemainDixHeuresEtSoldeLaPrecedente() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        ComActionMessage precedente = outils.relance(lead, aujourdhui.atTime(9, 0), null);

        mockMvc.perform(post(INJOIGNABLE, lead.getId()).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Pas joignable"));

        em.flush();
        em.clear();
        assertThat(messageRepository.findById(precedente.getId()))
                .get().extracting(ComActionMessage::getWithoutRevival).isEqualTo(true);
        assertThat(messageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null && lead.getId().equals(m.getComProspection().getId()))
                .map(ComActionMessage::getAlertValidityDate))
                .contains(aujourdhui.plusDays(1).atTime(10, 0));
        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** Une ligne déjà traitée revient par sa relance dans « Ma journée », pas dans la boîte. */
    @Test
    void liste_ecarteLesLignesDejaTraitees() throws Exception {
        ComProspection traitee = outils.preProspection(moi, aujourdhui.atTime(8, 0));
        outils.relance(traitee, aujourdhui.plusDays(2).atTime(9, 0), null);
        ComProspection intacte = outils.preProspection(moi, aujourdhui.atTime(9, 0));

        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].prospectionId").value(intacte.getId().toString()));
    }

    /** FEATURE-tri-a-jour.md §Règle 1 : l'étape changée suffit, aucune interaction n'est écrite. */
    @Test
    void liste_etapeChangee_sortDeLaBoiteSansInteraction() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        ComProspectionKind qualifie = outils.etape("3 QUALIFIE", false);

        mockMvc.perform(patch("/api/prospections/{id}/etape", lead.getId())
                        .with(ParcoursOutils.connecte(moi, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etapeId\":\"%s\"}".formatted(qualifie.getId())))
                .andExpect(status().isOk());
        em.flush();

        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(messageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null && lead.getId().equals(m.getComProspection().getId())))
                .isEmpty();
    }

    @Test
    void liste_etapeDeDepartOuNulle_resteATrier() throws Exception {
        ComProspection aTraiter = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        aTraiter.setComProspectionKind(outils.etape("1 A TRAITER", false));
        prospectionRepository.saveAndFlush(aTraiter);
        ComProspection sansEtape = outils.preProspection(moi, aujourdhui.atTime(8, 0));

        mockMvc.perform(get(BOITE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].prospectionId").value(aTraiter.getId().toString()))
                .andExpect(jsonPath("$.content[1].prospectionId").value(sansEtape.getId().toString()));
    }

    @Test
    void injoignable_surUneProspectionQualifiee_409() throws Exception {
        ComProspection qualifiee = outils.prospection(moi, outils.tranche("BT-T2"), aujourdhui.atStartOfDay());

        mockMvc.perform(post(INJOIGNABLE, qualifiee.getId()).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isConflict());
    }

    @Test
    void injoignable_surLaLigneDUnAutreCommercial_404() throws Exception {
        ComProspection sienne = outils.preProspection(outils.compte("BT-AUTRE", "Commercial"), aujourdhui.atTime(9, 0));

        mockMvc.perform(post(INJOIGNABLE, sienne.getId()).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isNotFound());
    }

    @Test
    void injoignable_idInconnu_404() throws Exception {
        mockMvc.perform(post(INJOIGNABLE, UUID.randomUUID()).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isNotFound());
    }
}
