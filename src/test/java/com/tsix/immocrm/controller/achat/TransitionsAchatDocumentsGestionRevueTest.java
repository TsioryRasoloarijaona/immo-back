package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Revue (2e tour) : la règle du décompte (seulement pour la gestion PrjInvoiceDebitWorkingOut) doit tenir aussi quand un
 * PUT change la gestion sans renvoyer documentsAJoindre (« absent = inchangés »).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsAchatDocumentsGestionRevueTest {

    private static final String DOCUMENTS = "documentsAJoindre";

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
    private UUID ordinaire;
    private UUID decomptes;
    private UUID typeDocument;
    private UUID typeMarche;

    @BeforeEach
    void setUp() throws SQLException {
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
        jeton = FixtureConfigurationAchat.jeton("F1G");
        admin = statuts.administrateur(jeton);
        depart = config.statut(Statut.simple("GD-" + jeton, 1));
        arrivee = config.statut(Statut.simple("GA-" + jeton, 2));
        ordinaire = config.gestion("GO-" + jeton, "Gestion ordinaire " + jeton);
        decomptes = config.gestion("PrjInvoiceDebitWorkingOut", "Décomptes " + jeton);
        transition = config.transition(decomptes, 1, "Gestion " + jeton, depart, arrivee);
        typeDocument = fichiers.type("TST-DOC-" + jeton);
        typeMarche = projet.typeLot("TST-TM-" + jeton);
    }

    private Map<String, Object> corps(UUID gestionId) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ordre", 1);
        c.put("description", "Gestion " + jeton);
        c.put("statutDepartId", depart.toString());
        c.put("statutArriveeId", arrivee.toString());
        c.put("roleIds", List.of());
        c.put("gestionId", gestionId.toString());
        return c;
    }

    private int envoyer(Map<String, Object> c) throws Exception {
        return mockMvc.perform(put(TRANSITIONS + "/{id}", transition).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(c)).with(admin)).andReturn().getResponse().getStatus();
    }

    private long lignesAvecDecompte() {
        return ((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM BuyDocumentWorkFlowDocumentToAttach WHERE BuyDocumentWorkFlowId = :id
                AND (ForFirstWorkingOut = 1 OR ForIntermediateWorkingOut = 1 OR ForFinalWorkingOut = 1)""")
                .setParameter("id", transition).getSingleResult()).longValue();
    }

    @Test
    void changerDeGestion_sansRenvoyerLesDocuments_neLaissePasDeDecompteHorsGestionDesDecomptes() throws Exception {
        Map<String, Object> avec = corps(decomptes);
        avec.put(DOCUMENTS, List.of(Map.of("typeDocumentId", typeDocument.toString(),
                "typeMarcheId", typeMarche.toString(), "decompte", "FINAL")));
        assertThat(envoyer(avec)).isEqualTo(200);
        assertThat(lignesAvecDecompte()).isEqualTo(1);

        int code = envoyer(corps(ordinaire));

        // Soit le PUT est refusé (400), soit les documents gardés respectent la règle de la nouvelle gestion.
        assertThat(code == 400 || lignesAvecDecompte() == 0)
                .as("PUT -> %d ; documents avec décompte restés sous une gestion ordinaire : %d", code,
                        lignesAvecDecompte())
                .isTrue();
    }
}
