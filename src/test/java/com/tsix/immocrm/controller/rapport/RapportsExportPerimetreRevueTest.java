package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue (règle 6) : l'export applique la même visibilité de données que le résultat (FEATURE-tdb-vente-rapports-socle.md
 * §3.4 RV1–RV2, §4.4 « mêmes paramètres »). TA : projet A ; TB : projet B ; TI : entité inactive.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsExportPerimetreRevueTest {

    private static final String FEUILLE_STOCK = "Stock par tranche";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private AdmUser commercialA;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche ti;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("REP");
        fixture.tables();
        fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        AdmRole roleLibre = fixture.role("Libre " + prefixe, false);
        CfgProject projetA = ev.projet("PA-" + prefixe, true);
        ta = ev.tranche("TA-" + prefixe, projetA, true);
        tb = ev.tranche("TB-" + prefixe, ev.projet("PB-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);
        for (CfgTranche t : List.of(ta, tb, ti)) {
            ev.article(t.getCode() + "-1", t, categorie, type);
        }
        commercialA = fixture.compte("rep.com." + prefixe, roleLibre);
        ev.affecter(commercialA, projetA);
        fixture.relire();
    }

    private RequestPostProcessor qui() {
        return connecte(commercialA);
    }

    private List<String> cellulesStock(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse reponse = appels.exporter(requete).getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        List<String> textes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            Sheet feuille = classeur.getSheet(FEUILLE_STOCK);
            assertThat(feuille).isNotNull();
            for (Row ligne : feuille) {
                for (Cell cellule : ligne) {
                    textes.add(cellule.toString());
                }
            }
        }
        return textes;
    }

    @Test
    void export_perimetreRestreint_sansTrancheHorsPerimetreNiEntiteInactive() throws Exception {
        assertThat(cellulesStock(get(export(VENTE, SUIVI_GLOBAL)).param("format", "xlsx").with(qui())))
                .contains(ta.getCode())
                .doesNotContain(tb.getCode(), ti.getCode());
    }

    @Test
    void export_trancheIdHorsPerimetre_aucuneTranche_pasToutes() throws Exception {
        assertThat(cellulesStock(get(export(VENTE, SUIVI_GLOBAL)).param("format", "xlsx")
                .param("trancheId", tb.getId().toString()).with(qui())))
                .doesNotContain(ta.getCode(), tb.getCode(), ti.getCode());
    }
}
