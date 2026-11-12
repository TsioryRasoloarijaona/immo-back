package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DOCUMENTS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.creation;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.detail;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.document;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.id;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.suppression;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DELETE /api/achat/config/statuts/{id} — FEATURE-statuts-achat-parite.md règle 9 : 204 si libre (documents à joindre
 * supprimés avec lui) ; 409 si cité par une pièce, une transition (départ ou arrivée) ou un historique ; 404 si inconnu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatSuppressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID libre;
    private UUID autre;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2D");
        statuts.tables();
        admin = statuts.administrateur(jeton);
        libre = config.statut(Statut.simple("K2D-" + jeton, 1));
        autre = config.statut(Statut.simple("K2D2-" + jeton, 2));
    }

    private void refuse409(UUID statut) throws Exception {
        mockMvc.perform(suppression(statut).with(admin)).andExpect(status().isConflict());
        assertThat(statuts.statutsParId(statut)).isEqualTo(1);
    }

    /** R9 : statut créé par l'API avec un document à joindre, libre → 204, absent de la liste, documents supprimés. */
    @Test
    void r9_statutLibre_204_documentsSupprimesAvecLui() throws Exception {
        UUID type = config.typeEtat("Nouveau " + jeton);
        UUID typeDocument = statuts.typeDocument("Devis " + jeton);
        Map<String, Object> c = corps("K2DX-" + jeton, type);
        c.put(DOCUMENTS, List.of(document(typeDocument, true, true)));
        UUID id = id(mockMvc.perform(creation(json, c).with(admin)).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(suppression(id).with(admin)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(statuts.statutsParId(id)).isZero(),
                () -> assertThat(statuts.documentsDuStatut(id)).isZero(),
                () -> assertThat(ligne(lire(mockMvc, STATUTS, admin), id)).isNull());
    }

    /** R9 : statut posé en base, sans aucune référence → 204. */
    @Test
    void r9_statutSansReference_204() throws Exception {
        mockMvc.perform(suppression(libre).with(admin)).andExpect(status().isNoContent());

        assertThat(statuts.statutsParId(libre)).isZero();
    }

    /** R9 : cité par une pièce ({@code BuyDocument.BuyDocumentStatusId}) → 409. */
    @Test
    void r9_citeParUnePiece_409() throws Exception {
        config.piece(libre, null);

        refuse409(libre);
    }

    /** R9 : cité en départ d'une transition → 409. */
    @Test
    void r9_citeEnDepartDeTransition_409() throws Exception {
        config.transition(null, 1, "Départ " + jeton, libre, autre);

        refuse409(libre);
    }

    /** R9 : cité en arrivée d'une transition → 409. */
    @Test
    void r9_citeEnArriveeDeTransition_409() throws Exception {
        config.transition(null, 1, "Arrivée " + jeton, autre, libre);

        refuse409(libre);
    }

    /** R9 : cité par l'historique seul (la pièce porte un autre statut) → 409. */
    @Test
    void r9_citeParUnHistorique_409() throws Exception {
        UUID piece = config.piece(autre, null);
        config.passage(piece, null, libre, LocalDateTime.of(2022, 6, 1, 9, 0));

        refuse409(libre);
    }

    /** R9 : le message, en français, donne les trois comptes (2 pièces, 1 transition, 3 historiques). */
    @Test
    void r9_messageDonneLesTroisComptes() throws Exception {
        config.piece(libre, null);
        UUID piece = config.piece(libre, null);
        config.transition(null, 1, "Départ " + jeton, libre, autre);
        for (int jour = 1; jour <= 3; jour++) {
            config.passage(piece, null, libre, LocalDateTime.of(2022, 7, jour, 9, 0));
        }

        MvcResult r = mockMvc.perform(suppression(libre).with(admin)).andExpect(status().isConflict()).andReturn();

        // Nombre isolé : le code du statut (chiffres du jeton) ne doit pas faire passer le test.
        assertThat(detail(r)).containsPattern("(?<![\\w-])2(?![\\w-])").containsPattern("(?<![\\w-])1(?![\\w-])")
                .containsPattern("(?<![\\w-])3(?![\\w-])");
    }

    /** 404 sur id inconnu ; témoin : la même route supprime un statut existant (204). */
    @Test
    void suppression_idInconnu_404_temoin204() throws Exception {
        mockMvc.perform(suppression(libre).with(admin)).andExpect(status().isNoContent());
        mockMvc.perform(suppression(UUID.randomUUID()).with(admin)).andExpect(status().isNotFound());
    }
}
