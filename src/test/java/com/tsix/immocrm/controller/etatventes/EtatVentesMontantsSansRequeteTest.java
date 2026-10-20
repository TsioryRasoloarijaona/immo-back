package com.tsix.immocrm.controller.etatventes;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-etat-des-ventes-parite.md §3.3, §5.7 : sans colonne (a) demandée, le calcul partagé n'est pas appelé. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesMontantsSansRequeteTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @MockitoSpyBean
    private MontantsParNatureDossier montantsParNature;

    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        String prefixe = FixtureEtatVentes.prefixe("EDVQ");
        tranche = fixture.tranche("TR-" + prefixe, fixture.projet("PRJ-" + prefixe, true), true);
        StkItemCategory categorie = fixture.categorie("APP-" + prefixe);
        StkItemType type = fixture.type("T3-" + prefixe);
        fixture.prix(fixture.vendre(fixture.article(prefixe + "-A1", tranche, categorie, type), "DOS-" + prefixe,
                fixture.statut(StatutDossier.CONCRETISATION), null, LocalDateTime.of(2025, 3, 15, 10, 0)),
                new BigDecimal("1000000"));
        fixture.relire();
    }

    @Test
    void n7_sansColonneDeNature_calculPartageNonAppele() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .param(COLONNES, "articleId,articleCode,prixVenteTTC,prixHt,prixContractuel"))
                .andExpect(status().isOk());

        verify(montantsParNature, never()).pour(any());
    }

    /** Témoin : une colonne (a) demandée passe bien par le calcul partagé (§3.1, une seule formule). */
    @Test
    void n7_colonneDeNatureDemandee_calculPartageAppele() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()).param(COLONNES, "articleId,avance"))
                .andExpect(status().isOk());

        verify(montantsParNature, atLeastOnce()).pour(any());
    }
}
