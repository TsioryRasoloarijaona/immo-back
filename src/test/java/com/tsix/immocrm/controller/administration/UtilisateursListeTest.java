package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.RACINE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.SEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Filtres ajoutés, R18 et export « des mêmes filtres ». */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursListeTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ROLE_ID = "roleId";
    private static final String PERSONNEL_ID = "personnelId";
    private static final String DISABLED = "disabled";
    private static final String TOTAL = "$.totalElements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AppelsUtilisateurs appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private void trouveSeul(String code, String... paires) throws Exception {
        appels.lire(RACINE, paires).andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].username").value(code));
    }

    private List<String> lignesExport(String... paires) throws Exception {
        MvcResult resultat = appels.exporter(EXPORT, paires);
        status().isOk().match(resultat);
        assertThat(resultat.getResponse().getContentType()).isEqualTo(XLSX);
        List<String> lignes = new ArrayList<>();
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(resultat.getResponse().getContentAsByteArray()))) {
            for (Row ligne : classeur.getSheetAt(0)) {
                StringBuilder texte = new StringBuilder();
                ligne.forEach(cellule -> texte.append(cellule).append('|'));
                lignes.add(texte.toString());
            }
        }
        return lignes;
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void liste_filtreRoleId() throws Exception {
        UUID role = fixture.role("ROL-" + jeton);
        AdmUser retenu = fixture.compte();
        fixture.compte();
        fixture.lierRole(retenu.getId(), role);

        trouveSeul(retenu.getCode(), ROLE_ID, role.toString());
    }

    @Test
    void liste_filtrePersonnelId() throws Exception {
        UUID personnel = fixture.personnel("Nom-" + jeton);
        AdmUser retenu = fixture.compte();
        fixture.compte();
        fixture.poser(retenu.getId(), "CfgEmployeeId", personnel.toString());

        trouveSeul(retenu.getCode(), PERSONNEL_ID, personnel.toString());
    }

    @Test
    void liste_filtres_secumulent() throws Exception {
        UUID role = fixture.role("ROL-" + jeton);
        AdmUser actif = fixture.compte();
        AdmUser inactif = fixture.compte(true);
        fixture.lierRole(actif.getId(), role);
        fixture.lierRole(inactif.getId(), role);

        trouveSeul(actif.getCode(), ROLE_ID, role.toString(), DISABLED, "false");
        trouveSeul(inactif.getCode(), ROLE_ID, role.toString(), DISABLED, "true");
    }

    /** {@code disabled} absent = tous : l'écran envoie lui-même {@code false} par défaut. */
    @Test
    void liste_disabledAbsent_tousLesComptes() throws Exception {
        UUID role = fixture.role("ROL-" + jeton);
        fixture.lierRole(fixture.compte().getId(), role);
        fixture.lierRole(fixture.compte(true).getId(), role);

        appels.lire(RACINE, ROLE_ID, role.toString()).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(2));
    }

    @Test
    void liste_disabledNull_luCommeActif() throws Exception {
        AdmUser compte = fixture.compte((Boolean) null);

        trouveSeul(compte.getCode(), SEARCH, compte.getCode(), DISABLED, "false");
    }

    // ------------------------------------------------------------------ R18

    /** Vert sur yesRemsTest ({@code French_CI_AS}) : ne mord qu'en CI, où {@code Code} est {@code CS_AS} comme en production. */
    @Test
    void r18_searchMinuscules_trouveCodeAvecMajuscule() throws Exception {
        AdmUser compte = fixture.compte("Sarah." + jeton);

        trouveSeul(compte.getCode(), SEARCH, compte.getCode().toLowerCase(Locale.ROOT));
    }

    @Test
    void r18_searchMajuscules_trouveCodeEnMinuscules() throws Exception {
        AdmUser compte = fixture.compte("sarah." + jeton.toLowerCase(Locale.ROOT));

        trouveSeul(compte.getCode(), SEARCH, compte.getCode().toUpperCase(Locale.ROOT));
    }

    @Test
    void r18_searchCouvreEmail() throws Exception {
        AdmUser compte = fixture.compte();
        String courriel = jeton + "@example.com";
        fixture.poser(compte.getId(), "Email", courriel);

        trouveSeul(compte.getCode(), SEARCH, courriel.toLowerCase(Locale.ROOT));
    }

    @Test
    void r18_searchCouvreDescription() throws Exception {
        AdmUser compte = fixture.compte();
        fixture.poser(compte.getId(), "Description", "Libelle " + jeton);

        trouveSeul(compte.getCode(), SEARCH, "libelle " + jeton.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ export

    @Test
    void export_suitRecherche() throws Exception {
        AdmUser retenu = fixture.compte();
        AdmUser ecarte = fixture.compte();

        assertThat(lignesExport(SEARCH, retenu.getCode())).anyMatch(l -> l.contains(retenu.getCode()))
                .noneMatch(l -> l.contains(ecarte.getCode()));
    }

    @Test
    void export_suitRoleId() throws Exception {
        UUID role = fixture.role("ROL-" + jeton);
        AdmUser retenu = fixture.compte();
        AdmUser ecarte = fixture.compte();
        fixture.lierRole(retenu.getId(), role);

        assertThat(lignesExport(ROLE_ID, role.toString())).anyMatch(l -> l.contains(retenu.getCode()))
                .noneMatch(l -> l.contains(ecarte.getCode()));
    }

    @Test
    void export_suitPersonnelId() throws Exception {
        UUID personnel = fixture.personnel("Nom-" + jeton);
        AdmUser retenu = fixture.compte();
        AdmUser ecarte = fixture.compte();
        fixture.poser(retenu.getId(), "CfgEmployeeId", personnel.toString());

        assertThat(lignesExport(PERSONNEL_ID, personnel.toString())).anyMatch(l -> l.contains(retenu.getCode()))
                .noneMatch(l -> l.contains(ecarte.getCode()));
    }

    @Test
    void export_suitDisabled() throws Exception {
        UUID role = fixture.role("ROL-" + jeton);
        AdmUser actif = fixture.compte();
        AdmUser inactif = fixture.compte(true);
        fixture.lierRole(actif.getId(), role);
        fixture.lierRole(inactif.getId(), role);

        assertThat(lignesExport(ROLE_ID, role.toString(), DISABLED, "false")).anyMatch(l -> l.contains(actif.getCode()))
                .noneMatch(l -> l.contains(inactif.getCode()));
    }
}
