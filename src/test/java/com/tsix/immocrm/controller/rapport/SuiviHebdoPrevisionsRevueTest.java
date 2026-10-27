package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
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

/** Revue Pb : semaine à cheval sur deux mois (FEATURE-tdb-vente-previsions.md §6 : S = [J ; J+6], M = mois de J). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviHebdoPrevisionsRevueTest {

    private static final String CODE = "ComTdbSuiviHebdoPrevision";
    private static final int Y = 2065;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureTransformationVente fx;

    private static LocalDate j(int mois, int jour) {
        return LocalDate.of(Y, mois, jour);
    }

    @Test
    void semaineAChevalSurDeuxMois_semaineLitLeMoisSuivant_moisResteCeluiDeJ() throws Exception {
        rapports.tables();
        rapports.rapportVente(CODE, "SUIVI HEBDOMADAIRE DES PREVISIONS*", "Suivi HEBDOMADAIRE ", 110);
        Jeu jeu = fixture.jeu("N4R");
        Statuts st = jeu.statuts();
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_RESILIATION, st.desistement().getId());
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_ANNULATION, st.annulation().getId());
        AdmRole role = rapports.role("Libre " + jeu.prefixe(), false);
        RequestPostProcessor libre = connecte(rapports.compte("n4r." + jeu.prefixe(), role));
        ComActionMessageType visite = fx.type(InteractionKpi.TYPE_VISITE);

        CfgTranche t = jeu.tranche("T", jeu.projet("P", true));
        ComFolder avril = jeu.dater(jeu.dossier(t, st.concretisation()), j(4, 28), null, null, null);
        jeu.dater(jeu.dossier(t, st.concretisation()), j(5, 2), null, null, null);
        for (int i = 0; i < 3; i++) {
            jeu.articleLibre(t, "1000");
        }
        t = jeu.lancer(t, j(3, 1));
        jeu.encaisser(avril, "1000", j(4, 29));
        jeu.encaisser(avril, "500", j(5, 3));
        fx.prospection(t, null, visite, j(4, 30), null);
        fx.prospection(t, null, visite, j(5, 3), null);
        rapports.relire();

        // J = lundi 27/04 : S = 27/04 … 03/05 ; M = avril
        String corps = new AppelsRapports(mockMvc).corps(demande(CODE, libre, List.of(t), DATE, j(4, 27).toString()));
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        Map<String, Object> l = ligneContenant(section(sections, "suivi"), "tranche", t.getCode());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get("stock"))).isEqualByComparingTo("5");
        soft.assertThat(nombre(l.get("realisationSemaine"))).as("28/04 et 02/05").isEqualByComparingTo("2");
        soft.assertThat(nombre(l.get("realisationMois"))).as("28/04 seulement").isEqualByComparingTo("1");
        soft.assertThat(nombre(l.get("visitesSemaine"))).as("30/04 et 03/05").isEqualByComparingTo("2");
        soft.assertThat(nombre(l.get("visitesMois"))).as("30/04 seulement").isEqualByComparingTo("1");
        soft.assertThat(nombre(l.get("versementsSemaine"))).as("29/04 + 03/05").isEqualByComparingTo("1500");
        soft.assertThat(nombre(l.get("versementsMois"))).as("29/04 seulement").isEqualByComparingTo("1000");
        soft.assertAll();
    }
}
