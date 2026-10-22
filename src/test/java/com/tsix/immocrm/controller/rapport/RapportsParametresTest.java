package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.REPARTITION_CLIENTS;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paramètres (FEATURE-tdb-vente-rapports-socle.md §3.2 {@code ParametresRapport}, §4.3, §4.4, §3.5) sur les deux rapports
 * réels : n° 12 ({@code trancheId}) et n° 13 ({@code du}, {@code au}, {@code trancheId}). Compte à rôle libre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsParametresTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String DU = "du";
    private static final String AU = "au";
    private static final String FORMAT = "format";
    private static final String XLSX = "xlsx";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes etatVentes;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("RPA");
        fixture.tables();
        fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        fixture.rapportVente(REPARTITION_CLIENTS, "REPARTITION DES CLIENTS", "", 1100);
        libre = connecte(fixture.compte("par." + prefixe, fixture.role("Libre " + prefixe, false)));
        tranche = etatVentes.tranche("TR-" + prefixe, etatVentes.projet("PRJ-" + prefixe, true), true);
        etatVentes.article(prefixe + "-A1", tranche, etatVentes.categorie("CAT-" + prefixe),
                etatVentes.type("TYP-" + prefixe));
        fixture.relire();
    }

    // ------------------------------------------------------------------ 400

    @Test
    void p400_parametreInconnu_messageListeLesNomsAcceptes() throws Exception {
        mockMvc.perform(get(resultat(VENTE, SUIVI_GLOBAL)).param("tranche", tranche.getId().toString()).with(libre))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(TRANCHE_ID)));
    }

    @Test
    void p400_parametreInconnu_surLExport() throws Exception {
        assertThat(appels.exporter(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, XLSX).param("inconnu", "1")
                .with(libre)).getResponse().getStatus()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01/02/2026", "2026-13-01", "2026-02-30", "demain"})
    void p400_dateMalFormee(String valeur) throws Exception {
        mockMvc.perform(get(resultat(VENTE, REPARTITION_CLIENTS)).param(DU, valeur).with(libre))
                .andExpect(status().isBadRequest());
    }

    @Test
    void p400_auAvantDu() throws Exception {
        mockMvc.perform(get(resultat(VENTE, REPARTITION_CLIENTS)).param(DU, "2025-06-02").param(AU, "2025-06-01")
                        .with(libre))
                .andExpect(status().isBadRequest());
    }

    @Test
    void p200_auEgalDu_accepte() throws Exception {
        mockMvc.perform(get(resultat(VENTE, REPARTITION_CLIENTS)).param(DU, "2025-06-01").param(AU, "2025-06-01")
                        .param(TRANCHE_ID, tranche.getId().toString()).with(libre))
                .andExpect(status().isOk());
    }

    @Test
    void p400_uuidMalForme() throws Exception {
        mockMvc.perform(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, "pas-un-uuid").with(libre))
                .andExpect(status().isBadRequest());
    }

    @Test
    void p400_repetitionDUnParametreNonMultiple() throws Exception {
        mockMvc.perform(get(resultat(VENTE, REPARTITION_CLIENTS)).param(DU, "2025-01-01", "2025-02-01")
                        .with(libre))
                .andExpect(status().isBadRequest());
    }

    @Test
    void p200_repetitionDUnParametreMultiple_accepte() throws Exception {
        mockMvc.perform(get(resultat(VENTE, SUIVI_GLOBAL))
                        .param(TRANCHE_ID, tranche.getId().toString(), UUID.randomUUID().toString()).with(libre))
                .andExpect(status().isOk());
    }

    @Test
    void p400_formatAutreQueXlsx() throws Exception {
        assertThat(appels.exporter(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, "csv").with(libre))
                .getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void p400_lesRefusDeParametreValentAussiPourLExport() throws Exception {
        assertThat(appels.exporter(get(export(VENTE, REPARTITION_CLIENTS)).param(FORMAT, XLSX)
                .param(DU, "2025-06-02").param(AU, "2025-06-01").with(libre)).getResponse().getStatus())
                .isEqualTo(400);
    }

    // ------------------------------------------------------------------ valeurs par défaut (horloge du projet)

    @Test
    void descripteur_n13_duAuParDefaut_premierJanvierEtAujourdhui() throws Exception {
        LocalDate aujourdhui = LocalDate.now(clock);

        mockMvc.perform(get(descripteur(VENTE, REPARTITION_CLIENTS)).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].type").value("DATE"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].valeurParDefaut")
                        .value(aujourdhui.withDayOfYear(1).toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'au')].valeurParDefaut").value(aujourdhui.toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].multiple").value(true));
    }

    @Test
    void resultat_parametresAppliques_defautsCompris() throws Exception {
        LocalDate aujourdhui = LocalDate.now(clock);

        mockMvc.perform(get(resultat(VENTE, REPARTITION_CLIENTS)).param(TRANCHE_ID, tranche.getId().toString())
                        .with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(REPARTITION_CLIENTS))
                .andExpect(jsonPath("$.module").value(VENTE))
                .andExpect(jsonPath("$.parametres.du[0]").value(aujourdhui.withDayOfYear(1).toString()))
                .andExpect(jsonPath("$.parametres.au[0]").value(aujourdhui.toString()));
    }

    // ------------------------------------------------------------------ export (§3.5, §4.4)

    @Test
    void export_uneFeuilleParSection_nomsNettoyesUniques_ligneTotal_nomDeFichier() throws Exception {
        String id = tranche.getId().toString();
        List<Map<String, Object>> sections = appels.sections(get(resultat(VENTE, SUIVI_GLOBAL))
                .param(TRANCHE_ID, id).with(libre));

        MockHttpServletResponse reponse = appels.exporter(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, XLSX)
                .param(TRANCHE_ID, id).with(libre)).getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        assertThat(reponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains(SUIVI_GLOBAL + "_").contains(".xlsx");
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            List<String> noms = new ArrayList<>();
            classeur.forEach(f -> noms.add(f.getSheetName()));
            assertThat(noms).isNotEmpty().hasSize(sections.size()).doesNotHaveDuplicates()
                    .allSatisfy(n -> assertThat(n).hasSizeLessThanOrEqualTo(31).doesNotContainPattern("[\\[\\]:*?/\\\\]"));
            Sheet stock = classeur.getSheet(nomFeuille(AppelsRapports.section(sections, "stock")));
            assertThat(stock).isNotNull();
            assertThat(cellules(stock.getRow(stock.getLastRowNum()))).contains("Total");
        }
    }

    @Test
    void export_lignesMapDeRapport_valeursEcrites() throws Exception {
        List<Map<String, Object>> sections = appels.sections(get(resultat(VENTE, SUIVI_GLOBAL))
                .param(TRANCHE_ID, tranche.getId().toString()).with(libre));
        String titre = nomFeuille(AppelsRapports.section(sections, "stock"));

        byte[] contenu = appels.exporter(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, XLSX)
                .param(TRANCHE_ID, tranche.getId().toString()).with(libre)).getResponse().getContentAsByteArray();

        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet stock = classeur.getSheet(titre);
            assertThat(cellules(stock.getRow(1))).contains(tranche.getCode());
        }
    }

    /** Nom attendu (§3.5) : titre sans {@code []:*?/\}, tronqué à 31 caractères (pas de doublon dans ce rapport). */
    private static String nomFeuille(Map<String, Object> section) {
        String nettoye = String.valueOf(section.get("titre")).replaceAll("[\\[\\]:*?/\\\\]", "");
        return nettoye.length() > 31 ? nettoye.substring(0, 31) : nettoye;
    }

    private static List<String> cellules(Row row) {
        List<String> valeurs = new ArrayList<>();
        for (Cell cell : row) {
            valeurs.add(cell.toString());
        }
        return valeurs;
    }

    @Test
    void sectionIds_uniquesEtStables() throws Exception {
        String corps = appels.corps(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, tranche.getId().toString())
                .with(libre));

        List<String> ids = JsonPath.read(corps, "$.sections[*].id");
        Set<String> uniques = new HashSet<>(ids);
        assertThat(ids).isNotEmpty().hasSameSizeAs(uniques)
                .allSatisfy(i -> assertThat(i).matches("[a-z0-9-]+"));
    }
}
