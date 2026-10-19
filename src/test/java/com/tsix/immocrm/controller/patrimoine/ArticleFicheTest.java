package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import jakarta.persistence.Tuple;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ChampsAViderJson.avecChampsAVider;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureArticleFiche.CATEGORIE_CHARGE;
import static com.tsix.immocrm.fixtures.FixtureArticleFiche.FACADE;
import static com.tsix.immocrm.fixtures.FixtureArticleFiche.ORIENTATION;
import static com.tsix.immocrm.fixtures.FixtureArticleFiche.VUE;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fiche article complète — FEATURE-patrimoine-article-fiche.md §1, §2, §9.1, §9.6, §9.7, §9.9 (ArticleFicheTest).
 * Noms de fil des champs foncier / compteurs / contenances non donnés par le spec : choisis ici (rapport du test-writer),
 * compteurs alignés sur {@code CompteursDto} du dossier. Les référentiels du lot sont créés test par test : un test qui
 * n'en a pas besoin ne dépend pas des tables 110.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleFicheTest {

    private static final String ARTICLES = "/api/articles";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String CONTENANCES = "/api/articles/{id}/contenances";
    private static final String SOUS_ARTICLES = "/api/articles/{id}/sous-articles";
    private static final String PRESENTATIONS = "/api/tranches/{id}/presentations";

    private static final String PRESENTATION_ID = "presentationId";
    private static final String ORIENTATION_ID = "orientationId";
    private static final String CATEGORIE_CHARGE_ID = "categorieChargeId";
    private static final String FACADE_ID = "facadeId";
    private static final String ENTITE_SYNDIC_ID = "entiteSyndicId";
    private static final String VUE_IDS = "vueIds";
    private static final String AVANCE_MINIMUM = "avanceMinimum";
    private static final String LOYER = "loyerMensuelHT";
    private static final String CAUTIONS = "nombreCautions";
    private static final String ANOMALIES = "anomalies";
    private static final String CONTENANCE_LIBRE = "contenanceLibre";
    private static final String VENTE_WEB = "disponibleVenteWeb";
    private static final String LOCATION_WEB = "disponibleLocationWeb";
    private static final String DATE_ENREGISTREMENT = "dateEnregistrement";
    private static final String DATE_TITRE = "dateTitreFoncier";
    private static final String TITRE_MERE = "titreFoncierMere";
    private static final String COMPTEUR_EAU = "compteurEau";
    private static final String CONTENANCE_ID = "contenanceId";
    private static final String NOMBRE = "nombre";
    private static final String AVEC_OPTION_1 = "avecOption1";
    private static final String QUANTITE_OPTION_1 = "quantiteOption1";
    private static final String ESTTITRE = "estTitre";
    private static final String SURFACE_PROVISOIRE = "surfaceProvisoire";
    private static final String SURFACE_DEFINITIVE = "surfaceDefinitive";
    private static final String JOUR = "2026-01-15";
    private static final String MINUIT = "T00:00:00";
    private static final String LIGNES_CONTENANCES = """
            SELECT CONVERT(varchar(36), StkContenanceId) AS contenance, ItemsNumber AS nombre, Option1 AS option1,
                   IsWithOption1 AS avec1, QuantityOption1 AS quantite1
            FROM StkItemContenance WHERE StkItemId = :id""";
    private static final String COMPTE_CONTENANCES = "SELECT COUNT(*) FROM StkItemContenance WHERE StkItemId = :id";
    private static final String VUES_DE_L_ARTICLE = "SELECT StkVueId FROM StkItemVue WHERE StkItemId = :id";
    private static final String DETAIL_NETTE = "$.superficieNette";
    private static final String DETAIL_TITREE = "$.superficieTitree";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureArticleFiche fiche;
    @Autowired private FixtureTrancheOnglets onglets;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkBuilding immeuble;
    private StkFloor etage;
    private StkItemCategory categorie;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        jeton = "P2A" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("ENT-" + jeton, true);
        projet = fixture.projet("PRJ-" + jeton, entite);
        tranche = fixture.tranche("TR-" + jeton, projet);
        immeuble = fixture.immeuble("IMM-" + jeton);
        etage = fixture.etage("ETG-" + jeton);
        categorie = fixture.categorie("CAT-" + jeton, fixture.natureBat(), false);
        type = fixture.type("TYP-" + jeton, fixture.natureBat(), false);
    }

    // ------------------------------------------------------------------ outils

    private String code(String suffixe) {
        return suffixe + "-" + jeton;
    }

    private CorpsJson corpsArticle(String code) {
        return corps().avec("trancheId", tranche.getId()).avec("code", code).avec("description", "Article " + code)
                .avec("categorieId", categorie.getId()).avec("typeId", type.getId())
                .avec("immeubleId", immeuble.getId()).avec("etageId", etage.getId())
                .avec(SURFACE_PROVISOIRE, new BigDecimal("63")).avec(SURFACE_DEFINITIVE, new BigDecimal("63"))
                .avec("prixM2ProvisoireTTC", new BigDecimal("13000")).avec("prixM2DefinitifTTC", new BigDecimal("13000"))
                .avec("tauxTva", new BigDecimal("0.20"));
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ARTICLES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions modifier(UUID id, String json) throws Exception {
        return mockMvc.perform(patch(ARTICLE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions detail(UUID id) throws Exception {
        return mockMvc.perform(get(ARTICLE, id).with(connecte(admin)));
    }

    private ResultActions enregistrerContenances(UUID id, String json) throws Exception {
        return mockMvc.perform(put(CONTENANCES, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private UUID creerSousArticle(UUID parentId, String code, String surface, boolean titre) throws Exception {
        StkItemCategory categorieSous = fixture.categorie("CATS-" + code, fixture.natureBat(), true);
        StkItemType typeSous = fixture.type("TYPS-" + code, fixture.natureBat(), true);
        String json = mockMvc.perform(post(SOUS_ARTICLES, parentId).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("code", code).avec("description", "Annexe " + code)
                                .avec("categorieId", categorieSous.getId()).avec("typeId", typeSous.getId())
                                .avec("etageId", etage.getId())
                                .avec(SURFACE_PROVISOIRE, new BigDecimal(surface))
                                .avec(SURFACE_DEFINITIVE, new BigDecimal(surface))
                                .avec("prixM2ProvisoireHT", new BigDecimal("1000"))
                                .avec("prixM2DefinitifHT", new BigDecimal("1000"))
                                .avec("tauxTva", BigDecimal.ZERO).avec(ESTTITRE, titre).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static String ligneContenance(UUID contenanceId) {
        return "$.lignes[?(@.contenanceId=='%s')]".formatted(contenanceId);
    }

    private static String parId(UUID id) {
        return "$[?(@.id=='%s')]".formatted(id);
    }

    private static String chaine(int longueur) {
        return "x".repeat(longueur);
    }

    private static String jour(Object valeur) {
        return valeur == null ? null : valeur.toString().substring(0, 10);
    }

    // ------------------------------------------------------------------ §1 champs simples en écriture

    @Test
    void r1_creation_montantsTextesEtCompteursDansLeursColonnes() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle(code("SIMPLE"))
                .avec(AVANCE_MINIMUM, new BigDecimal("50000")).avec(LOYER, new BigDecimal("4500.5"))
                .avec(CAUTIONS, 2).avec(ANOMALIES, "Fissure plafond").avec(CONTENANCE_LIBRE, "3 chambres, 1 salon")
                .avec("referenceElectricite", "RE-1").avec("policeElectricite", "PE-1")
                .avec("compteurElectricite", "CE-1").avec("releveElectricite", "IE-1")
                .avec("referenceEau", "RW-1").avec("policeEau", "PW-1").avec(COMPTEUR_EAU, "CW-1").avec("releveEau", "IW-1")
                .avec("referenceGaz", "RG-1").avec("policeGaz", "PG-1").avec("compteurGaz", "CG-1")
                .avec("releveGaz", "IG-1")));

        assertAll(
                () -> assertThat(decimal(a.get("ComSaleMinAdvance"))).isEqualByComparingTo("50000"),
                () -> assertThat(decimal(a.get("AmountHT"))).isEqualByComparingTo("4500.5"),
                () -> assertThat(((Number) a.get("CautionNumber")).intValue()).isEqualTo(2),
                () -> assertThat(a.get("Anomalies")).isEqualTo("Fissure plafond"),
                () -> assertThat(a.get("Contenance")).isEqualTo("3 chambres, 1 salon"),
                () -> assertThat(a.get("ElectricityRef")).isEqualTo("RE-1"),
                () -> assertThat(a.get("ElectricityMeterPolice")).isEqualTo("PE-1"),
                () -> assertThat(a.get("ElectricityMeter")).isEqualTo("CE-1"),
                () -> assertThat(a.get("ElectricityMeterIndex")).isEqualTo("IE-1"),
                () -> assertThat(a.get("WaterRef")).isEqualTo("RW-1"),
                () -> assertThat(a.get("WaterMeterPolice")).isEqualTo("PW-1"),
                () -> assertThat(a.get("WaterMeter")).isEqualTo("CW-1"),
                () -> assertThat(a.get("WaterMeterIndex")).isEqualTo("IW-1"),
                () -> assertThat(a.get("GasRef")).isEqualTo("RG-1"),
                () -> assertThat(a.get("GasMeterPolice")).isEqualTo("PG-1"),
                () -> assertThat(a.get("GasMeter")).isEqualTo("CG-1"),
                () -> assertThat(a.get("GasMeterIndex")).isEqualTo("IG-1"));
    }

    private CorpsJson avecFoncier(CorpsJson corps) {
        return corps.avec(ESTTITRE, true).avec("titreFoncier", "TF-100")
                .avec(DATE_ENREGISTREMENT, JOUR + MINUIT).avec("numeroEnregistrement", "ENR-7")
                .avec("lieuEnregistrement", "Marrakech").avec("cpfImmatriculation", "CPF-9")
                .avec("dateImmatriculation", "2026-02-10" + MINUIT).avec(TITRE_MERE, "TF-MERE-1")
                .avec("intituleTitreFoncier", "Villa Atlas").avec(DATE_TITRE, "2026-03-20" + MINUIT)
                .avec("proprieteDite", "Dar Salam").avec("fractionDivise", "F-12")
                .avec("fractionDiviseTerrain", "FT-3").avec("partiesDivises", new BigDecimal("40"))
                .avec("partiesIndivises", new BigDecimal("9")).avec("quotaTerrainGroupe", "Q-5")
                .avec("notesTitreFoncier", "Notes TF");
    }

    @Test
    void r1_creation_foncierDansSesColonnes() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(avecFoncier(corpsArticle(code("FONCIER")))));

        assertAll(
                () -> assertThat(a.get("IsTitled")).isEqualTo(true),
                () -> assertThat(a.get("TitleNumber")).isEqualTo("TF-100"),
                () -> assertThat(jour(a.get("RegistryDate"))).isEqualTo(JOUR),
                () -> assertThat(a.get("RegistryNumber")).isEqualTo("ENR-7"),
                () -> assertThat(a.get("RegistryPlace")).isEqualTo("Marrakech"),
                () -> assertThat(a.get("ImmatriculationCPF")).isEqualTo("CPF-9"),
                () -> assertThat(jour(a.get("ImmatriculationDate"))).isEqualTo("2026-02-10"),
                () -> assertThat(a.get("TitleNumberMother")).isEqualTo("TF-MERE-1"),
                () -> assertThat(a.get("TitleName")).isEqualTo("Villa Atlas"),
                () -> assertThat(jour(a.get("TitleDate"))).isEqualTo("2026-03-20"),
                () -> assertThat(a.get("PropertyCalled")).isEqualTo("Dar Salam"),
                () -> assertThat(a.get("FractionDivided")).isEqualTo("F-12"),
                () -> assertThat(a.get("FractionDividedLand")).isEqualTo("FT-3"),
                () -> assertThat(decimal(a.get("DividedPart"))).isEqualByComparingTo("40"),
                () -> assertThat(decimal(a.get("UndividedPart"))).isEqualByComparingTo("9"),
                () -> assertThat(a.get("GroupLandQuota")).isEqualTo("Q-5"),
                () -> assertThat(a.get("TitleNotes")).isEqualTo("Notes TF"));
    }

    /** §9.6 : les colonnes disent « disponible », le DTO aussi. */
    @Test
    void r9_6_casesWeb_ecritesDansLesColonnesDisponible() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle(code("WEB")).avec(VENTE_WEB, true).avec(LOCATION_WEB, false)));

        assertAll(
                () -> assertThat(a.get("IsAvailableForSaleInWebSite")).isEqualTo(true),
                () -> assertThat(a.get("IsAvailableForRentInWebSite")).isEqualTo(false));
    }

    @Test
    void r1_patch_champsSimplesModifies() throws Exception {
        UUID id = creerOk(avecFoncier(corpsArticle(code("PATCH"))));

        modifier(id, corps().avec(AVANCE_MINIMUM, new BigDecimal("75000")).avec(TITRE_MERE, "TF-MERE-2")
                .avec("compteurGaz", "CG-2").avec(CAUTIONS, 3).avec(LOCATION_WEB, true).json())
                .andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(decimal(a.get("ComSaleMinAdvance"))).isEqualByComparingTo("75000"),
                () -> assertThat(a.get("TitleNumberMother")).isEqualTo("TF-MERE-2"),
                () -> assertThat(a.get("GasMeter")).isEqualTo("CG-2"),
                () -> assertThat(((Number) a.get("CautionNumber")).intValue()).isEqualTo(3),
                () -> assertThat(a.get("IsAvailableForRentInWebSite")).isEqualTo(true),
                () -> assertThat(a.get("RegistryNumber")).as("champ absent inchangé").isEqualTo("ENR-7"));
    }

    // ------------------------------------------------------------------ §1 champs simples en lecture

    @Test
    void r1_detail_exposeLesChampsSimples() throws Exception {
        UUID id = creerOk(avecFoncier(corpsArticle(code("LECTURE")))
                .avec(AVANCE_MINIMUM, new BigDecimal("50000")).avec(LOYER, new BigDecimal("4500"))
                .avec(CAUTIONS, 2).avec(ANOMALIES, "Fissure").avec(CONTENANCE_LIBRE, "Duplex")
                .avec(COMPTEUR_EAU, "CW-9").avec("releveElectricite", "IE-9").avec(VENTE_WEB, true)
                .avec(LOCATION_WEB, false));

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$." + AVANCE_MINIMUM).value(50000))
                .andExpect(jsonPath("$." + LOYER).value(4500))
                .andExpect(jsonPath("$." + CAUTIONS).value(2))
                .andExpect(jsonPath("$." + ANOMALIES).value("Fissure"))
                .andExpect(jsonPath("$." + CONTENANCE_LIBRE).value("Duplex"))
                .andExpect(jsonPath("$." + COMPTEUR_EAU).value("CW-9"))
                .andExpect(jsonPath("$.releveElectricite").value("IE-9"))
                .andExpect(jsonPath("$." + VENTE_WEB).value(true))
                .andExpect(jsonPath("$." + LOCATION_WEB).value(false))
                .andExpect(jsonPath("$." + DATE_ENREGISTREMENT).value(containsString(JOUR)))
                .andExpect(jsonPath("$.numeroEnregistrement").value("ENR-7"))
                .andExpect(jsonPath("$.lieuEnregistrement").value("Marrakech"))
                .andExpect(jsonPath("$.cpfImmatriculation").value("CPF-9"))
                .andExpect(jsonPath("$.dateImmatriculation").value(containsString("2026-02-10")))
                .andExpect(jsonPath("$." + TITRE_MERE).value("TF-MERE-1"))
                .andExpect(jsonPath("$.intituleTitreFoncier").value("Villa Atlas"))
                .andExpect(jsonPath("$." + DATE_TITRE).value(containsString("2026-03-20")))
                .andExpect(jsonPath("$.proprieteDite").value("Dar Salam"))
                .andExpect(jsonPath("$.fractionDivise").value("F-12"))
                .andExpect(jsonPath("$.fractionDiviseTerrain").value("FT-3"))
                .andExpect(jsonPath("$.partiesDivises").value(40))
                .andExpect(jsonPath("$.partiesIndivises").value(9))
                .andExpect(jsonPath("$.quotaTerrainGroupe").value("Q-5"))
                .andExpect(jsonPath("$.notesTitreFoncier").value("Notes TF"));
    }

    // ------------------------------------------------------------------ §1 refus 400

    @Test
    void r1_nombreCautionsNegatif_400() throws Exception {
        creer(corpsArticle(code("CAUT-NEG")).avec(CAUTIONS, -1)).andExpect(status().isBadRequest());
    }

    /** §1 : compteurs nvarchar(50). */
    @Test
    void r1_compteurDePlusDe50Caracteres_400() throws Exception {
        creer(corpsArticle(code("CPT-LONG")).avec(COMPTEUR_EAU, chaine(51))).andExpect(status().isBadRequest());
    }

    /** §1 : Anomalies nvarchar(1000). */
    @Test
    void r1_anomaliesDePlusDe1000Caracteres_400() throws Exception {
        creer(corpsArticle(code("ANO-LONG")).avec(ANOMALIES, chaine(1001))).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §1 références (orientation, charge, façade, syndic, présentation)

    @Test
    void r1_creation_referencesDansLeursColonnes() throws Exception {
        UUID orientation = fiche.reference(ORIENTATION, code("ORI"));
        UUID charge = fiche.reference(CATEGORIE_CHARGE, code("CHG"));
        UUID facade = fiche.reference(FACADE, code("FAC"));
        CfgCompany syndic = fixture.entite(code("SYN"), true);
        fiche.habilitationEntite(admin.getId(), syndic.getId());

        Tuple a = fixture.ligneArticle(creerOk(corpsArticle(code("REFS")).avec(ORIENTATION_ID, orientation)
                .avec(CATEGORIE_CHARGE_ID, charge).avec(FACADE_ID, facade).avec(ENTITE_SYNDIC_ID, syndic.getId())));

        assertAll(
                () -> assertThat(guid(a.get("StkOrientationId"))).isEqualTo(guid(orientation)),
                () -> assertThat(guid(a.get("StkCostCategoryId"))).isEqualTo(guid(charge)),
                () -> assertThat(guid(a.get("StkFacadeId"))).isEqualTo(guid(facade)),
                () -> assertThat(guid(a.get("CfgCompanySyndicId"))).isEqualTo(guid(syndic.getId())));
    }

    @Test
    void r1_detail_exposeLesReferences() throws Exception {
        UUID orientation = fiche.reference(ORIENTATION, code("ORI-L"));
        UUID charge = fiche.reference(CATEGORIE_CHARGE, code("CHG-L"));
        UUID facade = fiche.reference(FACADE, code("FAC-L"));
        CfgCompany syndic = fixture.entite(code("SYN-L"), true);
        fiche.habilitationEntite(admin.getId(), syndic.getId());
        UUID id = creerOk(corpsArticle(code("REFS-L")).avec(ORIENTATION_ID, orientation)
                .avec(CATEGORIE_CHARGE_ID, charge).avec(FACADE_ID, facade).avec(ENTITE_SYNDIC_ID, syndic.getId()));

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$." + ORIENTATION_ID).value(orientation.toString()))
                .andExpect(jsonPath("$." + CATEGORIE_CHARGE_ID).value(charge.toString()))
                .andExpect(jsonPath("$." + FACADE_ID).value(facade.toString()))
                .andExpect(jsonPath("$." + ENTITE_SYNDIC_ID).value(syndic.getId().toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {ORIENTATION_ID, CATEGORIE_CHARGE_ID, FACADE_ID, ENTITE_SYNDIC_ID, PRESENTATION_ID})
    void r1_referenceInconnue_400(String champ) throws Exception {
        creer(corpsArticle(code("REF-INC")).avec(champ, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test
    void r1_patch_presentationDeLaTranche_ecriteEtRelue() throws Exception {
        UUID id = creerOk(corpsArticle(code("PRES")));
        UUID presentation = fiche.presentation(code("PT"), tranche.getId());

        modifier(id, corps().avec(PRESENTATION_ID, presentation).json()).andExpect(status().isOk());

        assertThat(guid(fixture.ligneArticle(id).get("StkPresentationTypeId"))).isEqualTo(guid(presentation));
        detail(id).andExpect(jsonPath("$.presentationTypeId").value(presentation.toString()));
    }

    // ------------------------------------------------------------------ §1 vues (remplacement de l'ensemble)

    @Test
    void r1_vues_creation_uneLigneParVue() throws Exception {
        UUID mer = fiche.reference(VUE, code("MER"));
        UUID jardin = fiche.reference(VUE, code("JAR"));

        UUID id = creerOk(corpsArticle(code("VUES")).avec(VUE_IDS, List.of(mer, jardin)));

        assertThat(fiche.guids(VUES_DE_L_ARTICLE, id)).containsExactlyInAnyOrder(guid(mer), guid(jardin));
    }

    @Test
    void r1_vues_patch_remplacentLEnsemble() throws Exception {
        UUID mer = fiche.reference(VUE, code("MER-R"));
        UUID jardin = fiche.reference(VUE, code("JAR-R"));
        UUID piscine = fiche.reference(VUE, code("PIS-R"));
        UUID id = creerOk(corpsArticle(code("VUES-R")).avec(VUE_IDS, List.of(mer, jardin)));

        modifier(id, corps().avec(VUE_IDS, List.of(jardin, piscine)).json()).andExpect(status().isOk());

        assertThat(fiche.guids(VUES_DE_L_ARTICLE, id)).containsExactlyInAnyOrder(guid(jardin), guid(piscine));
    }

    /** §1 : OMRAN ajoute des doublons à chaque enregistrement (BuildItem.cs:705-715) ; bogue non reproduit. */
    @Test
    void r1_vues_memeEnsembleRenvoye_aucunDoublon() throws Exception {
        UUID mer = fiche.reference(VUE, code("MER-D"));
        UUID id = creerOk(corpsArticle(code("VUES-D")).avec(VUE_IDS, List.of(mer)));

        modifier(id, corps().avec(VUE_IDS, List.of(mer)).json()).andExpect(status().isOk());
        modifier(id, corps().avec(VUE_IDS, List.of(mer)).json()).andExpect(status().isOk());

        assertThat(fiche.guids(VUES_DE_L_ARTICLE, id)).containsExactly(guid(mer));
    }

    @Test
    void r1_vues_absentesDuPatch_inchangees() throws Exception {
        UUID mer = fiche.reference(VUE, code("MER-A"));
        UUID id = creerOk(corpsArticle(code("VUES-A")).avec(VUE_IDS, List.of(mer)));

        modifier(id, corps().avec("description", "Autre").json()).andExpect(status().isOk());

        assertThat(fiche.guids(VUES_DE_L_ARTICLE, id)).containsExactly(guid(mer));
    }

    @Test
    void r1_vues_listeVide_toutesRetirees() throws Exception {
        UUID mer = fiche.reference(VUE, code("MER-V"));
        UUID id = creerOk(corpsArticle(code("VUES-V")).avec(VUE_IDS, List.of(mer)));

        modifier(id, corps().avec(VUE_IDS, List.of()).json()).andExpect(status().isOk());

        assertThat(fiche.guids(VUES_DE_L_ARTICLE, id)).isEmpty();
    }

    @Test
    void r1_vues_detail_exposeVueIds() throws Exception {
        UUID mer = fiche.reference(VUE, code("MER-L"));
        UUID id = creerOk(corpsArticle(code("VUES-L")).avec(VUE_IDS, List.of(mer)));

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$." + VUE_IDS, hasSize(1)))
                .andExpect(jsonPath("$." + VUE_IDS + "[0]").value(mer.toString()));
    }

    @Test
    void r1_vueInconnue_400() throws Exception {
        creer(corpsArticle(code("VUE-INC")).avec(VUE_IDS, List.of(UUID.randomUUID()))).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §9.7 champsAVider

    @Test
    void r9_7_champsAVider_videDatesEtEntiteSyndic() throws Exception {
        CfgCompany syndic = fixture.entite(code("SYN-V"), true);
        fiche.habilitationEntite(admin.getId(), syndic.getId());
        UUID id = creerOk(avecFoncier(corpsArticle(code("VIDER"))).avec(ENTITE_SYNDIC_ID, syndic.getId()));

        modifier(id, avecChampsAVider(corps(), DATE_ENREGISTREMENT, DATE_TITRE, ENTITE_SYNDIC_ID))
                .andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(a.get("RegistryDate")).isNull(),
                () -> assertThat(a.get("TitleDate")).isNull(),
                () -> assertThat(a.get("CfgCompanySyndicId")).isNull(),
                () -> assertThat(jour(a.get("ImmatriculationDate"))).as("date non citée gardée").isEqualTo("2026-02-10"));
    }

    @Test
    void r9_7_champsAVider_videLOrientation() throws Exception {
        UUID orientation = fiche.reference(ORIENTATION, code("ORI-V"));
        UUID id = creerOk(corpsArticle(code("VIDER-ORI")).avec(ORIENTATION_ID, orientation));

        modifier(id, avecChampsAVider(corps(), ORIENTATION_ID)).andExpect(status().isOk());

        assertThat(fixture.ligneArticle(id).get("StkOrientationId")).isNull();
    }

    // ------------------------------------------------------------------ §9.1 superficies calculées à la lecture

    @Test
    void r9_1_batimentSansSousArticle_netteEtTitreeEgalesALaSurface() throws Exception {
        UUID id = creerOk(corpsArticle(code("SURF-0")).avec(ESTTITRE, true));

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath(DETAIL_NETTE).value(63))
                .andExpect(jsonPath(DETAIL_TITREE).value(63));
    }

    @Test
    void r9_1_batiment_superficieNette_surfacePlusSousArticlesRattaches() throws Exception {
        UUID id = creerOk(corpsArticle(code("SURF-N")).avec(ESTTITRE, true));
        creerSousArticle(id, code("SA-N1"), "10", true);
        creerSousArticle(id, code("SA-N2"), "5", false);

        detail(id).andExpect(status().isOk()).andExpect(jsonPath(DETAIL_NETTE).value(78));
    }

    @Test
    void r9_1_batimentTitre_superficieTitree_surfacePlusSousArticlesTitres() throws Exception {
        UUID id = creerOk(corpsArticle(code("SURF-T")).avec(ESTTITRE, true));
        creerSousArticle(id, code("SA-T1"), "10", true);
        creerSousArticle(id, code("SA-T2"), "5", false);

        detail(id).andExpect(status().isOk()).andExpect(jsonPath(DETAIL_TITREE).value(73));
    }

    @Test
    void r9_1_batimentNonTitre_superficieTitree_sousArticlesTitresSeuls() throws Exception {
        UUID id = creerOk(corpsArticle(code("SURF-NT")).avec(ESTTITRE, false));
        creerSousArticle(id, code("SA-NT1"), "10", true);
        creerSousArticle(id, code("SA-NT2"), "5", false);

        detail(id).andExpect(status().isOk()).andExpect(jsonPath(DETAIL_TITREE).value(10));
    }

    /** §9.1 : nature Terrain, AreaNet = Area − EquipementsArea. */
    @Test
    void r9_1_terrain_superficieNette_surfaceMoinsEquipements() throws Exception {
        UUID terrain = onglets.article(code("TER"), tranche.getId(), onglets.natureTerrain(), fixture.vocationProjet());
        fiche.surfaces(terrain, new BigDecimal("1000"), new BigDecimal("150"), true);

        detail(terrain).andExpect(status().isOk()).andExpect(jsonPath(DETAIL_NETTE).value(850));
    }

    // ------------------------------------------------------------------ §2 recopie des contenances au changement de présentation

    @Test
    void r2_changementDePresentation_contenancesDuTypeRecopiees() throws Exception {
        UUID chambre = fiche.contenance(code("CH"), "SDB", true);
        UUID salon = fiche.contenance(code("SAL"), "", false);
        UUID presentation = fiche.presentation(code("PT-C"), tranche.getId());
        fiche.contenanceDePresentation(presentation, chambre, 3, "SDB", true, 1);
        fiche.contenanceDePresentation(presentation, salon, 1, "", false, 0);
        UUID id = creerOk(corpsArticle(code("COPIE")));

        modifier(id, corps().avec(PRESENTATION_ID, presentation).json()).andExpect(status().isOk());

        List<Tuple> lignes = fiche.lignes(LIGNES_CONTENANCES, id);
        Tuple ligneChambre = lignes.stream().filter(t -> guid(chambre).equals(guid(t.get("contenance")))).findFirst()
                .orElseThrow();
        assertAll(
                () -> assertThat(lignes).extracting(t -> guid(t.get("contenance")))
                        .containsExactlyInAnyOrder(guid(chambre), guid(salon)),
                () -> assertThat(((Number) ligneChambre.get(NOMBRE)).intValue()).isEqualTo(3),
                () -> assertThat(ligneChambre.get("option1")).isEqualTo("SDB"),
                () -> assertThat(ligneChambre.get("avec1")).isEqualTo(true),
                () -> assertThat(((Number) ligneChambre.get("quantite1")).intValue()).isEqualTo(1));
    }

    @Test
    void r2_changementDePresentation_remplaceLesContenancesPrecedentes() throws Exception {
        UUID ancienne = fiche.contenance(code("ANC"), "", false);
        UUID chambre = fiche.contenance(code("CH-R"), "", false);
        UUID presentation = fiche.presentation(code("PT-R"), tranche.getId());
        fiche.contenanceDePresentation(presentation, chambre, 2, "", false, 0);
        UUID id = creerOk(corpsArticle(code("REMPL")));
        fiche.contenanceArticle(id, ancienne, 1);

        modifier(id, corps().avec(PRESENTATION_ID, presentation).json()).andExpect(status().isOk());

        assertThat(fiche.lignes(LIGNES_CONTENANCES, id)).extracting(t -> guid(t.get("contenance")))
                .containsExactly(guid(chambre));
    }

    /** §2 / Item.cs:226 : la recopie suit un changement ; la même présentation renvoyée ne touche pas aux lignes. */
    @Test
    void r2_memePresentationRenvoyee_contenancesGardees() throws Exception {
        UUID chambre = fiche.contenance(code("CH-M"), "", false);
        UUID presentation = fiche.presentation(code("PT-M"), tranche.getId());
        fiche.contenanceDePresentation(presentation, chambre, 2, "", false, 0);
        UUID id = creerOk(corpsArticle(code("MEME")));
        modifier(id, corps().avec(PRESENTATION_ID, presentation).json()).andExpect(status().isOk());
        UUID autre = fiche.contenance(code("AUT-M"), "", false);
        fiche.contenanceArticle(id, autre, 5);

        modifier(id, corps().avec(PRESENTATION_ID, presentation).json()).andExpect(status().isOk());

        assertThat(fiche.lignes(LIGNES_CONTENANCES, id)).extracting(t -> guid(t.get("contenance")))
                .containsExactlyInAnyOrder(guid(chambre), guid(autre));
    }

    // ------------------------------------------------------------------ §2 GET / PUT /api/articles/{id}/contenances (patron P1b)

    @Test
    void r2_lectureDesContenances_lignesDeLArticle() throws Exception {
        UUID chambre = fiche.contenance(code("CH-G"), "", false);
        UUID id = creerOk(corpsArticle(code("LIRE-C")));
        UUID ligne = fiche.contenanceArticle(id, chambre, 4);

        mockMvc.perform(get(CONTENANCES, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes", hasSize(1)))
                .andExpect(jsonPath(ligneContenance(chambre) + ".id").value(ligne.toString()))
                .andExpect(jsonPath(ligneContenance(chambre) + "." + NOMBRE).value(4));
    }

    @Test
    void r2_put_ligneSansId_creee_reponseRelue() throws Exception {
        UUID chambre = fiche.contenance(code("CH-P"), "", false);
        UUID id = creerOk(corpsArticle(code("PUT-C")));

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, chambre).avec(NOMBRE, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes", hasSize(1)))
                .andExpect(jsonPath(ligneContenance(chambre) + "." + NOMBRE).value(2));

        assertThat(fiche.lignes(LIGNES_CONTENANCES, id)).singleElement().satisfies(t -> assertAll(
                () -> assertThat(guid(t.get("contenance"))).isEqualTo(guid(chambre)),
                () -> assertThat(((Number) t.get(NOMBRE)).intValue()).isEqualTo(2)));
    }

    @Test
    void r2_put_idConnu_misAJour_ligneAbsente_supprimee() throws Exception {
        UUID chambre = fiche.contenance(code("CH-U"), "", false);
        UUID salon = fiche.contenance(code("SAL-U"), "", false);
        UUID id = creerOk(corpsArticle(code("PUT-U")));
        UUID ligneChambre = fiche.contenanceArticle(id, chambre, 1);
        fiche.contenanceArticle(id, salon, 1);

        enregistrerContenances(id, lignes(corps().avec("id", ligneChambre).avec(CONTENANCE_ID, chambre).avec(NOMBRE, 3)))
                .andExpect(status().isOk());

        List<Tuple> lignes = fiche.lignes("""
                SELECT CONVERT(varchar(36), PKey) AS pkey, ItemsNumber AS nombre FROM StkItemContenance
                WHERE StkItemId = :id""", id);
        assertThat(lignes).singleElement().satisfies(t -> assertAll(
                () -> assertThat(guid(t.get("pkey"))).isEqualTo(guid(ligneChambre)),
                () -> assertThat(((Number) t.get(NOMBRE)).intValue()).isEqualTo(3)));
    }

    @Test
    void r2_put_idDUnAutreArticle_400_rienNEstEcrit() throws Exception {
        UUID chambre = fiche.contenance(code("CH-X"), "", false);
        UUID id = creerOk(corpsArticle(code("PUT-X")));
        UUID autre = creerOk(corpsArticle(code("PUT-X2")));
        UUID ligneAutre = fiche.contenanceArticle(autre, chambre, 1);

        enregistrerContenances(id, lignes(corps().avec("id", ligneAutre).avec(CONTENANCE_ID, chambre).avec(NOMBRE, 9)))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(fiche.compter(COMPTE_CONTENANCES, id)).isZero(),
                () -> assertThat(fiche.lignes(LIGNES_CONTENANCES, autre)).singleElement()
                        .satisfies(t -> assertThat(((Number) t.get(NOMBRE)).intValue()).isEqualTo(1)));
    }

    /** §2 : StkContenanceId obligatoire (O). */
    @Test
    void r2_put_contenanceManquante_400() throws Exception {
        UUID id = creerOk(corpsArticle(code("PUT-SANS")));

        enregistrerContenances(id, lignes(corps().avec(NOMBRE, 1))).andExpect(status().isBadRequest());
    }

    @Test
    void r2_put_contenanceInconnue_400() throws Exception {
        UUID id = creerOk(corpsArticle(code("PUT-INC")));

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, UUID.randomUUID()).avec(NOMBRE, 1)))
                .andExpect(status().isBadRequest());
    }

    /** §2 : option N grisée (refus 400) si StkContenance.OptionN est vide. */
    @Test
    void r2_put_optionGrisee_400() throws Exception {
        UUID salon = fiche.contenance(code("SAL-G"), "", false);
        UUID id = creerOk(corpsArticle(code("PUT-GRIS")));

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, salon).avec(NOMBRE, 1).avec(AVEC_OPTION_1, true)))
                .andExpect(status().isBadRequest());

        assertThat(fiche.compter(COMPTE_CONTENANCES, id)).isZero();
    }

    @Test
    void r2_put_optionRenseignee_cochee_200() throws Exception {
        UUID cuisine = fiche.contenance(code("CUI"), "Cuisine americaine", false);
        UUID id = creerOk(corpsArticle(code("PUT-OPT")));

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, cuisine).avec(NOMBRE, 1).avec(AVEC_OPTION_1, true)))
                .andExpect(status().isOk());

        assertThat(fiche.lignes(LIGNES_CONTENANCES, id)).singleElement()
                .satisfies(t -> assertThat(t.get("avec1")).isEqualTo(true));
    }

    /** §2 : quantité seulement si IsOptionNWithQuantity. */
    @Test
    void r2_put_quantiteSurOptionSansQuantite_400() throws Exception {
        UUID cuisine = fiche.contenance(code("CUI-Q"), "Cuisine americaine", false);
        UUID id = creerOk(corpsArticle(code("PUT-QTE")));

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, cuisine).avec(NOMBRE, 1).avec(AVEC_OPTION_1, true)
                .avec(QUANTITE_OPTION_1, 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r2_put_quantiteSurOptionAvecQuantite_200() throws Exception {
        UUID chambre = fiche.contenance(code("CH-Q"), "SDB", true);
        UUID id = creerOk(corpsArticle(code("PUT-QOK")));

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, chambre).avec(NOMBRE, 3).avec(AVEC_OPTION_1, true)
                .avec(QUANTITE_OPTION_1, 2)))
                .andExpect(status().isOk());

        assertThat(fiche.lignes(LIGNES_CONTENANCES, id)).singleElement()
                .satisfies(t -> assertThat(((Number) t.get("quantite1")).intValue()).isEqualTo(2));
    }

    @Test
    void r2_put_articleInconnu_404() throws Exception {
        enregistrerContenances(UUID.randomUUID(), lignes()).andExpect(status().isNotFound());
    }

    @Test
    void r2_get_articleInconnu_404() throws Exception {
        mockMvc.perform(get(CONTENANCES, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §2 types de présentation de la tranche

    @Test
    void r2_presentationsDeLaTranche_sansCellesDUneAutre() throws Exception {
        UUID duLot = fiche.presentation(code("PT-T"), tranche.getId());
        UUID ailleurs = fiche.presentation(code("PT-A"), fixture.tranche(code("TR-A"), projet).getId());

        mockMvc.perform(get(PRESENTATIONS, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parId(duLot) + ".code").value(code("PT-T")))
                .andExpect(jsonPath(parId(duLot) + ".description").value(FixtureArticleFiche.libelle(code("PT-T"))))
                .andExpect(jsonPath(parId(ailleurs)).isEmpty());
    }

    @Test
    void r2_presentations_trancheInconnue_404() throws Exception {
        mockMvc.perform(get(PRESENTATIONS, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ R4 : entité inactive → 404 sur les nouvelles routes

    /** Vue active (200), puis l'entité désactivée (404), comme VisibiliteR4EtendueTest. */
    @Test
    void r4_contenances_lecture_entiteInactive_404() throws Exception {
        UUID id = creerOk(corpsArticle(code("R4-G")));
        mockMvc.perform(get(CONTENANCES, id).with(connecte(admin))).andExpect(status().isOk());

        entites.actif(entite.getId(), false);

        mockMvc.perform(get(CONTENANCES, id).with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void r4_contenances_ecriture_entiteInactive_404() throws Exception {
        UUID chambre = fiche.contenance(code("CH-R4"), "", false);
        UUID id = creerOk(corpsArticle(code("R4-P")));
        enregistrerContenances(id, lignes()).andExpect(status().isOk());

        entites.actif(entite.getId(), false);

        enregistrerContenances(id, lignes(corps().avec(CONTENANCE_ID, chambre).avec(NOMBRE, 1)))
                .andExpect(status().isNotFound());
        assertThat(fiche.compter(COMPTE_CONTENANCES, id)).isZero();
    }

    @Test
    void r4_presentations_trancheDUneEntiteInactive_404() throws Exception {
        mockMvc.perform(get(PRESENTATIONS, tranche.getId()).with(connecte(admin))).andExpect(status().isOk());

        entites.actif(entite.getId(), false);

        mockMvc.perform(get(PRESENTATIONS, tranche.getId()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}
