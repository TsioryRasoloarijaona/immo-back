package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-etat-des-ventes-parite.md : cas non couverts par les tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesPariteRevueTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String GROUP_BY = "groupBy";
    private static final String AGREGATS = "agregats";
    private static final String TRANCHE_CODE = "trancheCode";
    private static final String M2_TTC = "prixVenteM2Ttc";
    private static final String M2_HT = "prixVenteM2Ht";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;

    private String prefixe;
    private CfgTranche tranche;
    private StkItemCategory categorie;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVR");
        CfgProject projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        categorie = fixture.categorie("APP-" + prefixe);
        type = fixture.type("T3-" + prefixe);
    }

    private StkItem article(String suffixe) {
        return fixture.article(prefixe + "-" + suffixe, tranche, categorie, type);
    }

    private ComFolder vendu(StkItem a) {
        ComFolder d = fixture.vendre(a, "DOS-" + a.getCode(), fixture.statut(StatutDossier.CONCRETISATION), null,
                LocalDateTime.of(2025, 3, 15, 10, 0));
        return fixture.prix(d, new BigDecimal("1000000"));
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** Réponse (6) du manager : une colonne (a) en agrégat de /groupes → 400. */
    @Test
    void groupes_agregatDeNature_400() throws Exception {
        echeance100000(vendu(article("G1")));
        fixture.relire();

        mockMvc.perform(get(URL + "/groupes").param(TRANCHE_ID, tranche.getId().toString())
                        .param(GROUP_BY, TRANCHE_CODE).param(AGREGATS, "avance"))
                .andExpect(status().isBadRequest());
    }

    private void echeance100000(ComFolder d) {
        fixture.echeance(d, Nature.AVANCE, true, new BigDecimal("100000"));
    }

    /** Prix (b) agrégé par /groupes en SQL : vendu = ComFolder.PriceHT, libre = StkItem.ComSalePriceHT. */
    @Test
    void groupes_agregatDePrix_sommeDuDossierEtDuCatalogue() throws Exception {
        ComFolder d = vendu(article("G2"));
        d.setPriceHT(new BigDecimal("570005"));
        fixture.enregistrer(d);
        fixture.catalogue(article("G3"), new BigDecimal("840000"), new BigDecimal("960000"), new BigDecimal("0.2"),
                new BigDecimal("800000"));
        fixture.relire();

        List<Map<String, Object>> groupes = JsonPath.read(corps(get(URL + "/groupes")
                .param(TRANCHE_ID, tranche.getId().toString()).param(GROUP_BY, TRANCHE_CODE)
                .param(AGREGATS, "prixHt")), "$.content");

        assertThat(groupes).hasSize(1);
        Map<String, Object> agregats = (Map<String, Object>) groupes.getFirst().get("agregats");
        assertThat(agregats).hasSize(1);
        Object somme = agregats.values().iterator().next();
        assertThat(somme).isInstanceOf(Number.class);
        assertThat(((Number) somme).doubleValue()).isCloseTo(1370005.0, within(0.01));
    }

    /** Prix au m² demandé seul (sans colonne de nature) : valeur de /api/dossiers sur le vendu, null sur le libre. */
    @Test
    void prixAuM2_seul_egalListeDesDossiers_nullSurLibre() throws Exception {
        StkItem a = article("M1");
        ComFolder d = vendu(a);
        d.setArea(new BigDecimal("100"));
        d.setPriceAfterDiscount(new BigDecimal("1200000"));
        d.setVATRatio(new BigDecimal("0.2"));
        fixture.enregistrer(d);
        StkItem libre = article("M2");
        fixture.relire();

        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                .param("colonnes", "articleId," + M2_TTC + "," + M2_HT)), "$.content");
        List<Map<String, Object>> dossiers = JsonPath.read(corps(get("/api/dossiers")
                .param(TRANCHE_ID, tranche.getId().toString()).param("colonnes", "id," + M2_TTC + "," + M2_HT)),
                "$.content");

        Map<String, Object> vendue = ligne(lignes, a);
        Map<String, Object> ligneLibre = ligne(lignes, libre);
        assertThat(dossiers).hasSize(1);
        Map<String, Object> dossier = dossiers.getFirst();
        assertThat(nombre(vendue.get(M2_TTC))).isNotNull().isCloseTo(12000.0, within(0.01))
                .isEqualTo(nombre(dossier.get(M2_TTC)));
        assertThat(nombre(vendue.get(M2_HT))).isNotNull().isCloseTo(10000.0, within(0.01))
                .isEqualTo(nombre(dossier.get(M2_HT)));
        assertThat(ligneLibre).containsEntry(M2_TTC, null).containsEntry(M2_HT, null);
    }

    private static Map<String, Object> ligne(List<Map<String, Object>> lignes, StkItem a) {
        List<Map<String, Object>> trouvees = lignes.stream()
                .filter(l -> a.getId().toString().equals(l.get("articleId"))).toList();
        assertThat(trouvees).hasSize(1);
        return trouvees.getFirst();
    }

    private static Double nombre(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }
}
