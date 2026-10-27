package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ApiCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.FINALISATION_1;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rapport n° 34 « COMMISSIONS DE VENTE PAR COMMERCIAL » ({@code ComCommissionsByCommercial}) — FEATURE-vente-commissions.md §6,
 * §3.1 C7 (même agrégation que l'écran), §8.1 « Rapport n° 34 ». Jeu (DocDate) :
 * <pre>
 *   X (tranche T) : Concrétisation 300 le 01/01/2025 (document D réglé 150), Désistement −100 le 30/06/2025,
 *                   Concrétisation 1 000 le 31/12/2024 (hors période)
 *   Y (tranche T2) : Finalisation 1 500 le 15/03/2025
 *   Z (tranche TI, entité inactive) : Concrétisation 700 le 01/02/2025
 *   période 01/01/2025 → 30/06/2025 ⇒ X 200 / réglé 150 / reste 50 ; Y 500 / 0 / 500 ; total 700
 * </pre>
 * Avant le code, le code de rapport sans classe répond 404 : les tests sont rouges.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionsParCommercialRapportTest {

    private static final String CODE = "ComCommissionsByCommercial";
    private static final String COMMERCIAUX = "commerciaux";
    private static final String LIGNES = "lignes";
    private static final String COMMERCIAL = "commercial";
    private static final String MONTANT = "montant";
    private static final String DU = "du";
    private static final String AU = "au";
    private static final String TRANCHE_ID = "trancheId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureCommissions fixture;

    private AppelsRapports appels;
    private Contexte c;
    private CfgTranche t2;
    private CfgTranche ti;
    private String nomX;
    private String nomY;
    private UUID x;
    private RequestPostProcessor libre;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        rapports.tables();
        rapports.rapportVente(CODE, "COMMISSIONS DE VENTE PAR COMMERCIAL", "COMMISSION", 10010);
        c = fixture.contexte("R34");
        t2 = fixture.autreTranche(c, "T2", true);
        ti = fixture.autreTranche(c, "TI", false);
        nomX = "X" + c.jeton();
        nomY = "Y" + c.jeton();
        x = fixture.personne(nomX, false);
        UUID y = fixture.personne(nomY, false);
        BigDecimal prix = new BigDecimal("775000");
        Dossier dx = fixture.dossier(c, "DX", x, prix, LocalDate.of(2025, 1, 1));
        Dossier dy = fixture.dossier(c, t2, "DY", y, prix, LocalDate.of(2025, 1, 1));
        Dossier dz = fixture.dossier(c, ti, "DZ", x, prix, LocalDate.of(2025, 1, 1));
        UUID doc = fixture.document(x, "DOC-" + c.jeton(), new BigDecimal("300"));
        fixture.ligne(dx, CONCRETISATION, x, new BigDecimal("300"), LocalDate.of(2025, 1, 1), doc);
        fixture.reglement(doc, new BigDecimal("150"), "IN_WORK");
        fixture.ligne(dx, DESISTEMENT, x, new BigDecimal("-100"), LocalDate.of(2025, 6, 30), null);
        fixture.ligne(dx, CONCRETISATION, fixture.personne("ANC" + c.jeton(), false), new BigDecimal("1000"),
                LocalDate.of(2024, 12, 31), null);
        fixture.ligne(dy, FINALISATION_1, y, new BigDecimal("500"), LocalDate.of(2025, 3, 15), null);
        fixture.ligne(dz, CONCRETISATION, x, new BigDecimal("700"), LocalDate.of(2025, 2, 1), null);
        libre = connecte(rapports.compte("r34." + c.jeton(), rapports.role("Libre " + c.jeton(), false)));
        rapports.relire();
    }

    private MockHttpServletRequestBuilder demande(String du, String au, CfgTranche... tranches) {
        MockHttpServletRequestBuilder r = get(resultat(VENTE, CODE)).param(DU, du).param(AU, au).with(libre);
        for (CfgTranche t : tranches) {
            r.param(TRANCHE_ID, t.getId().toString());
        }
        return r;
    }

    private List<Map<String, Object>> sections(MockHttpServletRequestBuilder requete) throws Exception {
        return appels.sections(requete);
    }

    private List<Map<String, Object>> semestre() throws Exception {
        return sections(demande("2025-01-01", "2025-06-30", c.tranche(), t2, ti));
    }

    private static Map<String, Object> ligneDe(Map<String, Object> section, String nom) {
        List<Map<String, Object>> l = lignes(section).stream()
                .filter(m -> Objects.toString(m.get(COMMERCIAL), "").contains(nom)).toList();
        assertThat(l).as("ligne de %s", nom).hasSize(1);
        return l.get(0);
    }

    // ------------------------------------------------------------------ droits (socle R0)

    @Test
    void r34_roleNonRestreint_200_deuxSections() throws Exception {
        assertThat(sections(demande("2025-01-01", "2025-06-30", c.tranche())))
                .extracting(s -> s.get("id")).containsExactly(COMMERCIAUX, LIGNES);
    }

    /** §1 : 0 ligne {@code AdmReportDataRole} → un rôle restreint ne voit pas le rapport. */
    @Test
    void r34_roleRestreint_403() throws Exception {
        RequestPostProcessor restreint = connecte(rapports.compte("r34r." + c.jeton(),
                rapports.role("Restreint " + c.jeton(), true)));
        rapports.relire();

        assertAll(
                () -> assertThat(appels.statut(get(descripteur(VENTE, CODE)).with(restreint))).isEqualTo(403),
                () -> assertThat(appels.statut(get(resultat(VENTE, CODE)).param(DU, "2025-01-01").param(AU, "2025-06-30")
                        .with(restreint))).isEqualTo(403));
    }

    // ------------------------------------------------------------------ section commerciaux

    @Test
    void r34_commerciaux_montantRegleReste_periodeBornesIncluses() throws Exception {
        Map<String, Object> s = section(semestre(), COMMERCIAUX);

        Map<String, Object> lx = ligneDe(s, nomX);
        Map<String, Object> ly = ligneDe(s, nomY);
        assertAll(
                () -> assertThat(nombre(lx.get(MONTANT))).as("300 − 100, 1 000 hors période").isEqualByComparingTo("200"),
                () -> assertThat(nombre(lx.get("regle"))).isEqualByComparingTo("150"),
                () -> assertThat(nombre(lx.get("reste"))).isEqualByComparingTo("50"),
                () -> assertThat(nombre(ly.get(MONTANT))).isEqualByComparingTo("500"),
                () -> assertThat(nombre(ly.get("regle"))).isEqualByComparingTo("0"),
                () -> assertThat(nombre(ly.get("reste"))).isEqualByComparingTo("500"),
                () -> assertThat(nombre(totaux(s).get(MONTANT))).isEqualByComparingTo("700"),
                () -> assertThat(nombre(totaux(s).get(MONTANT))).isEqualByComparingTo(somme(lignes(s), MONTANT)));
    }

    @Test
    void r34_trancheDUneEntiteInactive_exclue() throws Exception {
        Map<String, Object> s = section(semestre(), COMMERCIAUX);

        assertThat(nombre(ligneDe(s, nomX).get(MONTANT))).as("700 de TI exclus").isEqualByComparingTo("200");
    }

    // ------------------------------------------------------------------ section lignes

    @Test
    void r34_lignes_desistementNegatif_dansLeSousTotalDuCommercial() throws Exception {
        Map<String, Object> s = section(semestre(), LIGNES);

        List<Map<String, Object>> deX = lignes(s).stream()
                .filter(l -> Objects.toString(l.get(COMMERCIAL), "").contains(nomX)).toList();
        Map<String, Map<String, Object>> sousTotaux = sousTotaux(s);
        List<String> cleX = sousTotaux.keySet().stream().filter(k -> k.contains(nomX)).toList();
        assertAll(
                () -> assertThat(deX).hasSize(2)
                        .anySatisfy(l -> assertThat(nombre(l.get(MONTANT))).isEqualByComparingTo("-100")),
                () -> assertThat(cleX).hasSize(1),
                () -> assertThat(nombre(sousTotaux.get(cleX.get(0)).get(MONTANT))).isEqualByComparingTo("200"),
                () -> assertThat(nombre(totaux(s).get(MONTANT))).isEqualByComparingTo("700"));
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void r34_filtreTranche() throws Exception {
        Map<String, Object> s = section(sections(demande("2025-01-01", "2025-06-30", t2)), COMMERCIAUX);

        assertThat(lignes(s)).singleElement()
                .satisfies(l -> assertThat(Objects.toString(l.get(COMMERCIAL), "")).contains(nomY));
    }

    @Test
    void r34_filtreCommercial() throws Exception {
        Map<String, Object> s = section(sections(demande("2025-01-01", "2025-06-30", c.tranche(), t2)
                .param("commercialId", x.toString())), COMMERCIAUX);

        assertThat(lignes(s)).singleElement()
                .satisfies(l -> assertThat(Objects.toString(l.get(COMMERCIAL), "")).contains(nomX));
    }

    @Test
    void r34_periodeSansLigne_deuxSectionsVides() throws Exception {
        List<Map<String, Object>> sections = sections(demande("2030-01-01", "2030-12-31", c.tranche(), t2));

        assertAll(
                () -> assertThat(lignes(section(sections, COMMERCIAUX))).isEmpty(),
                () -> assertThat(lignes(section(sections, LIGNES))).isEmpty());
    }

    // ------------------------------------------------------------------ C7 : même agrégation que l'écran

    @Test
    void c7_totalDuRapport_egalMontantGenereDeLaTranche() throws Exception {
        Map<String, Object> s = section(sections(demande("2000-01-01", "2030-12-31", c.tranche())), COMMERCIAUX);
        RequestPostProcessor gestionnaire = fixture.compte(c.jeton(), ECRAN).qui();

        String entete = mockMvc.perform(ApiCommissions.tranche(c.tranche().getId()).with(gestionnaire))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        BigDecimal genere = new BigDecimal(String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(entete, "$.montantGenere")));
        BigDecimal regle = new BigDecimal(String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(entete, "$.montantRegle")));
        assertAll(
                () -> assertThat(nombre(totaux(s).get(MONTANT))).isEqualByComparingTo(genere),
                () -> assertThat(nombre(totaux(s).get("regle"))).isEqualByComparingTo(regle));
    }

    // ------------------------------------------------------------------ export

    @Test
    void r34_exportXlsx_deuxFeuilles() throws Exception {
        MvcResult reponse = appels.exporter(get(export(VENTE, CODE)).param("format", "xlsx").param(DU, "2025-01-01")
                .param(AU, "2025-06-30").param(TRANCHE_ID, c.tranche().getId().toString()).with(libre));

        assertThat(reponse.getResponse().getStatus()).isEqualTo(200);
        try (XSSFWorkbook classeur = new XSSFWorkbook(
                new ByteArrayInputStream(reponse.getResponse().getContentAsByteArray()))) {
            assertThat(classeur.getNumberOfSheets()).isEqualTo(2);
        }
    }
}
