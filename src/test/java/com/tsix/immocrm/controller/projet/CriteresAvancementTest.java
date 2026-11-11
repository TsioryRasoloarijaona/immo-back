package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critères d'avancement — FEATURE-projets-config-parite.md « Règles OMRAN confirmées » §4 et « Contrat » : CRUD de
 * {@code PrjProjectProgressCriteriaLine} ({@code ordre}, {@code description} ≤ 255 obligatoire, {@code categorieId} obligatoire, catégorie
 * de dépense seulement), suppression d'un critère cité par un PV → 409, import depuis un autre projet qui remplace toutes les lignes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CriteresAvancementTest {

    private static final String COMPTE_LIGNE = "SELECT COUNT(*) FROM PrjProjectProgressCriteriaLine WHERE PKey = :id";
    private static final String COMPTE_PROJET = "SELECT COUNT(*) FROM PrjProjectProgressCriteriaLine WHERE PrjProjectId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;

    private AdmUser admin;
    private AdmUser commercial;
    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID depense;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
        fixture.colonnesCriteres();
        projet = fixture.projet("CP-" + jeton, societe);
        depense = fixture.categorie("DEP-" + jeton, false);
    }

    private static RequestPostProcessor enCommercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, "Commercial");
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(requete.with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private CorpsJson critere(int ordre, String description, UUID categorie) {
        return corps().avec(ORDRE, ordre).avec(DESCRIPTION, description).avec(CATEGORIE_ID, categorie);
    }

    private int lignesDuProjet(UUID id) {
        return fixture.compter(COMPTE_PROJET, id);
    }

    // ------------------------------------------------------------------ lecture

    @Test
    void c_liste_lignesDuProjetAvecOrdreDescriptionCategorie() throws Exception {
        UUID ligne = fixture.critere(projet, 2, "Gros oeuvre " + jeton, depense);
        String filtre = "$[?(@.id == '" + ligne + "')]";

        mockMvc.perform(get(CRITERES, projet).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre + "." + ORDRE, hasItem(2)))
                .andExpect(jsonPath(filtre + "." + DESCRIPTION, hasItem("Gros oeuvre " + jeton)))
                .andExpect(jsonPath(filtre + "." + CATEGORIE_ID, hasItem(depense.toString())));
    }

    @Test
    void c_liste_neMontrePasLesCriteresDUnAutreProjet() throws Exception {
        UUID autre = fixture.projet("CA-" + jeton, societe);
        fixture.critere(autre, 1, "Autre " + jeton, depense);

        mockMvc.perform(get(CRITERES, projet).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------ création

    @Test
    void c_creation_2xx_ligneEnBase() throws Exception {
        String json = ecrire(post(CRITERES, projet), critere(3, "Electricite " + jeton, depense), connecte(admin))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(fixture.compter("""
                SELECT COUNT(*) FROM PrjProjectProgressCriteriaLine WHERE PKey = :id AND PrjProjectId = :projet
                  AND Sorting = 3 AND Description = :description AND StlCategoryId = :categorie""",
                Map.of("id", id, "projet", projet, "description", "Electricite " + jeton, "categorie", depense)))
                .isEqualTo(1);
    }

    @ParameterizedTest(name = "sans {0}")
    @ValueSource(strings = {DESCRIPTION, CATEGORIE_ID})
    void c_creation_champObligatoireManquant_400_rienEcrit(String champ) throws Exception {
        ecrire(post(CRITERES, projet), critere(1, "Plomberie " + jeton, depense).sans(champ), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(lignesDuProjet(projet)).isZero();
    }

    @Test
    void c_creation_descriptionDe256Caracteres_400_rienEcrit() throws Exception {
        ecrire(post(CRITERES, projet), critere(1, "x".repeat(256), depense), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(lignesDuProjet(projet)).isZero();
    }

    @Test
    void c_creation_descriptionDe255Caracteres_acceptee() throws Exception {
        ecrire(post(CRITERES, projet), critere(1, "x".repeat(255), depense), connecte(admin))
                .andExpect(status().is2xxSuccessful());

        assertThat(lignesDuProjet(projet)).isEqualTo(1);
    }

    @Test
    void c_creation_categorieDeRecette_400_rienEcrit() throws Exception {
        UUID recette = fixture.categorie("REC-" + jeton, true);

        ecrire(post(CRITERES, projet), critere(1, "Recette " + jeton, recette), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(lignesDuProjet(projet)).isZero();
    }

    // ------------------------------------------------------------------ modification

    @Test
    void c_modification_200_ordreDescriptionCategorieEnBase() throws Exception {
        UUID ligne = fixture.critere(projet, 1, "Avant " + jeton, depense);
        UUID autreDepense = fixture.categorie("DEP2-" + jeton, false);

        ecrire(put(CRITERE, projet, ligne), critere(5, "Apres " + jeton, autreDepense), connecte(admin))
                .andExpect(status().isOk());

        assertThat(fixture.compter("""
                SELECT COUNT(*) FROM PrjProjectProgressCriteriaLine WHERE PKey = :id
                  AND Sorting = 5 AND Description = :description AND StlCategoryId = :categorie""",
                Map.of("id", ligne, "description", "Apres " + jeton, "categorie", autreDepense))).isEqualTo(1);
    }

    @Test
    void c_modification_sansDescription_400_ligneInchangee() throws Exception {
        UUID ligne = fixture.critere(projet, 1, "Garde " + jeton, depense);

        ecrire(put(CRITERE, projet, ligne), critere(1, "Garde " + jeton, depense).sans(DESCRIPTION), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProjectProgressCriteriaLine WHERE PKey = :id AND Description = :d",
                Map.of("id", ligne, "d", "Garde " + jeton))).isEqualTo(1);
    }

    // ------------------------------------------------------------------ suppression

    @Test
    void c_suppression_204_ligneSupprimee() throws Exception {
        UUID ligne = fixture.critere(projet, 1, "Supprime " + jeton, depense);

        mockMvc.perform(delete(CRITERE, projet, ligne).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(fixture.compter(COMPTE_LIGNE, ligne)).isZero();
    }

    @Test
    void c_suppression_critereCiteParUnPv_409_ligneIntacte() throws Exception {
        UUID ligne = fixture.critere(projet, 1, "Cite " + jeton, depense);
        fixture.citerParPv(ligne);

        mockMvc.perform(delete(CRITERE, projet, ligne).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.compter(COMPTE_LIGNE, ligne)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ import depuis un autre projet

    @Test
    void c_import_remplaceToutesLesLignesParDesCopiesDuProjetSource() throws Exception {
        UUID ancienne = fixture.critere(projet, 1, "Ancienne " + jeton, depense);
        UUID source = fixture.projet("CS-" + jeton, societe);
        fixture.critere(source, 1, "Source A " + jeton, depense);
        fixture.critere(source, 2, "Source B " + jeton, depense);

        ecrire(post(IMPORTER_CRITERES, projet), corps().avec(PROJET_SOURCE_ID, source), connecte(admin))
                .andExpect(status().is2xxSuccessful());

        List<Integer> comptes = List.of(
                fixture.compter(COMPTE_LIGNE, ancienne),
                lignesDuProjet(projet),
                fixture.compter(COMPTE_PROJET + " AND Description IN (:a, :b)",
                        Map.of("id", projet, "a", "Source A " + jeton, "b", "Source B " + jeton)),
                lignesDuProjet(source));
        assertThat(comptes).containsExactly(0, 2, 2, 2);
    }

    @Test
    void c_import_ligneActuelleCiteeParUnPv_refuse_rienChange() throws Exception {
        UUID citee = fixture.critere(projet, 1, "Citee " + jeton, depense);
        fixture.citerParPv(citee);
        UUID source = fixture.projet("CR-" + jeton, societe);
        fixture.critere(source, 1, "Source " + jeton, depense);

        ecrire(post(IMPORTER_CRITERES, projet), corps().avec(PROJET_SOURCE_ID, source), connecte(admin))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isIn(400, 409));

        assertThat(List.of(fixture.compter(COMPTE_LIGNE, citee), lignesDuProjet(projet))).containsExactly(1, 1);
    }

    // ------------------------------------------------------------------ droits

    @Test
    void c_creation_nonAdministrateur_403_rienEcrit() throws Exception {
        ecrire(post(CRITERES, projet), critere(1, "Refus " + jeton, depense), enCommercial(commercial))
                .andExpect(status().isForbidden());

        assertThat(lignesDuProjet(projet)).isZero();
    }

    @Test
    void c_suppression_nonAdministrateur_403_ligneIntacte() throws Exception {
        UUID ligne = fixture.critere(projet, 1, "Garde " + jeton, depense);

        mockMvc.perform(delete(CRITERE, projet, ligne).with(enCommercial(commercial))).andExpect(status().isForbidden());

        assertThat(fixture.compter(COMPTE_LIGNE, ligne)).isEqualTo(1);
    }

    @Test
    void c_import_nonAdministrateur_403_lignesIntactes() throws Exception {
        UUID ligne = fixture.critere(projet, 1, "Garde " + jeton, depense);
        UUID source = fixture.projet("CN-" + jeton, societe);
        fixture.critere(source, 1, "Source " + jeton, depense);

        ecrire(post(IMPORTER_CRITERES, projet), corps().avec(PROJET_SOURCE_ID, source), enCommercial(commercial))
                .andExpect(status().isForbidden());

        assertThat(fixture.compter(COMPTE_LIGNE, ligne)).isEqualTo(1);
    }
}
