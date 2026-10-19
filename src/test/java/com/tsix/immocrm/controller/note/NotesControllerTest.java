package com.tsix.immocrm.controller.note;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAudit;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Notes ({@code AdmMessage}) — FEATURE-patrimoine-audit-actions.md §4 amendé par §11.8, §8 (NotesControllerTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotesControllerTest {

    private static final String NOTES = "/api/notes";
    private static final String NOTE = "/api/notes/{id}";
    private static final String PORTEUR = "porteur";
    private static final String ARTICLE = "article";
    private static final String TEXTE = "texte";
    private static final String MESSAGE_VIDE = "veuillez saisir une note";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureAudit audit;
    @Autowired private StkItemRepository stkItemRepository;

    private AdmUser admin;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItem article;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        projet = fixture.projet("PRJ-NOTE", fixture.entite("ENT-NOTE", true));
        tranche = fixture.tranche("TR-NOTE", projet);
        article = article("ART-NOTE");
    }

    private StkItem article(String code) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkHierarchy(tranche);
        item.setStkKind(fixture.natureBat());
        item.setStkVocation(fixture.vocationVente());
        item.setStkItemCategory(fixture.categorie("CAT-" + code, fixture.natureBat(), false));
        item.setStkItemType(fixture.type("TYP-" + code, fixture.natureBat(), false));
        return stkItemRepository.saveAndFlush(item);
    }

    private UUID idDuPorteur(String porteur) {
        return switch (porteur) {
            case "projet" -> projet.getId();
            case "tranche" -> tranche.getId();
            default -> article.getId();
        };
    }

    private ResultActions creer(AdmUser auteur, CorpsJson corps) throws Exception {
        return creer(connecte(auteur), corps.json());
    }

    private ResultActions creer(RequestPostProcessor auteur, String json) throws Exception {
        return mockMvc.perform(post(NOTES).with(auteur).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private CorpsJson corpsArticle(String texte) {
        return corps().avec(PORTEUR, ARTICLE).avec("id", article.getId()).avec(TEXTE, texte);
    }

    /** Une colonne datetime revient en LocalDateTime (Hibernate 7) ou en Timestamp selon le pilote. */
    private static LocalDateTime dateHeure(Object valeur) {
        return valeur instanceof java.sql.Timestamp t ? t.toLocalDateTime() : (LocalDateTime) valeur;
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    // ------------------------------------------------------------------ §4 POST nominal et colonnes

    @ParameterizedTest(name = "porteur {0}")
    @ValueSource(strings = {"projet", "tranche", ARTICLE})
    void r4_creation_201_colonnesCommeOmran(String porteur) throws Exception {
        UUID porteurId = idDuPorteur(porteur);

        creer(admin, corps().avec(PORTEUR, porteur).avec("id", porteurId).avec(TEXTE, "Appeler le notaire"))
                .andExpect(status().isCreated());

        assertThat(audit.notesDuPorteur(porteurId)).singleElement().satisfies(n -> assertAll(
                () -> assertThat(n.get("Notes")).isEqualTo("Appeler le notaire"),
                () -> assertThat(guid(n.get("CategoryId"))).isEqualTo(guid(porteurId)),
                () -> assertThat(n.get("DocDate")).isNotNull(),
                () -> assertThat((String) n.get("Description")).matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2} " + AUTEUR),
                () -> assertThat(n.get("UserName")).isEqualTo(AUTEUR),
                () -> assertThat(n.get("Origin")).isEqualTo(""),
                () -> assertThat(n.get("IsIn")).isNull(),
                () -> assertThat(n.get("ReminderDate")).isNull()));
    }

    @Test
    void r4_creation_rappelEcritDansReminderDate() throws Exception {
        creer(admin, corpsArticle("Relancer").avec("rappel", "2026-11-02T09:30:00")).andExpect(status().isCreated());

        assertThat(audit.notesDuPorteur(article.getId())).singleElement()
                .satisfies(n -> assertThat(dateHeure(n.get("ReminderDate"))).isEqualTo(LocalDateTime.of(2026, 11, 2, 9, 30)));
    }

    // ------------------------------------------------------------------ §11.8 écriture en texte échappé

    @Test
    void r11_8_ecriture_caracteresHtmlEchappesEtSautsDeLigneEnBr() throws Exception {
        creer(connecte(admin), "{\"porteur\":\"article\",\"id\":\"%s\",\"texte\":\"a < b & \\\"c\\\" 'd' > e\\nligne 2\"}"
                .formatted(article.getId())).andExpect(status().isCreated());

        String notes = (String) audit.notesDuPorteur(article.getId()).get(0).get("Notes");
        String sansBr = notes.replace("<br>", "");
        assertAll(
                () -> assertThat(notes).contains("&lt;", "&gt;", "&amp;", "&quot;", "<br>").startsWith("a &lt; b &amp; "),
                () -> assertThat(sansBr).doesNotContain("<", ">", "\"", "'", "\n", "& "));
    }

    @Test
    void r11_8_allerRetour_texteRelu_identiqueAuTexteSaisi() throws Exception {
        creer(connecte(admin), "{\"porteur\":\"article\",\"id\":\"%s\",\"texte\":\"a < b & \\\"c\\\" 'd' > e\\nligne 2\"}"
                .formatted(article.getId())).andExpect(status().isCreated());

        mockMvc.perform(get(NOTES).param(PORTEUR, ARTICLE).param("id", article.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..texte").value(contains("a < b & \"c\" 'd' > e\nligne 2")));
    }

    // ------------------------------------------------------------------ §4 400 « veuillez saisir une note ! »

    @ParameterizedTest(name = "texte \"{0}\"")
    @ValueSource(strings = {"", "   "})
    void r4_texteVide_400(String texte) throws Exception {
        creer(admin, corpsArticle(texte))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsStringIgnoringCase(MESSAGE_VIDE)));
        assertThat(audit.notesDuPorteur(article.getId())).isEmpty();
    }

    @Test
    void r4_texteAbsent_400() throws Exception {
        creer(admin, corpsArticle("x").sans(TEXTE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsStringIgnoringCase(MESSAGE_VIDE)));
    }

    // ------------------------------------------------------------------ §4 404, §0.3 403

    @Test
    void r4_porteurInconnu_404() throws Exception {
        creer(admin, corps().avec(PORTEUR, "projet").avec("id", UUID.randomUUID()).avec(TEXTE, "Note orpheline"))
                .andExpect(status().isNotFound());
    }

    @Test
    void r0_3_creationParUnNonAdministrateur_403() throws Exception {
        creer(commercial(admin), corpsArticle("Note commerciale").json()).andExpect(status().isForbidden());
        assertThat(audit.notesDuPorteur(article.getId())).isEmpty();
    }

    @Test
    void r0_3_suppressionParUnNonAdministrateur_403() throws Exception {
        UUID note = audit.note(article.getId(), LocalDateTime.of(2026, 1, 2, 10, 0), "Note", null);

        mockMvc.perform(delete(NOTE, note).with(commercial(admin))).andExpect(status().isForbidden());
        assertThat(audit.notesDuPorteur(article.getId())).hasSize(1);
    }

    @Test
    void r0_3_lectureParUnNonAdministrateur_200() throws Exception {
        UUID note = audit.note(article.getId(), LocalDateTime.of(2026, 1, 2, 10, 0), "Note", null);

        mockMvc.perform(get(NOTES).param(PORTEUR, ARTICLE).param("id", article.getId().toString())
                        .with(commercial(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..id").value(contains(note.toString())));
    }

    // ------------------------------------------------------------------ §4 GET

    @Test
    void r4_lecture_notesDuPorteurSeulement_plusRecentesDAbord() throws Exception {
        UUID ancienne = audit.note(article.getId(), LocalDateTime.of(2026, 1, 2, 10, 0), "Ancienne", null);
        UUID recente = audit.note(article.getId(), LocalDateTime.of(2026, 3, 2, 10, 0), "Recente", null);
        audit.note(article("ART-NOTE-AUTRE").getId(), LocalDateTime.of(2026, 2, 2, 10, 0), "Autre article", null);

        mockMvc.perform(get(NOTES).param(PORTEUR, ARTICLE).param("id", article.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..id").value(contains(recente.toString(), ancienne.toString())));
    }

    @Test
    void r11_8_lecture_champsDuNoteDto_sansTexteHtml() throws Exception {
        UUID note = audit.note(tranche.getId(), LocalDateTime.of(2026, 1, 2, 10, 0), "Texte simple",
                LocalDateTime.of(2026, 2, 1, 8, 30));

        mockMvc.perform(get(NOTES).param(PORTEUR, "tranche").param("id", tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..id").value(contains(note.toString())))
                .andExpect(jsonPath("$..date").value(contains("2026-01-02T10:00:00")))
                .andExpect(jsonPath("$..auteur").value(contains(FixtureAudit.UTILISATEUR_OMRAN)))
                .andExpect(jsonPath("$..description").value(contains("01/01/2026 10:00 " + FixtureAudit.UTILISATEUR_OMRAN)))
                .andExpect(jsonPath("$..texte").value(contains("Texte simple")))
                .andExpect(jsonPath("$..rappel").value(contains("2026-02-01T08:30:00")))
                .andExpect(jsonPath("$..texteHtml").doesNotExist());
    }

    // ------------------------------------------------------------------ §4 DELETE

    @Test
    void r4_suppression_204_etLigneSupprimee() throws Exception {
        UUID note = audit.note(article.getId(), LocalDateTime.of(2026, 1, 2, 10, 0), "A supprimer", null);

        mockMvc.perform(delete(NOTE, note).with(connecte(admin))).andExpect(status().isNoContent());

        List<Tuple> restantes = audit.notesDuPorteur(article.getId());
        assertThat(restantes).isEmpty();
    }

    @Test
    void r4_suppression_noteInconnue_404() throws Exception {
        mockMvc.perform(delete(NOTE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}
