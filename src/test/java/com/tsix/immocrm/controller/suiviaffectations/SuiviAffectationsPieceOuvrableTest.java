package com.tsix.immocrm.controller.suiviaffectations;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureSuiviAffectations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.IN_WORK;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** D10 (§11) : {@code pieceOuvrable} vrai seulement pour {@code IsCredit = 1}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SuiviAffectationsPieceOuvrableTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviAffectations fixture;

    @Test
    void pieceOuvrable_creditSeulement_fauxPourDebitEtIsCreditNull() throws Exception {
        FixtureEtatVentes ev = fixture.etatVentes();
        String prefixe = FixtureEtatVentes.prefixe("SAP");
        CfgTranche tranche = ev.tranche("TR-" + prefixe, ev.projet("PRJ-" + prefixe, true), true);
        ComFolder dossier = ev.vendre(ev.article(prefixe + "-A1", tranche, ev.categorie("APP-" + prefixe),
                ev.type("T3-" + prefixe)), "DOS-" + prefixe, ev.statut(StatutDossier.RESERVATION), null, null);
        ComDocument echeance = fixture.echeance(dossier, fixture.categorie("AV-" + prefixe), BigDecimal.TEN);
        StlSettlementLine credit = ligne(echeance, prefixe + " C", true);
        StlSettlementLine debit = ligne(echeance, prefixe + " D", false);
        StlSettlementLine sansSens = ligne(echeance, prefixe + " N", null);
        fixture.relire();

        mockMvc.perform(get("/api/suivi-affectations").param("trancheId", tranche.getId().toString())
                        .param("colonnes", "id,pieceOuvrable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(chemin(credit)).value(contains(true)))
                .andExpect(jsonPath(chemin(debit)).value(contains(false)))
                .andExpect(jsonPath(chemin(sansSens)).value(contains(false)));
    }

    private StlSettlementLine ligne(ComDocument echeance, String libelle, Boolean credit) {
        return fixture.ligne(fixture.piece(libelle, credit, IN_WORK, LocalDateTime.of(2025, 6, 1, 0, 0), null,
                BigDecimal.TEN), echeance, BigDecimal.TEN);
    }

    private static String chemin(StlSettlementLine l) {
        return "$.content[?(@.id=='" + l.getId() + "')].pieceOuvrable";
    }
}
