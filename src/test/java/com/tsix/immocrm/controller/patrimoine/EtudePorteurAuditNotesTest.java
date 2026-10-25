package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureLienProspection;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine.Porteur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureLienProspection.PORTEUR_ETUDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Porteur « etude » des notes et de l'audit — FEATURE-patrimoine-etude-prospection.md R9, R10, R11 : une étude visible se lit
 * et reçoit des notes ; une étude d'entité inactive répond 404 à tous, Administrateur compris, comme un id inconnu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudePorteurAuditNotesTest {

    private static final String NOTES = "/api/notes";
    private static final String AUDIT = "/api/audit";
    private static final String MESSAGES_DE = "SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id";
    private static final UUID INCONNU = UUID.fromString("6C7D8E9F-0A1B-4C2D-9E3F-4A5B6C7D8E9F");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private FixtureProspectionsTerrain prospections;
    @Autowired private FixtureLienProspection lien;

    private AdmUser admin;
    private CfgCompany societe;
    private UUID etude;

    @BeforeEach
    void setUp() throws Exception {
        faisabilite.parametrage();
        admin = patrimoine.administrateur();
        String jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
        etude = new ApiFaisabilite(mockMvc, admin).etude(fiche("PNA-" + jeton, societe.getId()));
    }

    private ResultActions lireNotes(UUID id) throws Exception {
        return mockMvc.perform(get(NOTES).param("porteur", PORTEUR_ETUDE).param("id", id.toString()).with(connecte(admin)));
    }

    private ResultActions creerNote(UUID id) throws Exception {
        return mockMvc.perform(post(NOTES).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("porteur", PORTEUR_ETUDE).avec("id", id).avec("texte", "Note P8c").json()));
    }

    private ResultActions lireAudit(UUID id) throws Exception {
        return mockMvc.perform(get(AUDIT).param("objetId", id.toString()).with(connecte(admin)));
    }

    // ------------------------------------------------------------------ R9 : audit

    @Test
    void r9_audit_etudeVisible_200() throws Exception {
        lireAudit(etude).andExpect(status().isOk());
    }

    @Test
    void r9_audit_etudeDEntiteInactive_404_administrateurCompris() throws Exception {
        lien.desactiverSociete(societe.getId());

        lireAudit(etude).andExpect(status().isNotFound());
    }

    @Test
    void r9_audit_idInconnu_404() throws Exception {
        lireAudit(INCONNU).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ R9 : notes

    @Test
    void r9_notes_lecture_etudeVisible_200_noteDeLEtude() throws Exception {
        prospections.note(etude);

        lireNotes(etude)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void r9_notes_lecture_etudeDEntiteInactive_404_administrateurCompris() throws Exception {
        prospections.note(etude);
        lien.desactiverSociete(societe.getId());

        lireNotes(etude).andExpect(status().isNotFound());
    }

    @Test
    void r9_notes_lecture_idInconnu_404() throws Exception {
        lireNotes(INCONNU).andExpect(status().isNotFound());
    }

    @Test
    void r9_notes_creation_etudeVisible_201_rattacheeALEtude() throws Exception {
        creerNote(etude).andExpect(status().isCreated());

        assertThat(faisabilite.compter(MESSAGES_DE, etude)).isEqualTo(1);
    }

    @Test
    void r9_notes_creation_etudeDEntiteInactive_404_rienEcrit() throws Exception {
        lien.desactiverSociete(societe.getId());

        creerNote(etude).andExpect(status().isNotFound());

        assertThat(faisabilite.compter(MESSAGES_DE, etude)).isZero();
    }

    // ------------------------------------------------------------------ R10 : valeur du fil

    @Test
    void r10_porteurDepuisEtude_ETUDE() {
        assertThat(Porteur.depuis(PORTEUR_ETUDE).name()).isEqualTo("ETUDE");
    }

    @Test
    void r10_porteurInvalide_400_messageCiteLesCinqValeurs() throws Exception {
        mockMvc.perform(get(NOTES).param("porteur", "inconnu").param("id", etude.toString()).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(allOf(containsString("projet"), containsString("tranche"),
                        containsString("article"), containsString("prospection-terrain"), containsString(PORTEUR_ETUDE))));
    }

    // ------------------------------------------------------------------ R11 : la prospection terrain reste servie

    @Test
    void r11_audit_idDeProspectionTerrain_200_commeAvant() throws Exception {
        Contexte c = prospections.contexte("PNP");
        UUID prospection = prospections.prospection(c, "PNP-" + c.jeton());

        lireAudit(prospection).andExpect(status().isOk());
    }
}
