package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Patron générique {@code /api/referentiels-admin/{table}} — FEATURE-patrimoine-referentiels.md §1, §2, §4, §8 :
 * création 201, unicité 409 (simple et composite), requis 400, PATCH 200, DELETE 204 / 409, lecture et recherche.
 * Les 409 de suppression sur FK NO_ACTION ne passent qu'en CI (yesRemsTest n'a aucune FK).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminTest {

    private static final String ID = "$.id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureEtatVentes etatVentes;
    @Autowired private FixtureReferentielsAdmin fixture;
    @Autowired private StkVocationRepository vocationRepository;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    /** Référentiels dont seuls Code (O U) et Description (O) sont requis, plus Sorting (O) pour les trois derniers. */
    static Stream<TableSimple> tablesCodeLibelle() {
        return Stream.of(
                new TableSimple(VOCATIONS, "StkVocation", false),
                new TableSimple(FACADES, "StkFacade", false),
                new TableSimple(VUES, "StkVue", false),
                new TableSimple(CATEGORIES_CHARGE, "StkCostCategory", false),
                new TableSimple(CONTENANCES, "StkContenance", false),
                new TableSimple(STANDINGS, "CfgTrancheStanding", false),
                new TableSimple(VOCATIONS_TERRAIN, "LndVocation", false),
                new TableSimple(NIVEAUX_TERRAIN, "LndLevel", false),
                new TableSimple(RUBRIQUES_TRANCHE, "StkTrancheHeading", false),
                new TableSimple(IMMEUBLES, "StkBuilding", true),
                new TableSimple(ETAGES, "StkFloor", true),
                new TableSimple(ORIENTATIONS, "StkOrientation", true));
    }

    record TableSimple(String slug, String table, boolean avecTri) {
        CorpsJson corps(String code) {
            return corpsSimple(code, avecTri);
        }

        @Override
        public String toString() {
            return slug;
        }
    }

    private ResultActions poster(String table, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, table).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(String table, UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(LIGNE, table, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private static CorpsJson corpsSimple(String code, boolean avecTri) {
        CorpsJson corps = corps().avec(CODE, code).avec(DESCRIPTION, "Libelle " + code);
        return avecTri ? corps.avec(SORTING, 7) : corps;
    }

    private UUID creer(String table, CorpsJson corps) throws Exception {
        String json = poster(table, corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, ID));
    }

    // ------------------------------------------------------------------ patron générique, tables code / libellé

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_creation_201_ligneEcrite(TableSimple t) throws Exception {
        String code = "TST-" + jeton;

        UUID id = creer(t.slug(), t.corps(code));

        assertThat(fixture.lignes(t.table(), id, code, "Libelle " + code)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IMMEUBLES, ETAGES, ORIENTATIONS})
    void s2_sortingObligatoire_ecrit(String slug) throws Exception {
        UUID id = creer(slug, corpsSimple("TRI-" + jeton, true));
        String table = switch (slug) {
            case IMMEUBLES -> "StkBuilding";
            case ETAGES -> "StkFloor";
            default -> "StkOrientation";
        };

        assertThat(fixture.compter("SELECT COUNT(*) FROM " + table + " WHERE PKey = :id AND Sorting = 7", id)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IMMEUBLES, ETAGES, ORIENTATIONS})
    void s2_creation_400_sansSorting(String slug) throws Exception {
        poster(slug, corpsSimple("STRI-" + jeton, true).sans(SORTING)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_unicite_409_codeDejaPris(TableSimple t) throws Exception {
        creer(t.slug(), t.corps("DBL-" + jeton));

        poster(t.slug(), t.corps("DBL-" + jeton)).andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_requis_400_sansCode(TableSimple t) throws Exception {
        poster(t.slug(), t.corps("SC-" + jeton).sans(CODE)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_requis_400_sansDescription(TableSimple t) throws Exception {
        poster(t.slug(), t.corps("SD-" + jeton).sans(DESCRIPTION)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_fiche_200(TableSimple t) throws Exception {
        UUID id = creer(t.slug(), t.corps("FI-" + jeton));

        mockMvc.perform(get(LIGNE, t.slug(), id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ID).value(id.toString()))
                .andExpect(jsonPath("$.code").value("FI-" + jeton))
                .andExpect(jsonPath("$.description").value("Libelle FI-" + jeton));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_fiche_404_idInconnu(TableSimple t) throws Exception {
        creer(t.slug(), t.corps("F404-" + jeton));

        mockMvc.perform(get(LIGNE, t.slug(), UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_liste_search_contientLaLigne(TableSimple t) throws Exception {
        UUID id = creer(t.slug(), t.corps("LST-" + jeton));

        mockMvc.perform(get(TABLE, t.slug()).with(connecte(admin)).param(SEARCH, "LST-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").exists());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_patch_200_descriptionModifiee_codeConserve(TableSimple t) throws Exception {
        String code = "MOD-" + jeton;
        UUID id = creer(t.slug(), t.corps(code));

        modifier(t.slug(), id, corps().avec(DESCRIPTION, "Nouveau " + jeton)).andExpect(status().isOk());

        assertThat(fixture.lignes(t.table(), id, code, "Nouveau " + jeton)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_patch_409_codeDUneAutreLigne(TableSimple t) throws Exception {
        creer(t.slug(), t.corps("PRIS-" + jeton));
        UUID id = creer(t.slug(), t.corps("LIBRE-" + jeton));

        modifier(t.slug(), id, corps().avec(CODE, "PRIS-" + jeton)).andExpect(status().isConflict());

        assertThat(fixture.lignes(t.table(), id, "LIBRE-" + jeton, "Libelle LIBRE-" + jeton)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_suppression_204_ligneEffacee(TableSimple t) throws Exception {
        UUID id = creer(t.slug(), t.corps("DEL-" + jeton));

        mockMvc.perform(delete(LIGNE, t.slug(), id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(fixture.lignes(t.table(), id)).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablesCodeLibelle")
    void s1_suppression_404_idInconnu(TableSimple t) throws Exception {
        creer(t.slug(), t.corps("D404-" + jeton));

        mockMvc.perform(delete(LIGNE, t.slug(), UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ descripteurs

    @Test
    void s1_descripteurs_200_listentLesDixNeufReferentiels() throws Exception {
        ResultActions reponse = mockMvc.perform(get(RACINE).with(connecte(admin))).andExpect(status().isOk());

        for (String slug : new String[]{CATEGORIES, TYPES, IMMEUBLES, ETAGES, ORIENTATIONS, VOCATIONS, NATURES, FACADES,
                VUES, CATEGORIES_CHARGE, CONTENANCES, TYPES_PRESENTATION, TYPES_FRAIS, STANDINGS, VOCATIONS_TERRAIN,
                NIVEAUX_TERRAIN, QUOTES_PARTS_EXONEREES, RUBRIQUES_TRANCHE, SEUILS_VERSEMENT}) {
            reponse.andExpect(content().string(containsString("\"" + slug + "\"")));
        }
    }

    // ------------------------------------------------------------------ catégories et types : unicité (nature, code)

    private CorpsJson corpsParNature(String code, UUID natureId, UUID categorieComptableId) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Libelle " + code)
                .avec(NATURE_ID, natureId).avec(CATEGORIE_COMPTABLE_ID, categorieComptableId);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIES, TYPES})
    void s2_parNature_creation_201_natureEtCategorieComptableEcrites(String slug) throws Exception {
        UUID nature = fixture.nature("NAT-" + jeton);
        UUID stl = fixture.categorieComptable("STL-" + jeton);

        UUID id = creer(slug, corpsParNature("CAT-" + jeton, nature, stl));

        assertThat(fixture.compter("SELECT COUNT(*) FROM " + tableParNature(slug) + " WHERE PKey = :id AND StkKindId = '"
                + nature + "' AND StlCategoryId = '" + stl + "' AND Code = N'CAT-" + jeton + "'", id)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIES, TYPES})
    void s1_uniciteComposite_409_memeNatureMemeCode(String slug) throws Exception {
        UUID nature = fixture.nature("NAT-" + jeton);
        UUID stl = fixture.categorieComptable("STL-" + jeton);
        creer(slug, corpsParNature("CMP-" + jeton, nature, stl));

        poster(slug, corpsParNature("CMP-" + jeton, nature, stl)).andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIES, TYPES})
    void s1_uniciteComposite_201_memeCodeAutreNature(String slug) throws Exception {
        UUID stl = fixture.categorieComptable("STL-" + jeton);
        creer(slug, corpsParNature("CMP-" + jeton, fixture.nature("NAT1-" + jeton), stl));

        poster(slug, corpsParNature("CMP-" + jeton, fixture.nature("NAT2-" + jeton), stl)).andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIES, TYPES})
    void s2_parNature_400_sansNature(String slug) throws Exception {
        UUID stl = fixture.categorieComptable("STL-" + jeton);

        poster(slug, corpsParNature("SN-" + jeton, null, stl).sans(NATURE_ID)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIES, TYPES})
    void s2_parNature_400_sansCategorieComptable(String slug) throws Exception {
        UUID nature = fixture.nature("NAT-" + jeton);

        poster(slug, corpsParNature("SS-" + jeton, nature, null).sans(CATEGORIE_COMPTABLE_ID))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIES, TYPES})
    void s1_choixInconnu_400_natureInexistante(String slug) throws Exception {
        UUID stl = fixture.categorieComptable("STL-" + jeton);

        poster(slug, corpsParNature("NI-" + jeton, UUID.randomUUID(), stl)).andExpect(status().isBadRequest());
    }

    private static String tableParNature(String slug) {
        return CATEGORIES.equals(slug) ? "StkItemCategory" : "StkItemType";
    }

    // ------------------------------------------------------------------ types de frais

    private CorpsJson corpsTypeFrais(String code, UUID stl, UUID marche) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Libelle " + code)
                .avec(CATEGORIE_COMPTABLE_ID, stl).avec(TYPE_MARCHE_ID, marche);
    }

    @Test
    void s2_typesFrais_creation_201_categorieEtMarcheEcrits() throws Exception {
        UUID stl = fixture.categorieComptable("STL-" + jeton);
        UUID marche = fixture.typeMarche("MKT-" + jeton);

        UUID id = creer(TYPES_FRAIS, corpsTypeFrais("FRS-" + jeton, stl, marche));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkExpenseType WHERE Pkey = :id AND StlCategoryId = '" + stl
                + "' AND PrjMarketTypeId = '" + marche + "' AND Code = N'FRS-" + jeton + "'", id)).isEqualTo(1);
    }

    @Test
    void s2_typesFrais_400_sansTypeMarche() throws Exception {
        UUID stl = fixture.categorieComptable("STL-" + jeton);

        poster(TYPES_FRAIS, corpsTypeFrais("FSM-" + jeton, stl, null).sans(TYPE_MARCHE_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void s2_typesFrais_409_codeDejaPris() throws Exception {
        UUID stl = fixture.categorieComptable("STL-" + jeton);
        UUID marche = fixture.typeMarche("MKT-" + jeton);
        creer(TYPES_FRAIS, corpsTypeFrais("FDB-" + jeton, stl, marche));

        poster(TYPES_FRAIS, corpsTypeFrais("FDB-" + jeton, stl, marche)).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ quote-part exonérée : unicité (année, tranche)

    private CfgTranche trancheActive(String code) {
        return patrimoine.tranche(code, patrimoine.projet("PRJ-" + code, patrimoine.entite("EJ-" + code, true)));
    }

    private static CorpsJson corpsExoneration(int annee, UUID trancheId) {
        return corps().avec(ANNEE, annee).avec(TRANCHE_ID, trancheId);
    }

    @Test
    void s2_exoneration_creation_201_anneeEtTrancheEcrites() throws Exception {
        CfgTranche tranche = trancheActive("EXO-" + jeton);

        UUID id = creer(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, tranche.getId()));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItemExonaration WHERE PKey = :id AND DocYear = 2031 "
                + "AND CfgTrancheId = '" + tranche.getId() + "'", id)).isEqualTo(1);
    }

    @Test
    void s1_exoneration_uniciteComposite_409_memeAnneeMemeTranche() throws Exception {
        CfgTranche tranche = trancheActive("EXO-" + jeton);
        creer(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, tranche.getId()));

        poster(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, tranche.getId())).andExpect(status().isConflict());
    }

    @Test
    void s1_exoneration_uniciteComposite_201_memeAnneeAutreTranche() throws Exception {
        creer(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, trancheActive("EXO1-" + jeton).getId()));

        poster(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, trancheActive("EXO2-" + jeton).getId()))
                .andExpect(status().isCreated());
    }

    @Test
    void s2_exoneration_400_sansAnnee() throws Exception {
        poster(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, trancheActive("EXO-" + jeton).getId()).sans(ANNEE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void s2_exoneration_400_sansTranche() throws Exception {
        poster(QUOTES_PARTS_EXONEREES, corpsExoneration(2031, null).sans(TRANCHE_ID)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ suppression refusée (§0 NO_ACTION, §8.1 CASCADE)

    /** §8.1 : StkHierarchyPath → StkFloor est en CASCADE ; on refuse plutôt que d'effacer un chemin de plan. */
    @Test
    void r8_1_etagePorteParUnChemin_409_conserve() throws Exception {
        UUID etage = creer(ETAGES, corpsSimple("ETG-" + jeton, true));
        UUID chemin = fixture.cheminDEtage(etage);

        mockMvc.perform(delete(LIGNE, ETAGES, etage).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.lignes("StkFloor", etage) + fixture.lignes("StkHierarchyPath", chemin)).isEqualTo(2);
    }

    /** NO_ACTION StkItem → StkItemCategory : 409 en CI seulement. */
    @Test
    void s0_categorieUtiliseeParUnArticle_409_conservee() throws Exception {
        StkItemCategory categorie = etatVentes.categorie("CAT-409-" + jeton);
        StkItemType type = etatVentes.type("TYP-409-" + jeton);
        etatVentes.article("ART-409-" + jeton, trancheActive("T409-" + jeton), categorie, type);

        mockMvc.perform(delete(LIGNE, CATEGORIES, categorie.getId()).with(connecte(admin)))
                .andExpect(status().isConflict());

        assertThat(fixture.lignes("StkItemCategory", categorie.getId())).isEqualTo(1);
    }

    /** NO_ACTION StkItem → StkVocation : 409 en CI seulement. */
    @Test
    void s0_vocationUtiliseeParUnArticle_409_conservee() throws Exception {
        UUID id = creer(VOCATIONS, corpsSimple("VOC-409-" + jeton, false));
        StkVocation vocation = vocationRepository.findById(id).orElseThrow();
        etatVentes.article("ART-V409-" + jeton, trancheActive("TV409-" + jeton), etatVentes.categorie("CV-" + jeton),
                etatVentes.type("TV-" + jeton), vocation);

        mockMvc.perform(delete(LIGNE, VOCATIONS, id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.lignes("StkVocation", id)).isEqualTo(1);
    }

    @Test
    void s1_slugInconnu_404() throws Exception {
        mockMvc.perform(get(TABLE, ORIENTATIONS).with(connecte(admin))).andExpect(status().isOk());

        mockMvc.perform(get(TABLE, "table-inconnue").with(connecte(admin))).andExpect(status().isNotFound());
    }
}
