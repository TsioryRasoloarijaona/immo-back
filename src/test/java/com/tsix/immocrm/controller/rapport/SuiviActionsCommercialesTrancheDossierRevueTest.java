package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.fixtures.FixtureTransformationVente.StatutsVente;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue R3b, n° 30 (FEATURE-tdb-vente-rapports-commercial.md §2.1, RV-R3a) : un dossier dont l'article est sur une
 * tranche d'entité inactive ne doit donner aucun chiffre, même né d'une prospection d'une tranche visible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviActionsCommercialesTrancheDossierRevueTest {

    private static final String CODE = "ComSaleDashboardSuiviActionCommerciale";
    private static final String ACTION = "action";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;

    private AdmUser direction;
    private String codeAction;

    @BeforeEach
    void setUp() {
        String prefixe = FixtureEtatVentes.prefixe("SACR");
        fixture.tables();
        fixture.rapportVente(CODE, "SUIVI DES ACTIONS COMMERCIALES*", "VENTE", 2090);
        direction = fixture.compte("dg." + prefixe, fixture.role("Direction Générale", false));
        CfgTranche visible = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        CfgTranche inactive = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        StatutsVente statuts = fx.statutsVente(prefixe);
        codeAction = prefixe + "-X";
        ComAction x = fx.action(codeAction, LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 20),
                new BigDecimal("50000"));
        ComProspection p = fx.prospection(visible, null, fx.type("Visite"), LocalDate.of(2025, 3, 15), x);
        ev.prix(fx.dossier("DSR-" + prefixe, inactive, null, statuts.concretisation(), p),
                new BigDecimal("1000000"));
        fixture.relire();
    }

    @Test
    void r30_dossierSurTrancheInactive_niConcretisationNiCa() throws Exception {
        List<Map<String, Object>> sections = new AppelsRapports(mockMvc).sections(get(resultat(VENTE, CODE))
                .param("du", "2025-03-01").param("au", "2025-03-31").param("prefixe", codeAction)
                .with(connecte(direction)));
        List<Map<String, Object>> lignes = lignesOu(sections.get(0), ACTION, codeAction);
        assertThat(lignes).as("action X (prospection visible)").hasSize(1);
        Map<String, Object> x = lignes.get(0);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(x.get("concretisations"))).as("dossier d'une entité inactive")
                .isEqualByComparingTo("0");
        soft.assertThat(nombre(x.get("caConcretise"))).as("prix d'un dossier d'une entité inactive")
                .isEqualByComparingTo("0");
        soft.assertAll();
    }
}
