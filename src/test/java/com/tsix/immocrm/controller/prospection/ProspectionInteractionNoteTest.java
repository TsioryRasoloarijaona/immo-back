package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La note d'une interaction — voir {@code instructions/FIX-interaction-note.md}.
 *
 * <p>Le point vérifié ici tient en une phrase : <b>le texte du commercial vit dans
 * {@code AlertMessage}, pas dans {@code Description}</b>. Ce dernier est un libellé
 * « date + auteur » que l'application de bureau régénère à chaque écriture — mesuré sur les
 * 46 546 interactions de {@code yesRemsProd}, sans une seule exception. Notre API lisait et
 * écrivait la mauvaise colonne des deux côtés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionInteractionNoteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    private ComProspection newProspection() {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-NOTE-" + UUID.randomUUID());
        return prospectionRepository.save(p);
    }

    /** Reproduit une ligne telle que l'application de bureau l'écrit. */
    private ComActionMessage newInteractionLegataire(ComProspection p, String note) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setDocDate(LocalDateTime.now());
        m.setDescription("07/09/2026 17:13 Khadija ELKORRI");
        m.setAlertMessage(note);
        m.setIsIn(true);
        return actionMessageRepository.save(m);
    }

    // --- Lecture ---

    @Test
    void lecture_renvoieLaNoteSaisieParLeCommercial() throws Exception {
        ComProspection p = newProspection();
        newInteractionLegataire(p, "la cliente cherche 2 chambres et salon");

        mockMvc.perform(get("/api/prospections/{id}/interactions", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].note").value("la cliente cherche 2 chambres et salon"))
                // Le libellé reste exposé, mais il ne porte pas le contenu.
                .andExpect(jsonPath("$.content[0].description").value("07/09/2026 17:13 Khadija ELKORRI"));
    }

    /** 3,4 % des lignes réelles n'ont pas de note : cela doit donner `null`, pas une erreur. */
    @Test
    void lecture_sansNote_renvoieNull() throws Exception {
        ComProspection p = newProspection();
        newInteractionLegataire(p, null);

        mockMvc.perform(get("/api/prospections/{id}/interactions", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].note").doesNotExist())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    // --- Écriture ---

    @Test
    void creation_ecritLeTexteDansAlertMessageEtPasDansDescription() throws Exception {
        ComProspection p = newProspection();
        String body = """
                {"description":"visite effectuee, tres interesse par le T3","entrante":false}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("visite effectuee, tres interesse par le T3"));

        ComActionMessage enBase = actionMessageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null && p.getId().equals(m.getComProspection().getId()))
                .findFirst().orElseThrow();

        // La vérification qui compte : c'est bien la colonne du légataire qui est alimentée.
        assertThat(enBase.getAlertMessage()).isEqualTo("visite effectuee, tres interesse par le T3");
        assertThat(enBase.getDescription()).doesNotContain("visite effectuee");
    }

    /**
     * Le libellé est généré et jamais imposé par l'appelant : c'est ce qui rend nos
     * interactions indiscernables de celles de l'application de bureau.
     */
    @Test
    void creation_genereLeLibelleAuFormatDuLegataire() throws Exception {
        ComProspection p = newProspection();
        String body = """
                {"description":"note quelconque"}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                // jj/MM/aaaa hh:mm, éventuellement suivi du nom de l'auteur.
                .andExpect(jsonPath("$.description")
                        .value(org.hamcrest.Matchers.matchesPattern(
                                "^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}( .+)?$")));
    }

    /**
     * R3 de la spec : sans principal authentifié — le cas de {@code @WithMockUser}, qui porte
     * un principal d'un autre type — la création réussit, l'auteur reste nul et le libellé se
     * réduit à la date. Une interaction sans auteur vaut mieux qu'une exception.
     */
    @Test
    void creation_sansPrincipalApplicatif_reussitSansAuteur() throws Exception {
        ComProspection p = newProspection();
        String body = """
                {"description":"saisie sans utilisateur applicatif"}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("saisie sans utilisateur applicatif"))
                .andExpect(jsonPath("$.employeId").doesNotExist());

        ComActionMessage enBase = actionMessageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null && p.getId().equals(m.getComProspection().getId()))
                .findFirst().orElseThrow();

        assertThat(enBase.getAdmUser()).isNull();
        assertThat(enBase.getUserCreateDateTime()).isNotNull();
        assertThat(enBase.getDescription()).matches("^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}$");
    }

    /** L'aller-retour complet : ce qu'on écrit est bien ce qu'on relit. */
    @Test
    void allerRetour_laNoteEcriteRessortEnLecture() throws Exception {
        ComProspection p = newProspection();
        String body = """
                {"description":"rdv fixe au 15/09 a 10h"}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/prospections/{id}/interactions", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].note").value("rdv fixe au 15/09 a 10h"));
    }
}
