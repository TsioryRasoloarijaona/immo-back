package com.tsix.immocrm.controller.note;

import com.tsix.immocrm.entities.adm.AdmMessage;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-patrimoine-audit-actions.md §4 : {@code NoteDto.texteHtml} est documenté « assaini côté serveur » et le
 * front l'injecte tel quel ; une note écrite par OMRAN (ou toute autre voie que POST /api/notes) doit l'être aussi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteLectureAssainieRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AdmMessageRepository repository;

    @Test
    void noteEcriteParOmran_relueAssainie() throws Exception {
        AdmUser lecteur = fixture.utilisateur("LECT-NOTE-REVUE");
        CfgTranche tranche = fixture.tranche("TR-NOTE-REVUE",
                fixture.projet("PRJ-NOTE-REVUE", fixture.entite("ENT-NOTE-REVUE", true)));
        AdmMessage note = new AdmMessage();
        note.setId(UUID.randomUUID());
        note.setDocDate(LocalDateTime.of(2026, 10, 1, 9, 0));
        note.setCategoryId(tranche.getId());
        note.setOrigin("");
        note.setNotes("<P>Bornage</P><img src=x onerror=\"alert(document.domain)\">");
        repository.save(note);

        String corps = mockMvc.perform(get("/api/notes").param("porteur", "tranche")
                        .param("id", tranche.getId().toString()).with(connecte(lecteur)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps).contains("Bornage").doesNotContain("onerror").doesNotContain("<img");
    }
}
