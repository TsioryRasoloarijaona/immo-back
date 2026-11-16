package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-restes-projet-achat.md §5 (réponse 3) : documents à joindre d'une transition d'achat. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsAchatDocumentsTest {

    private static final String DOCUMENTS = "documentsAJoindre";
    private static final String DECOMPTES = "PrjInvoiceDebitWorkingOut";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;
    @Autowired private FixtureFichiersAttaches fichiers;
    @Autowired private FixtureProjetConfig projet;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID depart;
    private UUID arrivee;
    private UUID transition;
    private UUID gestion;
    private UUID typeDocument;
    private UUID typeMarche;

    @BeforeEach
    void setUp() throws SQLException {
        colonnesYesRemsTest();
        jeton = FixtureConfigurationAchat.jeton("F1D");
        admin = statuts.administrateur(jeton);
        depart = config.statut(Statut.simple("DD-" + jeton, 1));
        arrivee = config.statut(Statut.simple("DA-" + jeton, 2));
        gestion = config.gestion("GD-" + jeton, "Gestion documents " + jeton);
        transition = config.transition(gestion, 1, "Documents " + jeton, depart, arrivee);
        typeDocument = fichiers.type("TST-DOC-" + jeton);
        typeMarche = projet.typeLot("TST-TM-" + jeton);
    }

    // yesRemsTest n'a que PKey et PrjMarketTypeId (CI : ci/schema/190) ; DDL hors transaction, idempotent.
    private void colonnesYesRemsTest() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(true);
            s.execute("""
                    IF COL_LENGTH('dbo.BuyDocumentWorkFlowDocumentToAttach', 'BuyDocumentWorkFlowId') IS NULL
                    ALTER TABLE dbo.BuyDocumentWorkFlowDocumentToAttach ADD BuyDocumentWorkFlowId uniqueidentifier NULL,
                        IsMandatory bit NULL, WithTitle bit NULL, UserCreate nvarchar(255) NULL,
                        UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                        AdmAttachedFileTypeId uniqueidentifier NULL, ForFirstWorkingOut bit NULL,
                        ForIntermediateWorkingOut bit NULL, ForFinalWorkingOut bit NULL""");
        }
    }

    private Map<String, Object> corps(UUID gestionId) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ordre", 1);
        c.put("description", "Documents " + jeton);
        c.put("statutDepartId", depart.toString());
        c.put("statutArriveeId", arrivee.toString());
        c.put("roleIds", List.of());
        c.put("gestionId", gestionId.toString());
        return c;
    }

    private Map<String, Object> document(String decompte) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("typeDocumentId", typeDocument.toString());
        d.put("typeMarcheId", typeMarche.toString());
        d.put("decompte", decompte);
        return d;
    }

    private ResultActions envoyer(Map<String, Object> c) throws Exception {
        return mockMvc.perform(put(TRANSITIONS + "/{id}", transition).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(c)).with(admin));
    }

    private long lignes() {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM BuyDocumentWorkFlowDocumentToAttach WHERE BuyDocumentWorkFlowId = :id")
                .setParameter("id", transition).getSingleResult()).longValue();
    }

    @Test
    void documents_ecritsLusPuisGardesQuandAbsents_videsEffaces() throws Exception {
        Map<String, Object> plein = corps(gestion);
        plein.put(DOCUMENTS, List.of(document(null)));
        envoyer(plein).andExpect(status().isOk());

        envoyer(corps(gestion)).andExpect(status().isOk());

        Map<String, Object> l = ligne(lire(mockMvc, TRANSITIONS, admin), transition);
        assertThat(l).isNotNull();
        assertThat(l.get(DOCUMENTS)).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1)
                .first().asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("typeDocumentId", typeDocument.toString())
                .containsEntry("typeMarcheId", typeMarche.toString())
                .containsEntry("typeMarcheLibelle", "TST-TM-" + jeton);

        Map<String, Object> vide = corps(gestion);
        vide.put(DOCUMENTS, List.of());
        envoyer(vide).andExpect(status().isOk());
        assertThat(lignes()).isZero();
    }

    @Test
    void decompte_obligatoirePourLaGestionDesDecomptes_interditAilleurs() throws Exception {
        Map<String, Object> horsDecomptes = corps(gestion);
        horsDecomptes.put(DOCUMENTS, List.of(document("FINAL")));
        envoyer(horsDecomptes).andExpect(status().isBadRequest());

        UUID decomptes = config.gestion(DECOMPTES, "Décomptes " + jeton);
        Map<String, Object> sans = corps(decomptes);
        sans.put(DOCUMENTS, List.of(document(null)));
        envoyer(sans).andExpect(status().isBadRequest());

        Map<String, Object> avec = corps(decomptes);
        avec.put(DOCUMENTS, List.of(document("PREMIER"), document("FINAL")));
        envoyer(avec).andExpect(status().isOk());
        assertThat(lignes()).isEqualTo(2);
        assertThat(((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM BuyDocumentWorkFlowDocumentToAttach WHERE BuyDocumentWorkFlowId = :id
                AND ForFinalWorkingOut = 1 AND ForFirstWorkingOut = 0 AND IsMandatory = 1""")
                .setParameter("id", transition).getSingleResult()).longValue()).isEqualTo(1);
    }

    @Test
    void documents_inconnusOuEnDouble_400_sansEcriture() throws Exception {
        Map<String, Object> inconnu = document(null);
        inconnu.put("typeMarcheId", UUID.randomUUID().toString());
        Map<String, Object> c1 = corps(gestion);
        c1.put(DOCUMENTS, List.of(inconnu));
        envoyer(c1).andExpect(status().isBadRequest());

        Map<String, Object> c2 = corps(gestion);
        c2.put(DOCUMENTS, List.of(document(null), document(null)));
        envoyer(c2).andExpect(status().isBadRequest());
        assertThat(lignes()).isZero();
    }

    @Test
    void suppression_emporteLesDocuments() throws Exception {
        Map<String, Object> plein = corps(gestion);
        plein.put(DOCUMENTS, List.of(document(null)));
        envoyer(plein).andExpect(status().isOk());

        mockMvc.perform(delete(TRANSITIONS + "/{id}", transition).with(admin)).andExpect(status().isNoContent());
        assertThat(lignes()).isZero();
    }
}
