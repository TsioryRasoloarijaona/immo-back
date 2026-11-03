package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.CATEGORIE_LIBELLE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.GROUPE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.MODIFIE_LE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Réponses T1 à T10 de FEATURE-admin-fichiers-attaches.md, non couvertes par les tests d'acceptation. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichiersAttachesReponsesTest {

    private static final String URL = "/api/administration/fichiers-attaches";
    private static final String P_RECHERCHE = "recherche";
    private static final String P_GROUPES = "groupes";
    private static final String NON_DEFINI = "NON_DEFINI";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFichiersAttaches fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = "FR" + jeton();
    }

    private ResultActions lire(String url, String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(url).with(connecte(admin));
        for (int i = 0; i < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private List<String> ids(String... parametres) throws Exception {
        String corps = lire(URL, parametres).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corps, "$.content[*].id");
    }

    private FixtureFichiersAttaches.Piece piece() {
        return fixture.piece(jeton, DATE);
    }

    @Test
    void t1_champAvecFichier_vraiOuFaux() throws Exception {
        piece().inserer();
        piece().cocheeSansFichier().inserer();

        String corps = lire(URL, P_RECHERCHE, jeton).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<List<Boolean>>read(corps, "$.content[*].avecFichier"))
                .containsExactlyInAnyOrder(true, false);
    }

    @Test
    void t2_periodeModification_restreint() throws Exception {
        UUID dedans = piece().avec(MODIFIE_LE, DATE).inserer();
        piece().avec(MODIFIE_LE, DATE.minusDays(5)).inserer();
        piece().inserer();

        assertThat(ids(P_RECHERCHE, jeton, "modifieDu", "2026-03-10", "modifieAu", "2026-03-10"))
                .containsExactly(dedans.toString());
    }

    @Test
    void t3_groupeNonDefini_videOuNull() throws Exception {
        UUID vide = piece().inserer();
        UUID nul = piece().sans(GROUPE).inserer();
        piece().avec(GROUPE, "PLAN-" + jeton).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_GROUPES, NON_DEFINI))
                .containsExactlyInAnyOrder(vide.toString(), nul.toString());
    }

    @Test
    void t3_groupeNonDefini_etUnGroupe_union() throws Exception {
        UUID vide = piece().inserer();
        UUID plan = piece().avec(GROUPE, "PLAN-" + jeton).inserer();
        piece().avec(GROUPE, "AUTRE-" + jeton).inserer();

        MockHttpServletRequestBuilder requete = get(URL).with(connecte(admin)).param(P_RECHERCHE, jeton)
                .param(P_GROUPES, NON_DEFINI).param(P_GROUPES, "PLAN-" + jeton);
        String corps = mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<List<String>>read(corps, "$.content[*].id"))
                .containsExactlyInAnyOrder(vide.toString(), plan.toString());
    }

    @Test
    void t3_filtres_proposentNonDefini() throws Exception {
        piece().inserer();

        lire(URL + "/filtres").andExpect(status().isOk())
                .andExpect(jsonPath("$.groupeNonDefini.code").value(NON_DEFINI))
                .andExpect(jsonPath("$.groupeNonDefini.libelle").value("Non défini"));
    }

    @Test
    void t4_auInclutToutLaJournee() throws Exception {
        UUID soir = fixture.piece(jeton, LocalDateTime.of(2026, 3, 10, 23, 59, 0)).inserer();
        fixture.piece(jeton, LocalDateTime.of(2026, 3, 11, 0, 0, 0)).inserer();

        assertThat(ids(P_RECHERCHE, jeton, "du", "2026-03-10", "au", "2026-03-10"))
                .containsExactly(soir.toString());
    }

    @ParameterizedTest
    @CsvSource({"du,au", "modifieDu,modifieAu"})
    void t4_periodeInversee_400(String debut, String fin) throws Exception {
        lire(URL, debut, "2026-03-11", fin, "2026-03-10").andExpect(status().isBadRequest());
    }

    @Test
    void t5_recherche_surLibelleDeCategorieEtType_sansCasse() throws Exception {
        UUID parCategorie = piece().avec(DESCRIPTION, "x").avec(CATEGORIE_LIBELLE, "Cat " + jeton).inserer();
        UUID parType = piece().avec(DESCRIPTION, "y").avec(TYPE_ID, fixture.type("TYPE " + jeton)).inserer();

        assertThat(ids(P_RECHERCHE, jeton.toLowerCase(Locale.ROOT)))
                .containsExactlyInAnyOrder(parCategorie.toString(), parType.toString());
    }

    @Test
    void t6_decaissement_objetCleNul() throws Exception {
        piece().avec(CATEGORIE, "Yes.Stl.Debit.Settlement").inserer();

        lire(URL, P_RECHERCHE, jeton).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categorie").value("DECAISSEMENT"))
                .andExpect(jsonPath("$.content[0].objetCle").doesNotExist());
    }

    @Test
    void categorieAutre_classeInconnueEtVide() throws Exception {
        UUID inconnue = piece().avec(CATEGORIE, "Yes.Cfg.CfgProject").inserer();
        UUID vide = piece().avec(CATEGORIE, "").inserer();
        piece().inserer();

        assertThat(ids(P_RECHERCHE, jeton, "categories", "AUTRE"))
                .containsExactlyInAnyOrder(inconnue.toString(), vide.toString());
    }

    @ParameterizedTest
    @CsvSource({"taille,0", "taille,201", "page,-1"})
    void t10_paginationHorsBornes_400(String parametre, String valeur) throws Exception {
        lire(URL, parametre, valeur).andExpect(status().isBadRequest());
    }

    @Test
    void export_classeurXlsx() throws Exception {
        piece().inserer();

        lire(URL + "/export", P_RECHERCHE, jeton).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void export_sansRoleAdministrateur_403() throws Exception {
        mockMvc.perform(get(URL + "/export").with(FixtureDossierVente.connecte(admin, "Commercial")))
                .andExpect(status().isForbidden());
    }

    @Test
    void t10_taille200_acceptee() throws Exception {
        lire(URL, "taille", "200").andExpect(status().isOk()).andExpect(jsonPath("$.size").value(200));
    }
}
