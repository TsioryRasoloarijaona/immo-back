package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.DATE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ligneContenant;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.lignesContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;

/** 2ᵉ revue Pb (n° 4) : semaine à cheval sur deux années ; versements hors portée ou d'une société inactive. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviHebdoPrevisionsRevue2Test {

    private static final String CODE = "ComTdbSuiviHebdoPrevision";
    private static final String VERSEMENTS_SEMAINE = "versementsSemaine";
    private static final String VERSEMENTS_MOIS = "versementsMois";
    private static final String SANS_DATE = "sans date de commercialisation";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureTransformationVente fx;
    @Autowired private FixtureEtatVentes ev;

    private Jeu jeu;
    private Statuts st;
    private AdmRole roleLibre;
    private RequestPostProcessor libre;
    private ComActionMessageType visite;

    @BeforeEach
    void setUp() {
        rapports.tables();
        rapports.rapportVente(CODE, "SUIVI HEBDOMADAIRE DES PREVISIONS*", "Suivi HEBDOMADAIRE ", 110);
        jeu = fixture.jeu("N4V");
        st = jeu.statuts();
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_RESILIATION, st.desistement().getId());
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_ANNULATION, st.annulation().getId());
        roleLibre = rapports.role("Libre " + jeu.prefixe(), false);
        libre = connecte(rapports.compte("n4v." + jeu.prefixe(), roleLibre));
        visite = fx.type(InteractionKpi.TYPE_VISITE);
    }

    private String corps(RequestPostProcessor qui, LocalDate jour, CfgTranche... tranches) throws Exception {
        return new AppelsRapports(mockMvc).corps(demande(CODE, qui, List.of(tranches), DATE, jour.toString()));
    }

    private static Map<String, Object> suivi(String corps) {
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        return section(sections, "suivi");
    }

    private static BigDecimal ouZero(Object valeur) {
        return valeur == null ? BigDecimal.ZERO : nombre(valeur);
    }

    /** Tranche lancée, 2 articles libres, un dossier concrétisé le {@code avance} et encaissé {@code montant} le {@code paye}. */
    private CfgTranche trancheEncaissee(String code, CfgProject projet, LocalDate avance, LocalDate paye, boolean lancer) {
        CfgTranche t = jeu.tranche(code, projet);
        ComFolder d = jeu.dater(jeu.dossier(t, st.concretisation()), avance, null, null, null);
        jeu.articleLibre(t, "1000");
        jeu.articleLibre(t, "1000");
        if (lancer) {
            t = jeu.lancer(t, LocalDate.of(2065, 3, 1));
        }
        jeu.encaisser(d, "7000", paye);
        fx.prospection(t, null, visite, paye, null);
        return t;
    }

    @Test
    void semaineAChevalSurDeuxAnnees_semaineLitJanvier_moisResteDecembre() throws Exception {
        CfgTranche t = jeu.tranche("T", jeu.projet("P", true));
        ComFolder dec = jeu.dater(jeu.dossier(t, st.concretisation()), LocalDate.of(2065, 12, 30), null, null, null);
        jeu.dater(jeu.dossier(t, st.concretisation()), LocalDate.of(2066, 1, 2), null, null, null);
        jeu.articleLibre(t, "1000");
        t = jeu.lancer(t, LocalDate.of(2065, 3, 1));
        jeu.encaisser(dec, "1000", LocalDate.of(2065, 12, 31));
        jeu.encaisser(dec, "500", LocalDate.of(2066, 1, 3));
        fx.prospection(t, null, visite, LocalDate.of(2065, 12, 30), null);
        fx.prospection(t, null, visite, LocalDate.of(2066, 1, 2), null);
        rapports.relire();

        // J = 29/12/2065 : S = 29/12/2065 … 04/01/2066 ; M = décembre 2065
        Map<String, Object> l = ligneContenant(suivi(corps(libre, LocalDate.of(2065, 12, 29), t)), "tranche",
                t.getCode());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get("stock"))).isEqualByComparingTo("3");
        soft.assertThat(nombre(l.get("realisationSemaine"))).as("30/12 et 02/01").isEqualByComparingTo("2");
        soft.assertThat(nombre(l.get("realisationMois"))).as("30/12 seulement").isEqualByComparingTo("1");
        soft.assertThat(nombre(l.get("visitesSemaine"))).isEqualByComparingTo("2");
        soft.assertThat(nombre(l.get("visitesMois"))).isEqualByComparingTo("1");
        soft.assertThat(nombre(l.get(VERSEMENTS_SEMAINE))).as("31/12 + 03/01").isEqualByComparingTo("1500");
        soft.assertThat(nombre(l.get(VERSEMENTS_MOIS))).as("31/12 seulement").isEqualByComparingTo("1000");
        soft.assertAll();
    }

    @Test
    void trancheIdHorsPortee_nonLancee_encaissee_aucunVersementNiAvertissement() throws Exception {
        LocalDate j = LocalDate.of(2065, 4, 6);
        CfgProject p = jeu.projet("P", true);
        CfgTranche t = trancheEncaissee("T", p, j.plusDays(1), j.plusDays(2), true);
        CfgTranche tb = trancheEncaissee("TB", jeu.projet("PB", true), j.plusDays(1), j.plusDays(2), false);
        AdmUser a = rapports.compte("n4va." + jeu.prefixe(), roleLibre, rapports.role(RolesEquipe.COMMERCIAL, true));
        ev.affecter(a, p);
        rapports.relire();

        String corps = corps(connecte(a), j, t, tb);
        Map<String, Object> s = suivi(corps);
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignesContenant(s, "tranche", tb.getCode())).isEmpty();
        soft.assertThat(ouZero(totaux(s).get(VERSEMENTS_SEMAINE))).as("seul T").isEqualByComparingTo("7000");
        soft.assertThat(ouZero(totaux(s).get(VERSEMENTS_MOIS))).as("seul T").isEqualByComparingTo("7000");
        soft.assertThat(ouZero(totaux(s).get("visitesSemaine"))).isEqualByComparingTo("1");
        soft.assertThat(String.join(" | ", avertissements)).doesNotContain(SANS_DATE);
        soft.assertAll();
    }

    @Test
    void trancheDUneSocieteInactive_encaissee_absenteDesVersements() throws Exception {
        LocalDate j = LocalDate.of(2065, 4, 6);
        CfgTranche t = trancheEncaissee("T", jeu.projet("P", true), j.plusDays(1), j.plusDays(2), true);
        CfgTranche ti = trancheEncaissee("TI", jeu.projet("PI", false), j.plusDays(1), j.plusDays(2), false);
        rapports.relire();

        String corps = corps(libre, j, t, ti);
        Map<String, Object> s = suivi(corps);
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignesContenant(s, "tranche", ti.getCode())).isEmpty();
        soft.assertThat(ouZero(totaux(s).get(VERSEMENTS_SEMAINE))).isEqualByComparingTo("7000");
        soft.assertThat(ouZero(totaux(s).get(VERSEMENTS_MOIS))).isEqualByComparingTo("7000");
        soft.assertThat(ouZero(totaux(s).get("realisationSemaine"))).isEqualByComparingTo("1");
        soft.assertThat(String.join(" | ", avertissements)).doesNotContain(SANS_DATE);
        soft.assertAll();
    }
}
