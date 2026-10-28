package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureLotissements.CATEGORIE_ID;
import static com.tsix.immocrm.fixtures.FixtureLotissements.CODE;
import static com.tsix.immocrm.fixtures.FixtureLotissements.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENTS;
import static com.tsix.immocrm.fixtures.FixtureLotissements.NOTES;
import static com.tsix.immocrm.fixtures.FixtureLotissements.PRIX_M2_DEFINITIF_HT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.PRIX_M2_DEFINITIF_TTC;
import static com.tsix.immocrm.fixtures.FixtureLotissements.SURFACE_DEFINITIVE;
import static com.tsix.immocrm.fixtures.FixtureLotissements.SURFACE_PROVISOIRE;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TAUX_TVA;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TITRE_FONCIER;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureLotissements.VOCATION_ID;
import static com.tsix.immocrm.fixtures.FixtureLotissements.corpsLot;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écriture d'un lot — FEATURE-patrimoine-lotissements.md §1.3, §2, règles L3 (POST), L4 à L9 ; décisions Q1 (pas de statut
 * de workflow), Q2 (catégories / types TER ou LOT DE TERRAIN), Q3 (code unique par tranche). Sans {@code @Transactional} :
 * chaque écriture est validée pour de bon et relue en base (carnet 27/10, écriture perdue) ; jeu effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotissementEcritureTest {

    private static final String STK_HIERARCHY_ID = "StkHierarchyId";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CE");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    // ------------------------------------------------------------------ outils

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(LOTISSEMENTS).with(connecte(j.admin()))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    /** Lot créé par l'API, retrouvé en base par tranche et code (la forme du corps 201 n'est pas imposée ici). */
    private UUID creerOk(CorpsJson corps, String code) throws Exception {
        creer(corps).andExpect(status().isCreated());
        List<UUID> ids = fixture.ids(j.tranche(), code);
        assertThat(ids).as("lot %s en base", code).hasSize(1);
        return ids.get(0);
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(LOTISSEMENT, id).with(connecte(j.admin()))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private static String ligneListe(UUID id) {
        return "$.content[?(@.id=='" + id + "')]";
    }

    // ------------------------------------------------------------------ L4 création

    /** L4 / §1.3 : 201, nature TER, entité « article », IsFinished, pas de statut de workflow (Q1), relu en base. */
    @Test
    void l4_creation_201_terEntiteArticleSansStatut_valideeEnBase() throws Exception {
        String code = j.code("LCR");

        UUID lot = creerOk(corpsLot(j, code), code);

        Map<String, Object> l = fixture.ligne(lot);
        assertAll(
                () -> assertThat(guid(l.get("StkKindId"))).isEqualTo(guid(FixtureLotissements.NATURE_TER)),
                () -> assertThat(guid(l.get(STK_HIERARCHY_ID))).isEqualTo(guid(j.tranche())),
                () -> assertThat(fixture.entiteArticle(lot)).as("StrEntityId = entité StkItem").isTrue(),
                () -> assertThat(l).containsEntry("IsFinished", 1).containsEntry("IsUnderItem", 0)
                        .containsEntry("StrStatusId", null)
                        .containsEntry("Description", FixtureLotissements.description(code)),
                () -> assertThat(decimal(l.get("Area"))).isEqualByComparingTo("1000"));
    }

    /** L4 : le lot créé est rendu par la liste existante. */
    @Test
    void l4_creation_lotPresentDansLaListe() throws Exception {
        String code = j.code("LLI");
        UUID lot = creerOk(corpsLot(j, code), code);

        mockMvc.perform(get(LOTISSEMENTS).param("search", j.jeton()).param("size", "200").with(connecte(j.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(lot)).isNotEmpty());
    }

    /** §1.3 : trancheId obligatoire à la création. */
    @Test
    void l1_3_creationSansTranche_400_rienEcrit() throws Exception {
        String code = j.code("LST");

        creer(corpsLot(j, code).sans(TRANCHE_ID)).andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItem WHERE Code = ?", code)).isZero();
    }

    /** L3 : création sur une tranche d'entité inactive ⇒ 400. */
    @Test
    void l3_creationSurTrancheDEntiteInactive_400_rienEcrit() throws Exception {
        UUID inactive = fixture.trancheInactive(j);
        String code = j.code("LTI");

        creer(corpsLot(j, code).avec(TRANCHE_ID, inactive)).andExpect(status().isBadRequest());

        assertThat(fixture.ids(inactive, code)).isEmpty();
    }

    /** Q2 : catégorie et type de nature LOT DE TERRAIN acceptés. */
    @Test
    void q2_categorieEtTypeLotDeTerrain_201() throws Exception {
        String code = j.code("LLT");

        UUID lot = creerOk(corpsLot(j, code).avec(CATEGORIE_ID, j.lotDeTerrain().categorie())
                .avec(TYPE_ID, j.lotDeTerrain().type()), code);

        assertThat(guid(fixture.ligne(lot).get("StkItemCategoryId"))).isEqualTo(guid(j.lotDeTerrain().categorie()));
    }

    // ------------------------------------------------------------------ L5 code unique par tranche (Q3)

    @Test
    void l5_memeCodeDansDeuxTranchesDeLaMemeEntite_201() throws Exception {
        String code = j.code("LDT");
        fixture.lot(j, code, j.autreTranche());

        creerOk(corpsLot(j, code), code);

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItem WHERE Code = ?", code)).isEqualTo(2);
    }

    @Test
    void l5_memeCodeDansLaMemeTranche_409() throws Exception {
        String code = j.code("LDB");
        fixture.lot(j, code);

        creer(corpsLot(j, code)).andExpect(status().isConflict());

        assertThat(fixture.ids(j.tranche(), code)).hasSize(1);
    }

    /** §1.3 : unicité « toutes natures confondues » — le code d'un article bâti de la tranche est pris. */
    @Test
    void l5_codeDUnArticleBatiDeLaTranche_409() throws Exception {
        String code = j.code("LDA");
        fixture.article(j, code);

        creer(corpsLot(j, code)).andExpect(status().isConflict());

        assertThat(fixture.ids(j.tranche(), code)).hasSize(1);
    }

    // ------------------------------------------------------------------ L6 400 nommant le champ

    @Test
    void l6_vocationProjet_400_nommeVocation() throws Exception {
        String code = j.code("LVP");

        creer(corpsLot(j, code).avec(VOCATION_ID, FixtureLotissements.VOCATION_PROJET))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(VOCATION_ID)));

        assertThat(fixture.ids(j.tranche(), code)).isEmpty();
    }

    @Test
    void l6_categorieDeNatureBatiment_400_nommeCategorie() throws Exception {
        String code = j.code("LCB");

        creer(corpsLot(j, code).avec(CATEGORIE_ID, j.bat().categorie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(CATEGORIE_ID)));

        assertThat(fixture.ids(j.tranche(), code)).isEmpty();
    }

    @Test
    void l6_typeDeNatureBatiment_400_nommeType() throws Exception {
        String code = j.code("LTB");

        creer(corpsLot(j, code).avec(TYPE_ID, j.bat().type()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(TYPE_ID)));

        assertThat(fixture.ids(j.tranche(), code)).isEmpty();
    }

    @ParameterizedTest(name = "{0} = 0")
    @ValueSource(strings = {SURFACE_PROVISOIRE, SURFACE_DEFINITIVE})
    void l6_superficieNulle_400_nommeLeChamp(String champ) throws Exception {
        String code = j.code("LS0");

        creer(corpsLot(j, code).avec(champ, BigDecimal.ZERO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(champ)));

        assertThat(fixture.ids(j.tranche(), code)).isEmpty();
    }

    @Test
    void l6_prixHTEtTTCEnsemble_400_nommeLePrix() throws Exception {
        String code = j.code("LPX");

        creer(corpsLot(j, code).avec(PRIX_M2_DEFINITIF_HT, new BigDecimal("500"))
                .avec(PRIX_M2_DEFINITIF_TTC, new BigDecimal("600")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(matchesPattern("(?s).*prixM2Definitif(HT|TTC).*")));

        assertThat(fixture.ids(j.tranche(), code)).isEmpty();
    }

    // ------------------------------------------------------------------ L7 changement de tranche

    @Test
    void l7_changementDeTranche_200_presentationRemiseANull() throws Exception {
        UUID lot = fixture.lot(j, j.code("LCT"));
        UUID presentation = fixture.presentation(j, j.tranche(), "PRES-" + j.jeton());
        fixture.definir(lot, "StkPresentationTypeId", presentation.toString());

        modifier(lot, corps().avec(TRANCHE_ID, j.autreTranche())).andExpect(status().isOk());

        Map<String, Object> l = fixture.ligne(lot);
        assertAll(
                () -> assertThat(guid(l.get(STK_HIERARCHY_ID))).isEqualTo(guid(j.autreTranche())),
                () -> assertThat(l.get("StkPresentationTypeId")).isNull());
    }

    @Test
    void l7_changementDeTranche_lotOccupe_409_trancheInchangee() throws Exception {
        UUID lot = fixture.lot(j, j.code("LCO"));
        fixture.occuper(j, lot);

        modifier(lot, corps().avec(TRANCHE_ID, j.autreTranche())).andExpect(status().isConflict());

        assertThat(guid(fixture.ligne(lot).get(STK_HIERARCHY_ID))).isEqualTo(guid(j.tranche()));
    }

    /** §1.3 : changer de tranche est refusé aussi si le lot a des prospections. */
    @Test
    void l7_changementDeTranche_lotAvecProspection_409_trancheInchangee() throws Exception {
        UUID lot = fixture.lot(j, j.code("LCP"));
        fixture.prospection(j, lot);

        modifier(lot, corps().avec(TRANCHE_ID, j.autreTranche())).andExpect(status().isConflict());

        assertThat(guid(fixture.ligne(lot).get(STK_HIERARCHY_ID))).isEqualTo(guid(j.tranche()));
    }

    // ------------------------------------------------------------------ L8 lot occupé

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PRIX_M2_DEFINITIF_HT, SURFACE_DEFINITIVE, SURFACE_PROVISOIRE, TITRE_FONCIER, CODE,
            CATEGORIE_ID})
    void l8_lotOccupe_champProtege_409_ligneInchangee(String champ) throws Exception {
        String code = j.code("LOC");
        UUID lot = fixture.lot(j, code);
        fixture.occuper(j, lot);

        modifier(lot, corps().avec(champ, valeurProtegee(champ))).andExpect(status().isConflict());

        Map<String, Object> l = fixture.ligne(lot);
        assertAll(
                () -> assertThat(l).containsEntry("Code", code).containsEntry("TitleNumber", null)
                        .containsEntry("ComSaleUnitCostHT", null),
                () -> assertThat(decimal(l.get("Area"))).isEqualByComparingTo("1000"),
                () -> assertThat(decimal(l.get("AreaFloor"))).isEqualByComparingTo("1000"),
                () -> assertThat(guid(l.get("StkItemCategoryId"))).isEqualTo(guid(j.ter().categorie())));
    }

    private Object valeurProtegee(String champ) {
        return switch (champ) {
            case TITRE_FONCIER -> "TF-" + j.jeton();
            case CODE -> j.code("NOUVEAU");
            case CATEGORIE_ID -> j.lotDeTerrain().categorie();
            default -> new BigDecimal("1234");
        };
    }

    @Test
    void l8_lotOccupe_description_200() throws Exception {
        UUID lot = fixture.lot(j, j.code("LOD"));
        fixture.occuper(j, lot);

        modifier(lot, corps().avec(DESCRIPTION, "Lot occupe renomme " + j.jeton())).andExpect(status().isOk());

        assertThat(fixture.ligne(lot)).containsEntry("Description", "Lot occupe renomme " + j.jeton());
    }

    // ------------------------------------------------------------------ PATCH : absent = inchangé, champsAVider

    @Test
    void l1_3_patch_champAbsentInchange_champModifieRelu() throws Exception {
        UUID lot = fixture.lot(j, j.code("LPT"));
        fixture.definir(lot, "Notes", "Notes avant");

        modifier(lot, corps().avec(DESCRIPTION, "Lot renomme " + j.jeton())).andExpect(status().isOk());

        assertThat(fixture.ligne(lot)).containsEntry("Description", "Lot renomme " + j.jeton())
                .containsEntry("Notes", "Notes avant");
    }

    @Test
    void l1_3_patch_champsAVider_videLesNotes() throws Exception {
        UUID lot = fixture.lot(j, j.code("LCV"));
        fixture.definir(lot, "Notes", "Notes avant");

        modifier(lot, corps().avec("champsAVider", List.of(NOTES))).andExpect(status().isOk());

        assertThat((String) fixture.ligne(lot).get("Notes")).isNullOrEmpty();
    }

    // ------------------------------------------------------------------ L9 prix

    /** L9 : prixM2DefinitifHT saisi ⇒ TTC m² et prix HT / TTC par CalculPrixArticle (1 000 m², TVA 0,2). */
    @Test
    void l9_prixM2DefinitifHT_prixCalculesCommeUnArticle() throws Exception {
        String code = j.code("LP9");

        UUID lot = creerOk(corpsLot(j, code).avec(PRIX_M2_DEFINITIF_HT, new BigDecimal("500"))
                .avec(TAUX_TVA, new BigDecimal("0.20")), code);

        Map<String, Object> l = fixture.ligne(lot);
        String json = mockMvc.perform(get(LOTISSEMENT, lot).with(connecte(j.admin())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertAll(
                () -> assertThat(decimal(l.get("ComSaleUnitCostHT"))).isEqualByComparingTo("500"),
                () -> assertThat(decimal(l.get("ComSaleUnitCost"))).isEqualByComparingTo("600"),
                () -> assertThat(decimal(l.get("SalePriceHT"))).isEqualByComparingTo("500000"),
                () -> assertThat(decimal(l.get("SalePrice"))).isEqualByComparingTo("600000"),
                () -> assertThat(new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$." + PRIX_M2_DEFINITIF_TTC))))
                        .isEqualByComparingTo("600"));
    }
}
