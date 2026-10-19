package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue de FEATURE-patrimoine-ecriture.md : un changement de surface garde les deux prix au m² stockés, comme le
 * setter OMRAN {@code Yes.Stk/Item.cs:Area} (SalePrice = ComSaleUnitCost × Area, SalePriceHT = ComSaleUnitCostHT × Area).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineRevueTest {

    private static final String SURFACE_DEFINITIVE = "surfaceDefinitive";
    private static final String SALE_PRICE = "SalePrice";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository stkItemRepository;

    private AdmUser admin;
    private CfgTranche tranche;
    private StkBuilding immeuble;
    private StkFloor etage;
    private StkItemCategory categorie;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        CfgCompany entite = fixture.entite("ENT-REVUE", true);
        CfgProject projet = fixture.projet("PRJ-REVUE", entite);
        tranche = fixture.tranche("TR-REVUE", projet);
        immeuble = fixture.immeuble("IMM-REVUE");
        etage = fixture.etage("ETG-REVUE");
        categorie = fixture.categorie("CAT-REVUE", fixture.natureBat(), false);
        type = fixture.type("TYP-REVUE", fixture.natureBat(), false);
    }

    /** 63 m², saisie TTC 13 000 / m², TVA 0,20 → 819 000 TTC (cas §5.5). */
    private CorpsJson corpsArticle(String code) {
        return corps().avec("trancheId", tranche.getId()).avec("code", code).avec("description", "Article " + code)
                .avec("vocationId", VOCATION_VENTE).avec("categorieId", categorie.getId()).avec("typeId", type.getId())
                .avec("immeubleId", immeuble.getId()).avec("etageId", etage.getId())
                .avec("surfaceProvisoire", new BigDecimal("63")).avec(SURFACE_DEFINITIVE, new BigDecimal("63"))
                .avec("prixM2ProvisoireTTC", new BigDecimal("13000")).avec("prixM2DefinitifTTC", new BigDecimal("13000"))
                .avec("tauxTva", new BigDecimal("0.20"));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = mockMvc.perform(post("/api/articles").with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps.json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch("/api/articles/{id}", id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    /** OMRAN Area : SalePrice = 13 000 × 69 = 897 000 ; le TTC au m² saisi n'est pas reconstruit depuis le HT arrondi. */
    @Test
    void patchSurface_articleSaisiEnTTC_garde13000AuM2_prix897000() throws Exception {
        UUID id = creerOk(corpsArticle("ART-REVUE-SURF"));

        modifier(id, corps().avec(SURFACE_DEFINITIVE, new BigDecimal("69"))).andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("13000"),
                () -> assertThat(decimal(a.get(SALE_PRICE))).isEqualByComparingTo("897000"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("897000"));
    }

    /**
     * OMRAN {@code Vat} (Item.cs:1351-1369) : le HT au m² reste, seuls le TTC au m² et SalePrice(Floor) suivent ;
     * SalePriceHT n'est pas réécrit. Saisi en TTC sur 69 m², il vaut 747 500 et doit le rester.
     */
    @Test
    void patchTva_articleSaisiEnTTC_prixHTInchange() throws Exception {
        UUID id = creerOk(corpsArticle("ART-REVUE-TVA")
                .avec("surfaceProvisoire", new BigDecimal("69")).avec(SURFACE_DEFINITIVE, new BigDecimal("69")));

        modifier(id, corps().avec("tauxTva", new BigDecimal("0.10"))).andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("747500"),
                () -> assertThat(decimal(a.get("SalePriceFloorHT"))).isEqualByComparingTo("747500"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostHT"))).isEqualByComparingTo("10833.333333"));
    }

    /** §5.4.4 : renvoyer la surface actuelle d'un article occupé (200, champ inchangé) ne doit pas toucher à ses prix. */
    @Test
    void patchSurfaceInchangee_articleOccupe_prixInchanges() throws Exception {
        UUID id = creerOk(corpsArticle("ART-REVUE-OCC"));
        ComFolder dossier = fixture.dossier("DOS-ART-REVUE-OCC");
        StkItem article = stkItemRepository.findById(id).orElseThrow();
        article.setComFolderId(dossier.getId());
        stkItemRepository.saveAndFlush(article);

        modifier(id, corps().avec(SURFACE_DEFINITIVE, new BigDecimal("63")).avec("description", "Autre"))
                .andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(decimal(a.get(SALE_PRICE))).isEqualByComparingTo("819000"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("819000"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("682500"));
    }
}
