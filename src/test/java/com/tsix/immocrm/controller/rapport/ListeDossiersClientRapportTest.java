package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnesAvecTotal;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.LISTE_DOSSIERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 1 LISTE DOSSIERS (FEATURE-tdb-vente-rapports-dossiers.md §3.4, §9 « N° 1 (rapport) », RV-R4a/b/c) : enveloppe de la
 * liste {@code /dossiers}, même périmètre, même ordre OMRAN (code article), mêmes montants.
 * <pre>
 *   T  (entité active) : articles créés dans l'ordre B, A, C
 *        A : en cours, client complet, syndic 10 000 réglé 4 000      B : en cours, syndic 2 000
 *        C : ANNULÉ, sans syndic
 *   TI (entité INACTIVE) : un dossier en cours avec syndic 50 000
 *   TV (entité active)   : aucun dossier
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListeDossiersClientRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String SECTION = "dossiers";
    private static final String CODE = "code";
    private static final String FRAIS_SYNDIC = "fraisSyndic";
    private static final String RESTE_SYNDIC = "resteSyndic";
    private static final String AVERTISSEMENTS = "$.avertissements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;
    @Autowired private CfgTierRepository tiers;

    private AppelsRapports appels;
    private String prefixe;
    private UUID rapport;
    private AdmRole roleLibre;
    private RequestPostProcessor libre;
    private CfgTranche t;
    private CfgTranche ti;
    private CfgTranche tv;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus enCours;
    private ComFolder dossierA;
    private ComFolder dossierB;
    private ComFolder dossierC;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("LDC");
        fixture.tables();
        rapport = fixture.rapportVente(LISTE_DOSSIERS, "LISTE DOSSIERS", null, 0);
        roleLibre = fixture.role("Libre " + prefixe, false);
        libre = connecte(fixture.compte("ldc." + prefixe, roleLibre));

        t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        tv = ev.tranche("TV-" + prefixe, ev.projet("PV-" + prefixe, true), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        enCours = ev.statutEnCours("ENC-" + prefixe);
        ComSettlementCategory syndic = rd.categorieSyndic();

        CfgTier clientA = ev.client("CA-" + prefixe);
        clientA.setEMail("a." + prefixe + "@exemple.ma");
        clientA = tiers.save(clientA);
        dossierB = dossier(t, "B", enCours, ev.client("CB-" + prefixe));
        dossierA = dossier(t, "A", enCours, clientA);
        dossierC = dossier(t, "C", rd.statutAnnule("ANN-" + prefixe), ev.client("CC-" + prefixe));
        ev.regler(rd.echeance(dossierA, syndic, new BigDecimal("10000")), new BigDecimal("4000"), true, "POINTED");
        rd.echeance(dossierB, syndic, new BigDecimal("2000"));
        rd.echeance(dossier(ti, "I", enCours, ev.client("CI-" + prefixe)), syndic, new BigDecimal("50000"));
        fixture.relire();
    }

    private ComFolder dossier(CfgTranche tranche, String lettre, StrStatus statut, CfgTier client) {
        return ev.vendre(ev.article(prefixe + "-" + lettre, tranche, categorie, type), "DOS-" + prefixe + "-" + lettre,
                statut, client, null);
    }

    private MockHttpServletRequestBuilder sur(RequestPostProcessor qui, String... trancheIds) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, LISTE_DOSSIERS)).with(qui);
        for (String id : trancheIds) {
            requete.param(TRANCHE_ID, id);
        }
        return requete;
    }

    private Map<String, Object> dossiers(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), SECTION);
    }

    private static List<Object> codes(Map<String, Object> section) {
        return lignes(section).stream().map(l -> l.get(CODE)).toList();
    }

    private static Map<String, Object> ligneDe(Map<String, Object> section, ComFolder dossier) {
        List<Map<String, Object>> trouvees = lignesOu(section, CODE, dossier.getCode());
        assertThat(trouvees).as("ligne de %s", dossier.getCode()).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ forme (§3.4)

    @Test
    void n1_seizeColonnes_dansLOrdreDuModele() throws Exception {
        Map<String, Object> section = dossiers(sur(libre, t.getId().toString()));

        assertThat(colonnes(section).stream().map(c -> c.get("nom")).toList()).containsExactly(CODE, "article",
                "client", "telephone", "mobile", "email", "adresse", "ville", "pays", "statut", FRAIS_SYNDIC,
                RESTE_SYNDIC, "dateConcretisation", "dateMinute", "dateRemiseCles", "titreFoncier");
    }

    @Test
    void n1_ordre_parCodeArticle() throws Exception {
        Map<String, Object> section = dossiers(sur(libre, t.getId().toString()));

        assertThat(codes(section)).containsExactly(dossierA.getCode(), dossierB.getCode(), dossierC.getCode());
    }

    @Test
    void n1_dossierAnnule_present() throws Exception {
        Map<String, Object> section = dossiers(sur(libre, t.getId().toString()));

        assertThat(codes(section)).contains(dossierC.getCode());
    }

    @Test
    void n1_valeursClientEtSyndic_duDossierA() throws Exception {
        Map<String, Object> ligneA = ligneDe(dossiers(sur(libre, t.getId().toString())), dossierA);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ligneA).containsEntry("email", "a." + prefixe + "@exemple.ma")
                .containsEntry("article", prefixe + "-A");
        soft.assertThat(nombre(ligneA.get(FRAIS_SYNDIC))).isEqualByComparingTo("10000");
        soft.assertThat(nombre(ligneA.get(RESTE_SYNDIC))).isEqualByComparingTo("6000");
        soft.assertAll();
    }

    /** Totaux serveur : seules {@code fraisSyndic} et {@code resteSyndic} portent une SOMME. */
    @Test
    void n1_totaux_fraisSyndicEtResteSyndic_sommeServeur() throws Exception {
        Map<String, Object> section = dossiers(sur(libre, t.getId().toString()));

        assertThat(colonnesAvecTotal(section, "SOMME")).containsExactlyInAnyOrder(FRAIS_SYNDIC, RESTE_SYNDIC);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(totaux(section).get(FRAIS_SYNDIC))).isEqualByComparingTo("12000")
                .isEqualByComparingTo(somme(lignes(section), FRAIS_SYNDIC));
        soft.assertThat(nombre(totaux(section).get(RESTE_SYNDIC))).isEqualByComparingTo("8000")
                .isEqualByComparingTo(somme(lignes(section), RESTE_SYNDIC));
        soft.assertAll();
    }

    @Test
    void n1_sansTrancheDemandee_toutesLesVisibles_sansEntiteInactive() throws Exception {
        Map<String, Object> section = dossiers(sur(libre));

        assertThat(codes(section)).isNotEmpty().contains(dossierA.getCode(), dossierB.getCode(), dossierC.getCode())
                .doesNotContain("DOS-" + prefixe + "-I");
    }

    // ------------------------------------------------------------------ RV-R4a : tranches

    @Test
    void rv_r4a_trancheDUneEntiteInactive_vide_jamaisToutes() throws Exception {
        assertThat(lignes(dossiers(sur(libre, ti.getId().toString())))).isEmpty();
    }

    @Test
    void rv_r4a_trancheIdInconnu_vide_jamaisToutes() throws Exception {
        assertThat(lignes(dossiers(sur(libre, UUID.randomUUID().toString())))).isEmpty();
    }

    @Test
    void rv_r4a_trancheHorsPerimetre_vide_jamaisToutes() throws Exception {
        AdmUser compte = fixture.compte("ldc.hp." + prefixe, roleLibre);
        ev.affecter(compte, tv.getCfgProject());
        fixture.relire();

        assertThat(lignes(dossiers(sur(connecte(compte), t.getId().toString())))).isEmpty();
    }

    @Test
    void n1_aucunDossier_avertissement() throws Exception {
        String corps = appels.corps(sur(libre, tv.getId().toString()));

        List<String> avertissements = JsonPath.read(corps, AVERTISSEMENTS);
        assertThat(avertissements).contains("Aucun dossier pour ces tranches");
    }

    // ------------------------------------------------------------------ RV-R4b : la liste est la source

    @Test
    void rv_r4b_memesLignesDansLeMemeOrdreQueLaListe() throws Exception {
        List<Object> duRapport = codes(dossiers(sur(libre, t.getId().toString())));

        String corps = appels.corps(get("/api/dossiers").param(TRANCHE_ID, t.getId().toString())
                .param("inclureSortis", "true").param("ordreOmran", "true").param("colonnes", CODE)
                .param("size", "100").with(libre));
        List<Object> deLaListe = JsonPath.read(corps, "$.content[*].code");

        assertThat(duRapport).isNotEmpty().containsExactlyElementsOf(deLaListe);
    }

    @Test
    void rv_r4b_fraisSyndic_memesValeursQueLaListe() throws Exception {
        Map<String, Object> section = dossiers(sur(libre, t.getId().toString()));
        String corps = appels.corps(get("/api/dossiers").param(TRANCHE_ID, t.getId().toString())
                .param("inclureSortis", "true").param("colonnes", "code,fraisSyndic,fraisSyndicReste")
                .param("size", "100").with(libre));
        List<Map<String, Object>> liste = JsonPath.read(corps, "$.content");

        assertThat(liste).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> l : liste) {
            List<Map<String, Object>> duRapport = lignesOu(section, CODE, l.get(CODE));
            soft.assertThat(duRapport).as("%s", l.get(CODE)).hasSize(1);
            if (duRapport.size() == 1) {
                soft.assertThat(nombre(duRapport.get(0).get(FRAIS_SYNDIC))).as("%s", l.get(CODE))
                        .isEqualByComparingTo(nombre(l.get(FRAIS_SYNDIC)));
                soft.assertThat(nombre(duRapport.get(0).get(RESTE_SYNDIC))).as("%s", l.get(CODE))
                        .isEqualByComparingTo(nombre(l.get("fraisSyndicReste")));
            }
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ RV-R4c : statuts visibles par rôle

    @Test
    void rv_r4c_statutMasquePourLeRole_dossierAbsent_etAvertissement() throws Exception {
        AdmRole chef = fixture.role("Chef " + prefixe, true);
        fixture.lier(rapport, chef);
        rd.statutVisiblePar(enCours, chef);
        StrStatus masque = ev.statutEnCours("MASQ-" + prefixe);
        ComFolder cache = dossier(t, "M", masque, ev.client("CM-" + prefixe));
        fixture.relire();
        RequestPostProcessor qui = connecte(fixture.compte("ldc.chef." + prefixe, chef));

        String corps = appels.corps(sur(qui, t.getId().toString()));

        List<String> codesVus = JsonPath.read(corps, "$.sections[?(@.id == 'dossiers')].lignes[*].code");
        List<String> avertissements = JsonPath.read(corps, AVERTISSEMENTS);
        assertThat(codesVus).isNotEmpty().contains(dossierA.getCode()).doesNotContain(cache.getCode());
        assertThat(avertissements).contains("Statuts masqués pour votre profil");
    }

    // ------------------------------------------------------------------ export

    @Test
    void export_uneFeuille() throws Exception {
        MvcResult reponse = appels.exporter(get(export(VENTE, LISTE_DOSSIERS)).param("format", "xlsx")
                .param(TRANCHE_ID, t.getId().toString()).with(libre));

        assertThat(reponse.getResponse().getStatus()).isEqualTo(200);
        try (XSSFWorkbook classeur = new XSSFWorkbook(
                new ByteArrayInputStream(reponse.getResponse().getContentAsByteArray()))) {
            assertThat(classeur.getNumberOfSheets()).isEqualTo(1);
        }
    }
}
