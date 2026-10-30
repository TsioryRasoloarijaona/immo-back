package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEmailHistorique;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.AUTOMATIQUE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.CORPS;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.COPIE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.COPIE_CACHEE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.DESTINATAIRES;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.ENVOYE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.ERREUR;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.EXPEDITEUR;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.HTML;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.LIBELLE_OBJET;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.OBJET;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.UTILISATEUR;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fiche d'un e-mail — FEATURE-admin-email-historique.md, règle 6 et décision sur le corps. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailHistoriqueDetailTest {

    private static final String URL = "/api/administration/emails/{id}";
    private static final String CHAMP_CORPS = "$.corps";
    private static final String CHAMP_HTML = "$.html";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureEmailHistorique fixture;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        fixture.assurerTable();
        admin = patrimoine.administrateur();
    }

    private ResultActions fiche(UUID id) throws Exception {
        return mockMvc.perform(get(URL, id).with(connecte(admin)));
    }

    @Test
    void detail_expose_laLigne_etLesChampsDeLaFiche() throws Exception {
        UUID email = fixture.email(DATE)
                .avec(UTILISATEUR, "Karim")
                .avec(EXPEDITEUR, "ventes@ayad.test")
                .avec(DESTINATAIRES, "client@ayad.test")
                .avec(COPIE, "copie@ayad.test")
                .avec(COPIE_CACHEE, "cachee@ayad.test")
                .avec(OBJET, "Appel de fonds")
                .avec(LIBELLE_OBJET, "Dossier 2026/001")
                .avec(CORPS, "Bonjour,\nCordialement")
                .avec(AUTOMATIQUE, true)
                .avec(ENVOYE, false)
                .avec(ERREUR, "Serveur injoignable")
                .inserer();

        fiche(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(email.toString()))
                .andExpect(jsonPath("$.date", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath("$.utilisateur").value("Karim"))
                .andExpect(jsonPath("$.expediteur").value("ventes@ayad.test"))
                .andExpect(jsonPath("$.destinataires").value("client@ayad.test"))
                .andExpect(jsonPath("$.copie").value("copie@ayad.test"))
                .andExpect(jsonPath("$.copieCachee").value("cachee@ayad.test"))
                .andExpect(jsonPath("$.objet").value("Appel de fonds"))
                .andExpect(jsonPath("$.libelleObjet").value("Dossier 2026/001"))
                .andExpect(jsonPath(CHAMP_CORPS).value("Bonjour,\nCordialement"))
                .andExpect(jsonPath("$.automatique").value(true))
                .andExpect(jsonPath("$.envoye").value(false))
                .andExpect(jsonPath("$.erreur").value("Serveur injoignable"));
    }

    @Test
    void decision_html_faux_pourUnCorpsTexte() throws Exception {
        UUID email = fixture.email(DATE).avec(CORPS, "Bonjour").inserer();

        fiche(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath(CHAMP_HTML).value(false));
    }

    // Si `corps` garde ou non les balises : voir « Questions sur le spec », seul le texte est figé ici.
    @Test
    void decision_html_vrai_pourUnCorpsHtml_etCorpsRendLeTexte_sansCorpsHtml() throws Exception {
        UUID email = fixture.email(DATE).avec(HTML, true).avec(CORPS, "<p>Bonjour</p>").inserer();

        fiche(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath(CHAMP_HTML).value(true))
                .andExpect(jsonPath(CHAMP_CORPS, containsString("Bonjour")))
                .andExpect(jsonPath("$.corpsHtml").doesNotExist());
    }

    @Test
    void detail_colonnesDeTexteVides_200() throws Exception {
        UUID email = fixture.email(DATE).inserer();

        fiche(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(email.toString()));
    }

    // Une route absente répond aussi 404 : le témoin 200 prouve que ce refus vient bien de l'id.
    @Test
    void r6_idInconnu_404() throws Exception {
        UUID existant = fixture.email(DATE).inserer();

        fiche(existant).andExpect(status().isOk());
        fiche(UUID.randomUUID()).andExpect(status().isNotFound());
    }
}
