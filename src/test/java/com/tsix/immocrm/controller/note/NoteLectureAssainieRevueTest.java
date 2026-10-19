package com.tsix.immocrm.controller.note;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.FixtureAudit;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue 1 §10.2 (XSS stockée) adaptée par §11.8 — FEATURE-patrimoine-audit-actions.md : OMRAN écrit du HTML dans
 * {@code AdmMessage.Notes} ; la lecture rend {@code texte} sans aucune balise, entités décodées, {@code <br>} et
 * {@code </p>} en saut de ligne. Ne pas affaiblir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteLectureAssainieRevueTest {

    private static final LocalDateTime JANVIER = LocalDateTime.of(2026, 1, 3, 11, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureAudit audit;

    private AdmUser admin;
    private CfgProject projet;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        projet = fixture.projet("PRJ-NOTE-REVUE", fixture.entite("ENT-NOTE-REVUE", true));
    }

    /** Écrit une note OMRAN brute sur le programme et renvoie le seul {@code texte} relu. */
    private String texteRelu(String notesHtml) throws Exception {
        audit.note(projet.getId(), JANVIER, notesHtml, null);
        String json = mockMvc.perform(get("/api/notes").param("porteur", "projet").param("id", projet.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContainIgnoringCase("<script").doesNotContainIgnoringCase("<img");
        List<String> textes = JsonPath.read(json, "$..texte");
        assertThat(textes).hasSize(1);
        return textes.get(0);
    }

    @Test
    void r10_2_script_retireDuTexte() throws Exception {
        String texte = texteRelu("<script>alert(1)</script>Bonjour");

        assertThat(texte).contains("Bonjour").doesNotContain("<", ">");
    }

    @Test
    void r10_2_attributEvenementiel_retireAvecSaBalise() throws Exception {
        String texte = texteRelu("<img src=x onerror=alert(2)>Plan joint");

        assertThat(texte).contains("Plan joint").doesNotContainIgnoringCase("onerror").doesNotContain("<", ">");
    }

    @Test
    void r11_8_balisesDeMiseEnFormeRetirees_paragraphesEnSautsDeLigne() throws Exception {
        String texte = texteRelu("<p><b>Ligne</b> 1</p><p><span style=\"color:red\">Ligne 2</span></p>");

        assertThat(texte).contains("Ligne 1\nLigne 2").doesNotContain("<", ">", "style");
    }

    @Test
    void r11_8_brEnSautDeLigne() throws Exception {
        String texte = texteRelu("premiere<br>seconde");

        assertThat(texte).isEqualTo("premiere\nseconde");
    }

    @Test
    void r11_8_entitesDecodees() throws Exception {
        String texte = texteRelu("Prix &lt; 100 &amp; &quot;frais&quot; &gt; 0");

        assertThat(texte).isEqualTo("Prix < 100 & \"frais\" > 0");
    }

    @Test
    void r11_8_texteSansHtml_renduTelQuel() throws Exception {
        String texte = texteRelu("Client rappelle lundi");

        assertThat(texte).isEqualTo("Client rappelle lundi");
    }

    @Test
    void r11_8_reponseSansChampTexteHtml() throws Exception {
        audit.note(projet.getId(), JANVIER, "<p>Note</p>", null);

        String json = mockMvc.perform(get("/api/notes").param("porteur", "projet").param("id", projet.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(json).doesNotContain("texteHtml").doesNotContain("<p>");
    }

    @Test
    void r10_2_balisesImbriquees_retirees() throws Exception {
        String texte = texteRelu("<div>Visite <i>prévue</i></div>");

        assertThat(texte).contains("Visite prévue").doesNotContain("<", ">");
    }
}
