package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Compte;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUS_LES_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUTES_LES_TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.VALIDEE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.majuscules;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N2b R-N2b-4 (ligne {@code StrHistory} d'une transition, description OMRAN {@code RessourceWithWorkFlow.cs:267}), R-N2b-8
 * (ligne « Création » à la création), {@code GET …/historique} plus récent d'abord, R-N2b-3 (409 sans ligne écrite).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierStatutHistoriqueTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String DESCRIPTION = "Description";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;
    private AdmUser auteur;
    private RequestPostProcessor restreint;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GSH-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TH-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        Compte c = fixture.restreint(jeton + "r", chantier.projet(), TOUS_LES_STATUTS, TOUTES_LES_TRANSITIONS, DROIT_LIRE,
                DROIT_MODIFIER);
        auteur = c.user();
        restreint = c.connexion();
    }

    private UUID jour(UUID statut, String code) {
        return fixture.jour(new Jour(chantier.societe(), chantier.projet(), type, zone, statut, DATE.atStartOfDay(),
                code + "-" + jeton, null));
    }

    private String description(String transition, String depuis, String vers) {
        return transition + ": " + depuis + "=>" + vers + "(" + auteur.getCode() + " & )";
    }

    // ------------------------------------------------------------------ R-N2b-4 : ligne écrite par une transition

    @Test
    void r4_transition_uneLigneStrHistory_descriptionOmran_objetEntiteTransitionStatut() throws Exception {
        UUID jour = jour(EN_COURS, "H4A");

        Reponse r = appels.executer(jour, T_DEMANDE, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> historique = fixture.historique(jour);
        assertThat(historique).hasSize(1);
        Map<String, Object> ligne = historique.get(0);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ligne.get(DESCRIPTION)).isEqualTo(description("Demande de validation", "En cours",
                    "Demande de validation"));
            s.assertThat(String.valueOf(ligne.get("ObjectId"))).isEqualToIgnoringCase(jour.toString());
            s.assertThat(String.valueOf(ligne.get("StrConfigEntityId"))).isEqualToIgnoringCase(fixture.entite().toString());
            s.assertThat(String.valueOf(ligne.get("StrWorkflowId"))).isEqualToIgnoringCase(T_DEMANDE.toString());
            s.assertThat(String.valueOf(ligne.get("StrStatusId"))).isEqualToIgnoringCase(DEMANDE_VALIDATION.toString());
            s.assertThat(String.valueOf(ligne.get("AdmUserId"))).isEqualToIgnoringCase(auteur.getId().toString());
            s.assertThat(ligne.get("Notes")).isEqualTo("");
        });
    }

    @Test
    void r4_deuxTransitions_deuxLignes_lisiblesParLaRoute() throws Exception {
        UUID jour = jour(EN_COURS, "H4B");
        assertThat(appels.executer(jour, T_DEMANDE, restreint).statut()).isEqualTo(200);
        assertThat(appels.executer(jour, T_VALIDATION, restreint).statut()).isEqualTo(200);

        Reponse r = appels.historique(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fixture.historique(jour)).hasSize(2);
            s.assertThat(r.<List<String>>lire("$[*].description")).containsExactlyInAnyOrder(
                    description("Demande de validation", "En cours", "Demande de validation"),
                    description("Validation", "Demande de validation", "Validée"));
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(VALIDEE));
        });
    }

    // ------------------------------------------------------------------ R-N2b-8 : ligne « Création »

    @Test
    void r8_creation_201_uneLigneCreation_auStatutDeDepart() throws Exception {
        String code = "H8A-" + jeton;

        Reponse r = appels.creer(AppelsTravauxChantier.ecriture(chantier.societe(), chantier.projet(), type, zone, DATE, code),
                restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        UUID jour = UUID.fromString(fixture.lire("SELECT CAST(Pkey AS varchar(36)) FROM GrhWorkedDay WHERE Code = ?", code));
        List<Map<String, Object>> historique = fixture.historique(jour);
        assertThat(historique).hasSize(1);
        Map<String, Object> ligne = historique.get(0);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ligne.get(DESCRIPTION)).isEqualTo("Création");
            s.assertThat(String.valueOf(ligne.get("StrStatusId"))).isEqualToIgnoringCase(EN_COURS.toString());
            s.assertThat(String.valueOf(ligne.get("StrConfigEntityId"))).isEqualToIgnoringCase(fixture.entite().toString());
            s.assertThat(ligne.get("StrWorkflowId")).isNull();
        });
    }

    @Test
    void r8_creation_lisibleParLaRoute() throws Exception {
        String code = "H8B-" + jeton;
        assertThat(appels.creer(AppelsTravauxChantier.ecriture(chantier.societe(), chantier.projet(), type, zone, DATE, code),
                restreint).statut()).isEqualTo(201);
        UUID jour = UUID.fromString(fixture.lire("SELECT CAST(Pkey AS varchar(36)) FROM GrhWorkedDay WHERE Code = ?", code));

        Reponse r = appels.historique(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.<List<String>>lire("$[*].description")).containsExactly("Création");
    }

    // ------------------------------------------------------------------ GET historique : ordre et champs

    @Test
    void historique_plusRecentDAbord_avecSesChamps() throws Exception {
        UUID jour = jour(VALIDEE, "HOR");
        fixture.ligneHistorique(jour, EN_COURS, "Ancienne " + jeton, LocalDateTime.of(2077, 3, 1, 9, 0));
        fixture.ligneHistorique(jour, VALIDEE, "Recente " + jeton, LocalDateTime.of(2077, 3, 20, 9, 0));
        fixture.ligneHistorique(jour, DEMANDE_VALIDATION, "Milieu " + jeton, LocalDateTime.of(2077, 3, 10, 9, 0));

        Reponse r = appels.historique(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<List<String>>lire("$[*].description"))
                    .containsExactly("Recente " + jeton, "Milieu " + jeton, "Ancienne " + jeton);
            s.assertThat(r.<String>lire("$[0].utilisateur")).isEqualTo("Compte historique");
            s.assertThat(r.<String>lire("$[0].date")).startsWith("2077-03-20");
            s.assertThat(r.<String>lire("$[0].dateUtilisateur")).startsWith("2077-03-20");
            s.assertThat(r.<String>lire("$[0].notes")).isEmpty();
        });
    }

    @Test
    void historique_jourInconnu_404() throws Exception {
        UUID temoin = jour(EN_COURS, "HTI");

        Reponse ok = appels.historique(temoin, restreint);
        Reponse r = appels.historique(UUID.randomUUID(), restreint);

        assertThat(ok.statut()).as("témoin : " + ok.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // ------------------------------------------------------------------ R-N2b-3 : un 409 n'écrit aucune ligne

    @Test
    void r3_transitionRefusee_409_aucuneLigne_historiqueVide() throws Exception {
        UUID jour = jour(VALIDEE, "H3A");

        Reponse refus = appels.executer(jour, T_DEMANDE, restreint);
        Reponse r = appels.historique(jour, restreint);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(fixture.historique(jour)).isEmpty();
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.<List<String>>lire("$[*].description")).isEmpty();
        });
    }
}
