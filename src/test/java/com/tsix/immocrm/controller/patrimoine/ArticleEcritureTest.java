package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine.ReferentielArticle;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §5 et §11. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleEcritureTest {

    private static final String CHAMP = "$.champ";
    private static final String TRANCHE_ID = "trancheId";
    private static final String CATEGORIE_ID = "categorieId";
    private static final String IMMEUBLE_ID = "immeubleId";
    private static final String ETAGE_ID = "etageId";
    private static final String SURFACE_PROVISOIRE = "surfaceProvisoire";
    private static final String PRIX_M2_DEFINITIF = "prixM2DefinitifHT";
    private static final String TAUX_TVA = "tauxTva";
    private static final String DESCRIPTION = "description";
    private static final String PRIX_ART344_HT = "10833.333333";

    /**
     * Colonnes non NULL sur la ligne modèle ART344 (PATRIMOINE-base.md §2 et §8), hors colonnes tardives et
     * d'affectation : une colonne NULL ici peut empêcher OMRAN d'ouvrir la fiche.
     */
    private static final List<String> COLONNES_NON_NULLES_OMRAN = List.of(
            "Code", "Description", "Address", "Area", "Notes", "ComSaleUnitCost", "ComSaleMinAdvance", "ComSalePrice",
            "SalePrice", "Contenance", "IsUnderItem", "AreaFloor", "AlwaysAffected", "Anomalies", "Cos", "Cuf",
            "AlinmentStop", "NeighborStop", "MaxHeight", "AmountHT", "CautionNumber", "StkItemTypeId",
            "StkItemCategoryId", "StkVocationId", "StkKindId", "UserCreate", "UserCreateDateTime", "UserUpdate",
            "UserUpdateDateTime", "TitleNumber", "StkFloorId", "StkBuildingId", "StkHierarchyId", "DividedPart",
            "UndividedPart", "Vat", "AreaCos", "AreaCuf", "ComSaleUnitCostFloor", "SalePriceFloor", "ComSalePriceFloor",
            "IsUnderItemOrigin", "DiscountMax", "ComSaleUnitCostFloorHT", "ComSaleUnitCostHT", "SalePriceHT",
            "ComSalePriceHT", "SalePriceFloorHT", "ComSalePriceFloorHT", "IsAvailableForRentInWebSite",
            "IsAvailableForSaleInWebSite", "IsTitled", "InteriorArea", "WallsArea", "CommonPartsArea", "DiscountMax2",
            "StrEntityId", "MapLatitude", "MapLongitude");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private ReferentielArticle ref;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-ART-ECR");
        ref = fixture.referentielArticle("ART-ECR");
    }

    private Map<String, Object> corps(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(TRANCHE_ID, ref.tranche().getId());
        corps.put("code", code);
        corps.put(DESCRIPTION, "Lot " + code);
        corps.put(CATEGORIE_ID, ref.categorie().getId());
        corps.put("typeId", ref.type().getId());
        corps.put(IMMEUBLE_ID, ref.immeuble().getId());
        corps.put(ETAGE_ID, ref.etage().getId());
        return corps;
    }

    private ResultActions creer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post("/api/articles").with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private ResultActions modifier(UUID id, Map<String, Object> corps) throws Exception {
        return mockMvc.perform(patch("/api/articles/{id}", id).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private UUID creerId(Map<String, Object> corps) throws Exception {
        String reponse = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        em.clear();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private Map<String, Object> corpsArt344(String code) {
        Map<String, Object> corps = corps(code);
        corps.put(SURFACE_PROVISOIRE, 63);
        corps.put("surfaceDefinitive", 63);
        corps.put("prixM2ProvisoireHT", new BigDecimal(PRIX_ART344_HT));
        corps.put(PRIX_M2_DEFINITIF, new BigDecimal(PRIX_ART344_HT));
        corps.put(TAUX_TVA, new BigDecimal("0.20"));
        return corps;
    }

    @Test
    void creation_201_prixExactsDuCasArt344() throws Exception {
        creer(corpsArt344("ART-ECR-344"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixM2DefinitifTTC").value(13000))
                .andExpect(jsonPath("$.prixM2ProvisoireTTC").value(13000))
                .andExpect(jsonPath("$.prixArticleDefinitifTTC").value(819000))
                .andExpect(jsonPath("$.prixArticleProvisoireTTC").value(819000))
                .andExpect(jsonPath("$.comSalePrice").value(819000))
                .andExpect(jsonPath("$.comSalePriceFloor").value(819000))
                .andExpect(jsonPath("$.estSousArticle").value(false))
                .andExpect(jsonPath("$.occupe").value(false))
                .andExpect(jsonPath("$.creePar").value("Compte ADM-ART-ECR"));
    }

    @Test
    void creation_colonnesPoseesParLeServeur() throws Exception {
        StkItem item = itemRepository.findById(creerId(corps("ART-ECR-SRV"))).orElseThrow();

        assertAll(
                () -> assertThat(item.getStrEntity().getId()).isEqualTo(FixturePatrimoine.STR_ENTITY_ARTICLE),
                () -> assertThat(item.getStkKind().getId()).isEqualTo(FixturePatrimoine.NATURE_BATIMENT),
                () -> assertThat(item.getStkVocation().getCode()).isEqualTo("VENTE"),
                () -> assertThat(item.getIsTitled()).isTrue(),
                () -> assertThat(item.getIsUnderItem()).isFalse(),
                () -> assertThat(item.getIsUnderItemOrigin()).isFalse(),
                () -> assertThat(item.getAlwaysAffected()).isFalse(),
                () -> assertThat(item.getIsAvailableForSaleInWebSite()).isFalse(),
                () -> assertThat(item.getIsAvailableForRentInWebSite()).isFalse(),
                () -> assertThat(item.getAreaFloor()).isEqualByComparingTo("1"),
                () -> assertThat(item.getArea()).isEqualByComparingTo("1"),
                () -> assertThat(item.getComFolderId()).isNull(),
                () -> assertThat(item.getCfgProject()).isNull(),
                () -> assertThat(item.getParent()).isNull());
    }

    @Test
    void colonnesNonNullesCommeOmran() throws Exception {
        UUID id = creerId(corps("ART-ECR-NN"));

        Object[] ligne = (Object[]) em.createNativeQuery("SELECT " + String.join(", ", COLONNES_NON_NULLES_OMRAN)
                + " FROM StkItem WHERE PKey = :id").setParameter("id", id).getSingleResult();
        List<String> nulles = new ArrayList<>();
        for (int i = 0; i < ligne.length; i++) {
            if (ligne[i] == null) {
                nulles.add(COLONNES_NON_NULLES_OMRAN.get(i));
            }
        }
        assertThat(nulles).as("colonnes NULL alors qu'OMRAN y écrit une valeur").isEmpty();
    }

    @Test
    void creation_400_surfaceNulle() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-S0");
        corps.put(SURFACE_PROVISOIRE, 0);
        creer(corps).andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(SURFACE_PROVISOIRE))
                .andExpect(jsonPath("$.detail").value("La valeur de la supérficie provisoire doit être strictement supérieure à 0 !"));
    }

    @Test
    void creation_400_immeubleManquant() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-IMM");
        corps.remove(IMMEUBLE_ID);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(IMMEUBLE_ID));
    }

    @Test
    void creation_400_etageManquant() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-ETG");
        corps.remove(ETAGE_ID);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(ETAGE_ID));
    }

    @Test
    void creation_400_categoriePourSousArticles() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-CATS");
        corps.put(CATEGORIE_ID, ref.categorieSous().getId());
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(CATEGORIE_ID));
    }

    @Test
    void creation_400_categorieDUneAutreNature() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-CATT");
        corps.put(CATEGORIE_ID, fixture.categorie("CAT-TER-ECR",
                fixture.nature(UUID.randomUUID(), "TER-ECR"), false).getId());
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(CATEGORIE_ID));
    }

    @Test
    void creation_400_vocationProjet() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-PRJ");
        corps.put("vocationId", fixture.vocation("PROJET").getId());
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value("vocationId"));
    }

    @Test
    void creation_400_tvaAuDelaDe100() throws Exception {
        Map<String, Object> corps = corps("ART-ECR-TVA");
        corps.put(TAUX_TVA, new BigDecimal("1.5"));
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(TAUX_TVA));
    }

    @Test
    void creation_409_memeCodeMemeTranche_casseDifferente() throws Exception {
        creerId(corps("ART-ECR-DUP"));
        creer(corps("art-ecr-dup")).andExpect(status().isConflict()).andExpect(jsonPath(CHAMP).value("code"));
    }

    @Test
    void creation_201_memeCodeDansUneAutreTranche() throws Exception {
        creerId(corps("ART-ECR-AUTRE"));
        Map<String, Object> corps = corps("ART-ECR-AUTRE");
        corps.put(TRANCHE_ID, fixture.referentielArticle("ART-ECR2").tranche().getId());
        creer(corps).andExpect(status().isCreated());
    }

    @Test
    void modification_400_changementDeTranche() throws Exception {
        UUID id = creerId(corps("ART-ECR-MOVE"));
        modifier(id, Map.of(TRANCHE_ID, fixture.referentielArticle("ART-ECR3").tranche().getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(TRANCHE_ID));
    }

    @Test
    void modification_404_idInconnu() throws Exception {
        modifier(UUID.randomUUID(), Map.of(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    @Test
    void modification_prixRecalcules() throws Exception {
        UUID id = creerId(corps("ART-ECR-PRIX"));
        modifier(id, Map.of("surfaceDefinitive", 50, PRIX_M2_DEFINITIF, 10000, TAUX_TVA, 0.2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixArticleDefinitifHT").value(500000))
                .andExpect(jsonPath("$.comSalePrice").value(600000))
                .andExpect(jsonPath("$.modifiePar").value("Compte ADM-ART-ECR"));
    }

    private UUID articleOccupe(String code) throws Exception {
        UUID id = creerId(corps(code));
        ComFolder dossier = fixture.dossier("26/ECR-" + code, ref.vente());
        StkItem item = itemRepository.findById(id).orElseThrow();
        item.setComFolderId(dossier.getId());
        itemRepository.save(item);
        em.flush();
        em.clear();
        return id;
    }

    @Test
    void articleOccupe_409_surLePrix() throws Exception {
        UUID id = articleOccupe("ART-ECR-OCC1");
        modifier(id, Map.of(PRIX_M2_DEFINITIF, 12000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(CHAMP).value(PRIX_M2_DEFINITIF))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("26/ECR-ART-ECR-OCC1")));
    }

    @Test
    void articleOccupe_200_surLaDescription_etComFolderIdConserve() throws Exception {
        UUID id = articleOccupe("ART-ECR-OCC2");
        UUID dossierAvant = itemRepository.findById(id).orElseThrow().getComFolderId();

        modifier(id, Map.of(DESCRIPTION, "Nouvelle description", "notes", "Vue mer", PRIX_M2_DEFINITIF, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nouvelle description"))
                .andExpect(jsonPath("$.occupe").value(true));

        em.flush();
        em.clear();
        assertThat(itemRepository.findById(id).orElseThrow().getComFolderId()).isNotNull().isEqualTo(dossierAvant);
    }
}
