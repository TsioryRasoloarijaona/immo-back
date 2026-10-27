package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.DATE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ligneContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;

/** Un lancement de la semaine n'est pas une réalisation (FEATURE-tdb-vente-previsions.md, réponses à la revue Pb). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviHebdoPrevisionsLancementTest {

    private static final String CODE = "ComTdbSuiviHebdoPrevision";
    private static final int Y = 2065;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    @Test
    void trancheLanceeEtConcretiseeDansLaSemaine_realisationNeCompteQueLaConcretisation() throws Exception {
        rapports.tables();
        rapports.rapportVente(CODE, "SUIVI HEBDOMADAIRE DES PREVISIONS*", "Suivi HEBDOMADAIRE ", 110);
        Jeu jeu = fixture.jeu("N4L");
        Statuts st = jeu.statuts();
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_RESILIATION, st.desistement().getId());
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_ANNULATION, st.annulation().getId());
        AdmRole role = rapports.role("Libre " + jeu.prefixe(), false);
        RequestPostProcessor libre = connecte(rapports.compte("n4l." + jeu.prefixe(), role));

        CfgTranche t = jeu.tranche("T", jeu.projet("P", true));
        ComFolder vendu = jeu.dater(jeu.dossier(t, st.concretisation()), LocalDate.of(Y, 4, 30), null, null, null);
        for (int i = 0; i < 3; i++) {
            jeu.articleLibre(t, "1000");
        }
        t = jeu.lancer(t, LocalDate.of(Y, 4, 29));
        rapports.relire();

        // J = lundi 27/04 : lancement le 29/04, concrétisation le 30/04, dans la semaine et dans le mois
        String corps = new AppelsRapports(mockMvc).corps(
                demande(CODE, libre, List.of(t), DATE, LocalDate.of(Y, 4, 27).toString()));
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        Map<String, Object> l = ligneContenant(section(sections, "suivi"), "tranche", t.getCode());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(vendu.getId()).isNotNull();
        soft.assertThat(nombre(l.get("realisationSemaine"))).isEqualByComparingTo("1");
        soft.assertThat(nombre(l.get("realisationMois"))).isEqualByComparingTo("1");
        soft.assertAll();
    }
}
