package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsReunions.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.REUNIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revue du lot N1 : R7 contourné par la suppression d'une décision, contrat Swagger, cohérence des lignes filles. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierRevueTest {

    private static final LocalDateTime AVANT = LocalDateTime.of(2026, 3, 3, 9, 0);
    private static final String DECISIONS = "/decisions/";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private UUID type;
    private UUID ouvert;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRV-" + jeton);
        type = fixture.type("RV-" + jeton);
        ouvert = fixture.statut("OUV-" + jeton, false);
        admin = fixture.admin(jeton, projet);
    }

    private UUID nouvelle() throws Exception {
        Reponse r = appels.creer(projet, type, null, LocalDate.of(2026, 3, 10), null, admin);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return r.id();
    }

    /** R7 + Swagger « 409 Décision reprise » : supprimer la décision d'origine laisse un BulMeetingLineId orphelin. */
    @Test
    void r7_suppressionDecisionRepriseParUneReunionSuivante_409() throws Exception {
        UUID reunion = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID decision = fixture.decision(reunion, 1, "Origine", ouvert, null);
        UUID suivante = fixture.reunion(projet, type, 2, "R2-" + jeton, AVANT.plusDays(7));
        fixture.decision(suivante, 1, "Reprise", ouvert, decision);

        Reponse r = appels.appeler(delete(REUNIONS + "/" + reunion + DECISIONS + decision), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingLine WHERE PKey = ?", decision)).isEqualTo(1);
    }

    /** Swagger de POST …/maitres-ouvrage : « 400 Liste vide ou contact inconnu ». */
    @Test
    void swagger_ajoutMaitresOuvrage_listeVide_400() throws Exception {
        UUID reunion = nouvelle();

        Reponse r = appels.envoyer(post(REUNIONS + "/" + reunion + "/maitres-ouvrage"), "{\"contactIds\":[]}", admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void patchMaitreOuvrage_presentEtANotifier_ecritsEtRelus() throws Exception {
        UUID reunion = nouvelle();
        UUID ligne = fixture.maitreOuvrage(reunion, fixture.tiers("MO-" + jeton));

        Reponse r = appels.envoyer(patch(REUNIONS + "/" + reunion + "/maitres-ouvrage/" + ligne),
                "{\"present\":false,\"aNotifier\":true}", admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingContact WHERE PKey = ? AND Notify = 1 AND IsPresent = 0",
                ligne)).isEqualTo(1);
        assertThat(r.<List<Boolean>>lire("$.maitresOuvrage[?(@.id == '" + ligne + "')].aNotifier")).containsExactly(true);
    }

    @Test
    void decisionDUneAutreReunion_404() throws Exception {
        UUID autre = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID decision = fixture.decision(autre, 1, "D'une autre réunion", ouvert, null);
        UUID reunion = nouvelle();

        Reponse r = appels.envoyer(put(REUNIONS + "/" + reunion + DECISIONS + decision),
                "{\"description\":\"Détournée\",\"observationId\":\"" + ouvert + "\"}", admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.lire("SELECT Description FROM BulMeetingLine WHERE PKey = ?", decision))
                .isEqualTo("D'une autre réunion");
    }

    @Test
    void r6_intervenantMemeLotEtFournisseur_409() throws Exception {
        UUID reunion = nouvelle();
        String corps = "{\"fournisseurId\":\"" + fixture.tiers("FOU-" + jeton) + "\",\"effectif\":2}";
        Reponse premier = appels.envoyer(post(REUNIONS + "/" + reunion + "/intervenants"), corps, admin);

        Reponse second = appels.envoyer(post(REUNIONS + "/" + reunion + "/intervenants"), corps, admin);

        assertThat(premier.statut()).as(premier.corps()).isEqualTo(200);
        assertThat(second.statut()).as(second.corps()).isEqualTo(409);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulDelegate WHERE BulMeetingId = ?", reunion)).isEqualTo(1);
    }

    @Test
    void contactDUnAutreIntervenant_404() throws Exception {
        UUID reunion = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID intervenantA = fixture.intervenant(reunion, fixture.tiers("FA-" + jeton));
        UUID intervenantB = fixture.intervenant(reunion, fixture.tiers("FB-" + jeton));
        UUID contactA = fixture.contactIntervenant(intervenantA, fixture.tiers("CA-" + jeton));

        Reponse r = appels.appeler(delete(REUNIONS + "/" + reunion + "/intervenants/" + intervenantB + "/contacts/"
                + contactA), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulDelegateContact WHERE PKey = ?", contactA)).isEqualTo(1);
    }
}
