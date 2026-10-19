package com.tsix.immocrm.controller.etatventes;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue R4 : l'export passe par la même base que la liste (périmètre et entités inactives). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesExportVisibiliteTest {

    private static final String URL = "/api/etat-des-ventes/export";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projetA;
    private StkItem articleA;
    private StkItem articleB;
    private StkItem articleInactif;
    private String clientB;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVX");
        projetA = fixture.projet("PRJA-" + prefixe, true);
        CfgTranche trancheA = fixture.tranche("TRA-" + prefixe, projetA, true);
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, fixture.projet("PRJB-" + prefixe, true), true);
        CfgTranche trancheI = fixture.tranche("TRI-" + prefixe, fixture.projet("PRJI-" + prefixe, false), true);
        StkItemCategory categorie = fixture.categorie("APP-" + prefixe);
        StkItemType type = fixture.type("T3-" + prefixe);
        articleA = fixture.article(prefixe + "-A1", trancheA, categorie, type);
        articleB = fixture.article(prefixe + "-B1", trancheB, categorie, type);
        clientB = "ZEXB" + prefixe;
        fixture.vendre(articleB, "DOS-" + prefixe + "-B1", fixture.statut(StatutDossier.RESERVATION),
                fixture.client(clientB), LocalDateTime.of(2025, 3, 15, 10, 0));
        articleInactif = fixture.article(prefixe + "-I1", trancheI, categorie, type);
        fixture.relire();
    }

    private List<String> cellules(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete).andExpect(request().asyncStarted()).andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<String> valeurs = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            for (Row row : feuille) {
                for (Cell cell : row) {
                    valeurs.add(cell.toString());
                }
            }
        }
        return valeurs;
    }

    @Test
    void r4_commercialHorsPerimetre_exportSansLigneNiClientHorsPerimetre() throws Exception {
        AdmUser commercial = fixtureDossier.compte("edvx.com." + prefixe);
        fixture.affecter(commercial, projetA);
        fixture.relire();

        List<String> valeurs = cellules(get(URL).param("search", prefixe)
                .param("colonnes", "articleCode,clientNom,dossierCode")
                .with(connecte(commercial, "Commercial")));

        assertThat(valeurs).contains(articleA.getCode())
                .doesNotContain(articleB.getCode(), articleInactif.getCode(), "DOS-" + prefixe + "-B1");
        assertThat(String.join("|", valeurs)).doesNotContain(clientB);
    }

    @Test
    void r4_entiteInactive_exclueDeLExport_memeNonRestreint() throws Exception {
        List<String> valeurs = cellules(get(URL).param("search", prefixe).param("colonnes", "articleCode"));

        assertThat(valeurs).contains(articleA.getCode(), articleB.getCode())
                .doesNotContain(articleInactif.getCode());
    }
}
