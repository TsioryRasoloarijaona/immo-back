package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.DOSSIER;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.GROUPE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtres de la liste des fichiers attachés — FEATURE-admin-fichiers-attaches.md, décision 4, règles testables 2 et
 * 7, réponse R2 (période « Créé le » par défaut : tout), endpoint {@code /filtres}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichiersAttachesFiltresTest {

    private static final String URL = "/api/administration/fichiers-attaches";
    private static final String URL_FILTRES = URL + "/filtres";
    private static final String P_RECHERCHE = "recherche";
    private static final String P_TYPES = "typeIds";
    private static final String P_CATEGORIES = "categories";
    private static final String P_GROUPES = "groupes";
    private static final String P_AVEC_FICHIER = "avecFichier";
    private static final String P_DU = "du";
    private static final String P_AU = "au";
    private static final String DU = "2026-03-09";
    private static final String AU = "2026-03-11";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFichiersAttaches fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = "FA" + jeton();
    }

    private String corps(String url, String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(url).with(connecte(admin));
        for (int i = 0; i < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> ids(String... parametres) throws Exception {
        return JsonPath.read(corps(URL, parametres), "$.content[*].id");
    }

    private FixtureFichiersAttaches.Piece piece() {
        return fixture.piece(jeton, DATE);
    }

    // ------------------------------------------------------------------ recherche texte

    @Test
    void d4_recherche_surLaDescription_sansTenirCompteDeLaCasse() throws Exception {
        UUID retenue = piece().avec(DESCRIPTION, "avant " + jeton + " apres").inserer();

        assertThat(ids(P_RECHERCHE, jeton.toLowerCase(Locale.ROOT))).containsExactly(retenue.toString());
    }

    @Test
    void endpoint_recherche_surLeGroupe() throws Exception {
        UUID retenue = piece().avec(DESCRIPTION, "sans le motif").avec(GROUPE, "G-" + jeton).inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(retenue.toString());
    }

    // ------------------------------------------------------------------ période de création

    @Test
    void r2_periodeCreation_parDefautTout() throws Exception {
        UUID ancienne = fixture.piece(jeton, LocalDateTime.of(2001, 1, 2, 3, 4)).inserer();
        UUID recente = piece().inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactlyInAnyOrder(ancienne.toString(), recente.toString());
    }

    @Test
    void d4_periodeCreation_restreintAuxLignesDeLaPeriode() throws Exception {
        UUID dedans = piece().inserer();
        fixture.piece(jeton, DATE.minusDays(3)).inserer();
        fixture.piece(jeton, DATE.plusDays(3)).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_DU, DU, P_AU, AU)).containsExactly(dedans.toString());
    }

    // ------------------------------------------------------------------ type

    @Test
    void r2_type_restreintAuxTypesCoches_sansTypeExclu() throws Exception {
        UUID plan = fixture.type("PLAN " + jeton);
        UUID contrat = fixture.type("CONTRAT " + jeton);
        UUID retenue = piece().avec(TYPE_ID, plan).inserer();
        piece().avec(TYPE_ID, contrat).inserer();
        piece().inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_TYPES, plan.toString())).containsExactly(retenue.toString());
    }

    @Test
    void r2_type_plusieursCoches_unionDesTypes() throws Exception {
        UUID plan = fixture.type("PLAN " + jeton);
        UUID contrat = fixture.type("CONTRAT " + jeton);
        UUID unePlan = piece().avec(TYPE_ID, plan).inserer();
        UUID unContrat = piece().avec(TYPE_ID, contrat).inserer();
        piece().inserer();

        MockHttpServletRequestBuilder requete = get(URL).with(connecte(admin)).param(P_RECHERCHE, jeton)
                .param(P_TYPES, plan.toString()).param(P_TYPES, contrat.toString());
        String reponse = mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<List<String>>read(reponse, "$.content[*].id"))
                .containsExactlyInAnyOrder(unePlan.toString(), unContrat.toString());
    }

    @Test
    void r2_rienCoche_tout() throws Exception {
        UUID avecType = piece().avec(TYPE_ID, fixture.type("PLAN " + jeton)).avec(GROUPE, "PLAN").inserer();
        UUID sansType = piece().inserer();
        UUID cochee = piece().cocheeSansFichier().inserer();

        assertThat(ids(P_RECHERCHE, jeton))
                .containsExactlyInAnyOrder(avecType.toString(), sansType.toString(), cochee.toString());
    }

    // ------------------------------------------------------------------ catégorie normalisée

    private String categorieDe(UUID piece) throws Exception {
        List<Map<String, Object>> lignes = JsonPath.read(corps(URL, P_RECHERCHE, jeton), "$.content");
        return lignes.stream().filter(l -> piece.toString().equals(l.get("id"))).findFirst()
                .map(l -> (String) l.get("categorie"))
                .orElseThrow(() -> new AssertionError("pièce absente de la liste : " + piece));
    }

    // Règle 2 : « Dossier » = Yes.Com.Sales.Sale + libellés « Dossier de vente N° … du … ».
    @Test
    void r2_categorieDossier_regroupeClasseEtLibelleDossierDeVente() throws Exception {
        UUID classe = piece().avec(CATEGORIE, DOSSIER).inserer();
        UUID libelle = piece().avec(CATEGORIE, "Dossier de vente N° 2026/001 du 10/03/2026").inserer();

        assertThat(categorieDe(libelle)).isNotBlank().isEqualTo(categorieDe(classe));
    }

    @Test
    void r2_categorie_restreintALaCategorieCochee() throws Exception {
        UUID classe = piece().avec(CATEGORIE, DOSSIER).inserer();
        UUID libelle = piece().avec(CATEGORIE, "Dossier de vente N° 2026/002 du 11/03/2026").inserer();
        UUID article = piece().avec(CATEGORIE, ARTICLE).inserer();
        String dossier = categorieDe(classe);
        assertThat(categorieDe(article)).isNotEqualTo(dossier);

        assertThat(ids(P_RECHERCHE, jeton, P_CATEGORIES, dossier))
                .containsExactlyInAnyOrder(classe.toString(), libelle.toString());
    }

    // ------------------------------------------------------------------ groupe (R2)

    @Test
    void r2_groupe_restreintAuGroupeCoche() throws Exception {
        UUID retenue = piece().avec(GROUPE, "PLAN-" + jeton).inserer();
        piece().avec(GROUPE, "AUTRE-" + jeton).inserer();
        piece().inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_GROUPES, "PLAN-" + jeton)).containsExactly(retenue.toString());
    }

    // ------------------------------------------------------------------ règle 7 : fichier vivant

    @Test
    void r7_avecFichierFaux_lignesSansAppFileObjectVivant() throws Exception {
        piece().inserer();
        UUID cochee = piece().cocheeSansFichier().inserer();
        UUID supprime = piece().fichierSupprime().inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_AVEC_FICHIER, "false"))
                .containsExactlyInAnyOrder(cochee.toString(), supprime.toString());
    }

    @Test
    void r7_avecFichierVrai_lignesAvecAppFileObjectVivant() throws Exception {
        UUID vivant = piece().inserer();
        piece().cocheeSansFichier().inserer();
        piece().fichierSupprime().inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_AVEC_FICHIER, "true")).containsExactly(vivant.toString());
    }

    // ------------------------------------------------------------------ règle 2 : combinaison = ET

    @Test
    void r2_filtresCombines_et() throws Exception {
        UUID plan = fixture.type("PLAN " + jeton);
        String groupe = "PLAN-" + jeton;
        UUID retenue = piece().avec(TYPE_ID, plan).avec(GROUPE, groupe).inserer();
        piece().avec(TYPE_ID, plan).avec(GROUPE, groupe).cocheeSansFichier().inserer();
        fixture.piece(jeton, DATE.minusDays(5)).avec(TYPE_ID, plan).avec(GROUPE, groupe).inserer();
        piece().avec(GROUPE, groupe).inserer();
        piece().avec(TYPE_ID, plan).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_TYPES, plan.toString(), P_GROUPES, groupe, P_AVEC_FICHIER, "true",
                P_DU, DU, P_AU, AU)).containsExactly(retenue.toString());
    }

    // ------------------------------------------------------------------ endpoint /filtres

    @Test
    void filtres_types_contientLeTypeCree() throws Exception {
        UUID plan = fixture.type("PLAN " + jeton);

        List<Map<String, Object>> types = JsonPath.read(corps(URL_FILTRES), "$.types");

        assertThat(types).anySatisfy(t -> assertThat(t)
                .containsEntry("id", plan.toString())
                .containsEntry("libelle", "PLAN " + jeton));
    }

    @Test
    void filtres_categories_contientLaCategorieDesDossiers() throws Exception {
        UUID classe = piece().avec(CATEGORIE, DOSSIER).inserer();
        String dossier = categorieDe(classe);

        List<Map<String, Object>> categories = JsonPath.read(corps(URL_FILTRES), "$.categories");

        assertThat(categories).anySatisfy(c -> assertThat(c).containsEntry("code", dossier)
                .containsKey("libelle"));
    }

    @Test
    void filtres_groupes_contientLeGroupeCree() throws Exception {
        piece().avec(GROUPE, "PLAN-" + jeton).inserer();

        List<String> groupes = JsonPath.read(corps(URL_FILTRES), "$.groupes");

        assertThat(groupes).contains("PLAN-" + jeton);
    }
}
