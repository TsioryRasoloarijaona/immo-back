package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import jakarta.persistence.Tuple;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.junit.jupiter.api.Assertions.assertAll;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statut et transition du projet — FEATURE-projets-config-parite.md §5 : seules les transitions partant du statut courant
 * (workflow {@code StrWorkFlow} de l'entité « Projet ») sont proposées et exécutables ; une autre répond 409, statut inchangé ;
 * écriture réservée à l'Administrateur (403). Le projet est posé sur des statuts de test (A → B → C) pour isoler le jeu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetConfigTransitionsTest {

    private static final String TRANSITIONS_AUTORISEES = "$.transitionsAutorisees";
    private static final String COMPTE_STATUT ="SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND StrStatusId = :statut";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;

    private AdmUser admin;
    private AdmUser commercial;
    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID statutA;
    private UUID statutB;
    private UUID transitionAB;
    private UUID transitionBC;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
        statutA = fixture.statut(ENTITE_PROJET, "SA-" + jeton);
        statutB = fixture.statut(ENTITE_PROJET, "SB-" + jeton);
        UUID statutC = fixture.statut(ENTITE_PROJET, "SC-" + jeton);
        transitionAB = fixture.transitionProjet(statutA, statutB, "A=>B " + jeton);
        transitionBC = fixture.transitionProjet(statutB, statutC, "B=>C " + jeton);
        projet = fixture.projet("ST-" + jeton, societe);
        fixture.statutDuProjet(projet, statutA);
    }

    private static RequestPostProcessor enCommercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, "Commercial");
    }

    private ResultActions executer(UUID id, UUID transition, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(TRANSITION, id).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(TRANSITION_ID, transition).json()));
    }

    private int surStatut(UUID statut) {
        return fixture.compter(COMPTE_STATUT, Map.of("id", projet, "statut", statut));
    }

    // ------------------------------------------------------------------ transitions proposées

    @Test
    void s5_transitionsProposees_seulementCellesDuStatutCourant() throws Exception {
        mockMvc.perform(get(TRANSITIONS, projet).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(transitionAB.toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(transitionBC.toString()))));
    }

    @Test
    void s5_transitionsProposees_suiventLeNouveauStatut() throws Exception {
        fixture.statutDuProjet(projet, statutB);

        mockMvc.perform(get(TRANSITIONS, projet).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(transitionBC.toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(transitionAB.toString()))));
    }

    // ------------------------------------------------------------------ exécution

    @Test
    void s5_transitionAutorisee_200_statutPoseEnBaseEtDansLaFiche() throws Exception {
        executer(projet, transitionAB, connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutId").value(statutB.toString()));

        assertThat(surStatut(statutB)).isEqualTo(1);
    }

    @Test
    void s5_transitionNePartantPasDuStatutCourant_409_statutInchange() throws Exception {
        executer(projet, transitionBC, connecte(admin)).andExpect(status().isConflict());

        assertThat(surStatut(statutA)).isEqualTo(1);
    }

    @Test
    void s5_transitionDejaFaite_rejouee_409() throws Exception {
        executer(projet, transitionAB, connecte(admin)).andExpect(status().isOk());

        executer(projet, transitionAB, connecte(admin)).andExpect(status().isConflict());

        assertThat(surStatut(statutB)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ droits et 404

    @Test
    void s5_transition_nonAdministrateur_403_statutInchange() throws Exception {
        executer(projet, transitionAB, enCommercial(commercial)).andExpect(status().isForbidden());

        assertThat(surStatut(statutA)).isEqualTo(1);
    }

    @Test
    void s5_transitionsProposees_nonAdministrateur_403() throws Exception {
        mockMvc.perform(get(TRANSITIONS, projet).with(enCommercial(commercial))).andExpect(status().isForbidden());
    }

    @Test
    void s5_projetInconnu_404() throws Exception {
        UUID inconnu = UUID.randomUUID();

        mockMvc.perform(get(TRANSITIONS, inconnu).with(connecte(admin))).andExpect(status().isNotFound());
        executer(inconnu, transitionAB, connecte(admin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ contrat validé : 409 détaillé, historique

    @Test
    void s5_transitionInconnue_409_listeLesTransitionsPermises_statutInchange() throws Exception {
        executer(projet, UUID.randomUUID(), connecte(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, hasItem("SB-" + jeton)))
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, not(hasItem("SC-" + jeton))));

        assertThat(surStatut(statutA)).isEqualTo(1);
    }

    @Test
    void s5_transitionNePartantPasDuStatutCourant_409_listeLesTransitionsPermises() throws Exception {
        executer(projet, transitionBC, connecte(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, hasItem("SB-" + jeton)));
    }

    @Test
    void s5_transitionAutorisee_uneLigneStrHistoryAuFormatOmran() throws Exception {
        executer(projet, transitionAB, connecte(admin)).andExpect(status().isOk());

        List<Tuple> lignes = fixture.lignes("SELECT * FROM StrHistory WHERE ObjectId = :id AND StrWorkflowId IS NOT NULL", projet);
        assertThat(lignes).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guid(h.get("StrWorkflowId"))).isEqualTo(guid(transitionAB)),
                () -> assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(statutB)),
                () -> assertThat(guid(h.get("StrConfigEntityId"))).isEqualTo(guid(ENTITE_PROJET)),
                () -> assertThat((String) h.get("Description"))
                        .startsWith("A=>B " + jeton + ": SA-" + jeton + "=>SB-" + jeton + "(")
                        .endsWith(" & )"),
                () -> assertThat(h.get("DocDate")).isNotNull()));
    }

    @Test
    void s5_transitionRefusee_aucuneLigneStrHistory() throws Exception {
        executer(projet, transitionBC, connecte(admin)).andExpect(status().isConflict());

        assertThat(fixture.lignes("SELECT * FROM StrHistory WHERE ObjectId = :id AND StrWorkflowId IS NOT NULL", projet))
                .isEmpty();
    }

    @Test
    void s5_creationProjet_ligneStrHistoryCreationAuStatutNouveau() throws Exception {
        String json = mockMvc.perform(post(PROJETS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, "SH-" + jeton).avec(DESCRIPTION, "Projet SH").avec(SOCIETE_ID, societe).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID cree = UUID.fromString(JsonPath.read(json, "$.id"));

        List<Tuple> lignes = fixture.lignes("SELECT * FROM StrHistory WHERE ObjectId = :id", cree);
        assertThat(lignes).singleElement().satisfies(h -> assertAll(
                () -> assertThat(h.get("Description")).isEqualTo("Création"),
                () -> assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(STATUT_NOUVEAU)),
                () -> assertThat(guid(h.get("StrConfigEntityId"))).isEqualTo(guid(ENTITE_PROJET)),
                () -> assertThat(h.get("StrWorkflowId")).isNull()));
    }
}
