package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P8c, 2e tour : Direction Générale garde la lecture audit/notes d'étude ; écriture et suppression (Admin) intactes. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudePorteurRoleLectureRevue2Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite faisabilite;

    private UUID etude;
    private RequestPostProcessor commercial;
    private RequestPostProcessor direction;
    private RequestPostProcessor administrateur;

    @BeforeEach
    void setUp() throws Exception {
        faisabilite.parametrage();
        AdmUser admin = patrimoine.administrateur();
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        etude = new ApiFaisabilite(mockMvc, admin).etude(fiche("PRL-" + jeton, societe.getId()));
        commercial = connecte(patrimoine.administrateur(), "rev2.com." + jeton, "Commercial");
        direction = connecte(patrimoine.administrateur(), "rev2.dg." + jeton, "Direction Générale");
        administrateur = connecte(patrimoine.administrateur(), "rev2.adm." + jeton, "Administrateur");
    }

    private static RequestPostProcessor connecte(AdmUser compte, String login, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), login, null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private String corpsNote() {
        return "{\"porteur\":\"etude\",\"id\":\"" + etude + "\",\"texte\":\"note revue\"}";
    }

    @Test
    void direction_lit_audit_et_notes_etude() throws Exception {
        mockMvc.perform(get("/api/audit").param("objetId", etude.toString()).with(direction))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notes").param("porteur", "etude").param("id", etude.toString()).with(direction))
                .andExpect(status().isOk());
    }

    @Test
    void commercial_ne_cree_pas_de_note_etude() throws Exception {
        mockMvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(corpsNote()).with(commercial))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrateur_cree_et_supprime_une_note_etude_commercial_non() throws Exception {
        String reponse = mockMvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote()).with(administrateur))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        String note = JsonPath.read(reponse, "$.id");
        mockMvc.perform(delete("/api/notes/" + note).with(commercial)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/notes/" + note).with(administrateur)).andExpect(status().is2xxSuccessful());
    }
}
