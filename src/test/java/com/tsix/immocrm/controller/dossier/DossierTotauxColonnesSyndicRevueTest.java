package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue 2 (écart F3, Décisions 23/10) : les montants par nature sans chemin SQL sont sommés en Java par
 * {@code /totaux-colonnes} ; pour le même filtre la valeur doit égaler {@code /totaux}.
 * <pre>
 *   tranche A : d1 syndic 12 000 réglé 5 000 (+ 900 CANCELED), frais 3 000 ; d2 syndic 4 000
 *   tranche B : d3 syndic 7 000, crédit 50 000
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierTotauxColonnesSyndicRevueTest {

    private static final String LISTE = "/api/dossiers";
    private static final String SEARCH = "search";
    private static final String SYNDIC = "fraisSyndic";
    private static final String SYNDIC_RESTE = "fraisSyndicReste";
    private static final String CREDIT = "credit";
    private static final List<String> NOMS = List.of(SYNDIC, "fraisSyndicRegles", SYNDIC_RESTE, "frais", CREDIT,
            "avance");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;

    private String prefixe;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus enCours;
    private ComSettlementCategory syndic;
    private int numero;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("TCS");
        trancheA = ev.tranche("TA-" + prefixe, ev.projet("PA-" + prefixe, true), true);
        trancheB = ev.tranche("TB-" + prefixe, ev.projet("PB-" + prefixe, true), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        enCours = ev.statutEnCours("ENC-" + prefixe);
        syndic = rd.categorieSyndic();
    }

    private ComFolder dossier(CfgTranche tranche) {
        numero++;
        return ev.vendre(ev.article(prefixe + "-A" + numero, tranche, categorie, type), prefixe + "-D" + numero,
                enCours, ev.client("C" + numero + "-" + prefixe), null);
    }

    private void jeu() {
        ComFolder d1 = dossier(trancheA);
        ComDocument e1 = rd.echeance(d1, syndic, new BigDecimal("12000"));
        ev.regler(e1, new BigDecimal("5000"), true, "POINTED");
        ev.regler(e1, new BigDecimal("900"), true, "CANCELED");
        ev.echeance(d1, Nature.FRAIS, true, new BigDecimal("3000"));
        rd.echeance(dossier(trancheA), syndic, new BigDecimal("4000"));
        ComFolder d3 = dossier(trancheB);
        rd.echeance(d3, syndic, new BigDecimal("7000"));
        ev.echeance(d3, Nature.CREDIT, true, new BigDecimal("50000"));
        ev.relire();
    }

    private Map<String, Object> json(MockHttpServletRequestBuilder requete, String chemin) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(corps, chemin);
    }

    private void comparer(String trancheId, Map<String, String> attendus) throws Exception {
        MockHttpServletRequestBuilder totaux = get(LISTE + "/totaux").param(SEARCH, prefixe);
        MockHttpServletRequestBuilder colonnes = get(LISTE + "/totaux-colonnes").param(SEARCH, prefixe)
                .param("agregats", NOMS.toArray(String[]::new));
        if (trancheId != null) {
            totaux.param("trancheId", trancheId);
            colonnes.param("trancheId", trancheId);
        }
        Map<String, Object> sommes = json(totaux, "$.sommes");
        Map<String, Object> java = json(colonnes, "$.totaux");

        SoftAssertions soft = new SoftAssertions();
        for (String nom : NOMS) {
            soft.assertThat(java).as("totaux-colonnes contient " + nom).containsKey(nom);
            soft.assertThat(new BigDecimal(String.valueOf(java.get(nom))))
                    .as(nom + " : /totaux-colonnes = /totaux")
                    .isEqualByComparingTo(new BigDecimal(String.valueOf(sommes.get(nom))));
        }
        attendus.forEach((nom, valeur) -> soft.assertThat(new BigDecimal(String.valueOf(java.get(nom))))
                .as(nom).isEqualByComparingTo(valeur));
        soft.assertAll();
    }

    @Test
    void totauxColonnes_egaleTotaux_toutesTranches() throws Exception {
        jeu();

        comparer(null, Map.of(SYNDIC, "23000", "fraisSyndicRegles", "5000", SYNDIC_RESTE, "18000",
                "frais", "26000", CREDIT, "50000"));
    }

    @Test
    void totauxColonnes_egaleTotaux_filtreTranche() throws Exception {
        jeu();

        comparer(trancheA.getId().toString(), Map.of(SYNDIC, "16000", SYNDIC_RESTE, "11000", CREDIT, "0"));
    }
}
