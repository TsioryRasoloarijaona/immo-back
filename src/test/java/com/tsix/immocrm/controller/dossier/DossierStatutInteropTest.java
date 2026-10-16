package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code PATCH /api/dossiers/{id}/statut} : écritures et refus d'OMRAN (FEATURE-vente-socle.md A, B5 ;
 * FEATURE-interop-omran-ecritures.md §4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierStatutInteropTest {

    private static final String AUTEUR = "Agent Statut";
    private static final String ANNULATION = "{\"statut\":\"ANNULATION\",\"note\":\"changement d'avis\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureStatutsDossier fixtureStatuts;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private StrFolderStatusHistoryRepository statusHistoryRepository;

    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        fixtureStatuts.statut(StatutDossier.ANNULATION);
        fixtureStatuts.statut(StatutDossier.CONCRETISATION);
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-STATUT-INTEROP");
        vocationRepository.save(vocation);
        dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-STATUT-INTEROP");
        dossier.setStkVocation(vocation);
        dossier.setStrStatus(fixtureStatuts.reservation());
        dossier = folderRepository.save(dossier);
        entityManager.flush();
    }

    private MockHttpServletRequestBuilder changer(String corps) {
        return patch("/api/dossiers/{id}/statut", dossier.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    private UUID statutSyndic(boolean annule) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO StrStatus (PKey, Code, BoolField001, BoolField002) "
                        + "VALUES (:id, :code, 0, :annule)")
                .setParameter("id", id)
                .setParameter("code", "TST-SYN-" + id)
                .setParameter("annule", annule)
                .executeUpdate();
        return id;
    }

    private void dossierDeSyndic(boolean annule) {
        entityManager.createNativeQuery("INSERT INTO SynFolder (PKey, Code, ComFolderId, StrStatusId) "
                        + "VALUES (:id, 'TST-SYN', :dossier, :statut)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("dossier", dossier.getId())
                .setParameter("statut", statutSyndic(annule))
                .executeUpdate();
    }

    private void factureLigne(String type, String montantHT) {
        UUID document = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO StlDocument (PKey, Code, DocType) VALUES (:id, 'TST-FAC', :type)")
                .setParameter("id", document)
                .setParameter("type", type)
                .executeUpdate();
        entityManager.createNativeQuery("INSERT INTO StlDocumentLine (Pkey, StlDocumentId, ComFolderId, TotalHT) "
                        + "VALUES (:id, :document, :dossier, :montant)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("document", document)
                .setParameter("dossier", dossier.getId())
                .setParameter("montant", new BigDecimal(montantHT))
                .executeUpdate();
    }

    private String statutEnBase() {
        entityManager.flush();
        entityManager.clear();
        return folderRepository.findById(dossier.getId()).orElseThrow().getStrStatus().getCode();
    }

    @Test
    void dossierSansStatut_409_repriseNecessaire() throws Exception {
        dossier.setStrStatus(null);
        entityManager.flush();

        mockMvc.perform(changer("{\"statut\":\"CONCRETISATION\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("VENTE-reprise-interop.sql")));
    }

    @Test
    void changement_signeUserUpdateEtHistoriseAvecLAuteur() throws Exception {
        AdmUser agent = new AdmUser();
        agent.setId(UUID.randomUUID());
        agent.setCode("agent.statut." + UUID.randomUUID());
        agent.setDescription(AUTEUR);
        agent.setDisabled(false);
        AdmUser enregistre = userRepository.save(agent);

        mockMvc.perform(changer("{\"statut\":\"CONCRETISATION\",\"note\":\"acompte reçu\"}").with(connecte(enregistre)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        ComFolder apres = folderRepository.findById(dossier.getId()).orElseThrow();
        List<StrFolderStatusHistory> lignes = statusHistoryRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId());
        assertAll(
                () -> assertThat(apres.getUserUpdate()).isEqualTo(AUTEUR),
                () -> assertThat(apres.getUserUpdateDateTime()).isNotNull(),
                () -> assertThat(lignes).singleElement().satisfies(l -> assertAll(
                        () -> assertThat(l.getDescription()).isEqualTo("acompte reçu"),
                        () -> assertThat(l.getAdmUserId()).isEqualTo(enregistre.getId()),
                        () -> assertThat(l.getStrFolderStatus().getCode()).isEqualTo("CONCRETISATION"))));
    }

    /** B5 : Sale.cs:BeforeUpdate refuse la sortie tant qu'un dossier de syndic attaché est en cours. */
    @Test
    void annulation_syndicEnCours_409_statutInchange() throws Exception {
        dossierDeSyndic(false);

        mockMvc.perform(changer(ANNULATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("dossier de syndic")));

        assertThat(statutEnBase()).isEqualTo("RESERVATION");
    }

    @Test
    void annulation_syndicAnnule_acceptee() throws Exception {
        dossierDeSyndic(true);

        mockMvc.perform(changer(ANNULATION)).andExpect(status().isOk());

        assertThat(statutEnBase()).isEqualTo("ANNULATION");
    }

    /** B5 : Sale.cs:Verif refuse la sortie d'un dossier facturé sans avoir. */
    @Test
    void annulation_factureSansAvoir_409() throws Exception {
        factureLigne("INVOICE", "500000");

        mockMvc.perform(changer(ANNULATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("avoir")));

        assertThat(statutEnBase()).isEqualTo("RESERVATION");
    }

    @Test
    void annulation_factureCompenseeParUnAvoir_acceptee() throws Exception {
        factureLigne("INVOICE", "500000");
        factureLigne("REFUND", "500000");

        mockMvc.perform(changer(ANNULATION)).andExpect(status().isOk());

        assertThat(statutEnBase()).isEqualTo("ANNULATION");
    }
}
