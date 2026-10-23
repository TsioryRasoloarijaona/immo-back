package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsCommerciaux;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.entier;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.repartition;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue R3a : n° 19 / n° 31 doivent égaler {@code /api/prospections/groupes} (FEATURE-tdb-vente-rapports-commercial.md
 * §5 « Un seul calcul », §9.2, RE-R3). L'écran exclut les pré-prospections ({@code StrEntity = ComPreProspection}) ;
 * en prod elles font 2 659 des 4 407 prospections de 2026. Tranche T : 3 prospections + 2 pré-prospections, source S1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviProspectionsPreProspectionsRevueTest {

    private static final String CODE_19 = "ComTdbProspectionState";
    private static final String CODE_31 = "ComSaleDashboardAnalyseActionCommerciale";
    private static final int ANNEE = 2075;
    private static final String TRANCHE_ID = "trancheId";
    private static final String ADMINISTRATEUR = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsCommerciaux rc;
    @Autowired private ComProspectionRepository prospections;

    private AppelsRapports appels;
    private CfgTranche tranche;
    private AdmUser admin;
    private String s1;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("RPP");
        fixture.tables();
        fixture.rapportVente(CODE_19, "SUIVI DES PROSPECTIONS", "", 1700);
        fixture.rapportVente(CODE_31, "ANALYSE DES ACTIONS COMMERCIALES*", "VENTE", 2080);
        CfgProject projet = rc.projet("P-" + prefixe, true);
        tranche = rc.tranche("T-" + prefixe, projet);
        admin = rc.compte("rpp.adm." + prefixe, new CfgProject[0], fixture.role(ADMINISTRATEUR, false));
        ComProspectionOrigin origine = rc.origine("S1-" + prefixe);
        s1 = origine.getCode();
        LocalDateTime date = LocalDate.of(ANNEE, 3, 10).atTime(10, 0);
        for (int i = 0; i < 3; i++) {
            rc.prospection(tranche, date, null, origine, null, null);
        }
        for (int i = 0; i < 2; i++) {
            ComProspection pre = rc.prospection(tranche, date, null, origine, null, null);
            pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
            prospections.save(pre);
        }
        rc.relire();
    }

    private long ecranSourceS1() throws Exception {
        String corps = appels.corps(get("/api/prospections/groupes").param("groupBy", "origine")
                .param(TRANCHE_ID, tranche.getId().toString()).param("dateDebut", ANNEE + "-01-01")
                .param("dateFin", ANNEE + "-12-31").param("size", "200").with(connecte(admin, ADMINISTRATEUR)));
        List<Map<String, Object>> groupes = JsonPath.read(corps, "$.content");
        return groupes.stream().filter(g -> s1.equals(String.valueOf(g.get("libelle"))))
                .mapToLong(g -> entier(g.get("nombre"))).sum();
    }

    @Test
    void re19_source_egaleEcran_horsPreProspections() throws Exception {
        long ecran = ecranSourceS1();
        Map<String, Object> source = section(appels.sections(get(resultat(VENTE, CODE_19))
                .param("annee", String.valueOf(ANNEE)).param(TRANCHE_ID, tranche.getId().toString())
                .with(connecte(admin, ADMINISTRATEUR))), "prospections-source");

        assertThat(ecran).isEqualTo(3L);
        assertThat(repartition(source)).containsEntry(s1, ecran);
    }

    @Test
    void re31_source_egaleEcran_horsPreProspections() throws Exception {
        long ecran = ecranSourceS1();
        Map<String, Object> source = section(appels.sections(get(resultat(VENTE, CODE_31))
                .param("du", ANNEE + "-01-01").param("au", ANNEE + "-12-31")
                .param(TRANCHE_ID, tranche.getId().toString()).with(connecte(admin, ADMINISTRATEUR))), "sources");

        assertThat(repartition(source)).containsEntry(s1, ecran);
    }
}
