package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue (argent) de FEATURE-etat-des-ventes-parite.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesArgentRevueTest {

    private static final String ARTICLE_HT = "prixArticleHt";
    private static final String SOUS_ARTICLES_HT = "prixSousArticlesHt";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;

    /**
     * F_0255 lit ComSaleMonitoringView.ItemPriceHT = ComSaleFolderView.ItemPriceHT = ComFolder.ItemPriceAfterHT
     * (après remise) ; ComFolder.ItemPriceHT y est « avant remise ». yesRemsProd 20/10 : 151 lignes vendues sur 361
     * diffèrent, Σ prixArticleHt 395,5 M > Σ prixHt 307,0 M.
     */
    @Test
    void prixHtDuVendu_apresRemiseCommeLaVueOmran() throws Exception {
        String prefixe = FixtureEtatVentes.prefixe("EDVA");
        CfgProject projet = fixture.projet("PRJ-" + prefixe, true);
        CfgTranche tranche = fixture.tranche("TR-" + prefixe, projet, true);
        StkItem a = fixture.article(prefixe + "-A1", tranche, fixture.categorie("APP-" + prefixe),
                fixture.type("T3-" + prefixe));
        ComFolder d = fixture.prix(fixture.vendre(a, "DOS-" + prefixe, fixture.statut(StatutDossier.CONCRETISATION),
                null, LocalDateTime.of(2025, 3, 15, 10, 0)), new BigDecimal("1000000"));
        d.setItemPriceHT(new BigDecimal("876000"));
        d.setItemPriceAfterHT(new BigDecimal("750000"));
        d.setUnderItemsPriceHT(new BigDecimal("30000"));
        d.setUnderItemsPriceAfterHT(new BigDecimal("25000"));
        d.setPriceHT(new BigDecimal("775000"));
        fixture.enregistrer(d);
        fixture.relire();

        String corps = mockMvc.perform(get("/api/etat-des-ventes").param("trancheId", tranche.getId().toString())
                        .param("colonnes", "articleId,prixHt," + ARTICLE_HT + "," + SOUS_ARTICLES_HT))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content");

        assertThat(lignes).hasSize(1);
        Map<String, Object> ligne = lignes.getFirst();
        assertThat(((Number) ligne.get(ARTICLE_HT)).doubleValue()).isCloseTo(750000.0, within(0.01));
        assertThat(((Number) ligne.get(SOUS_ARTICLES_HT)).doubleValue()).isCloseTo(25000.0, within(0.01));
    }
}
