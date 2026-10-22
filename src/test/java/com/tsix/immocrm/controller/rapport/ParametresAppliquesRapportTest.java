package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** {@code parametres.trancheId} du résultat = tranches effectivement appliquées (FEATURE-tdb-vente-rapports-socle.md §4.3). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ParametresAppliquesRapportTest {

    private static final String TRANCHE_ID = "trancheId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche visible;
    private CfgTranche invisible;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("PAP");
        fixture.tables();
        fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        AdmRole role = fixture.role("Libre " + prefixe, false);
        visible = ev.tranche("TV-" + prefixe, ev.projet("PV-" + prefixe, true), true);
        invisible = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        fixture.relire();
        libre = connecte(fixture.compte("pap.libre." + prefixe, role));
    }

    private List<String> tranchesAppliquees(String... ids) throws Exception {
        String corps = appels.corps(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, ids).with(libre));
        return JsonPath.read(corps, "$.parametres.trancheId");
    }

    @Test
    void trancheInvisibleSeule_parametreRenvoyeVide() throws Exception {
        assertThat(tranchesAppliquees(invisible.getId().toString())).isEmpty();
    }

    @Test
    void visibleEtInvisible_seuleLaVisibleRenvoyee() throws Exception {
        assertThat(tranchesAppliquees(visible.getId().toString(), invisible.getId().toString()))
                .containsExactly(visible.getId().toString());
    }
}
